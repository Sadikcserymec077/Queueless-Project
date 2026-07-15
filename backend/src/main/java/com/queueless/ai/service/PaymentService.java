package com.queueless.ai.service;

import com.queueless.ai.dto.PaymentDtos.CreateOrderRequest;
import com.queueless.ai.dto.PaymentDtos.CreateOrderResponse;
import com.queueless.ai.dto.PaymentDtos.VerifyPaymentRequest;
import com.queueless.ai.dto.TokenDtos.TokenResponse;
import com.queueless.ai.entity.Counter;
import com.queueless.ai.entity.Payment;
import com.queueless.ai.entity.Token;
import com.queueless.ai.entity.TokenStatus;
import com.queueless.ai.entity.User;
import com.queueless.ai.exception.BadRequestException;
import com.queueless.ai.exception.CapacityExceededException;
import com.queueless.ai.exception.PaymentVerificationException;
import com.queueless.ai.exception.ResourceNotFoundException;
import com.queueless.ai.repository.PaymentRepository;
import com.queueless.ai.repository.TokenRepository;
import com.queueless.ai.repository.UserRepository;
import com.queueless.ai.util.QrCodeGenerator;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final CounterService counterService;
    private final PricingService pricingService;
    private final NotificationService notificationService;
    private final WaitTimePredictionService waitTimePredictionService;
    private final QrCodeGenerator qrCodeGenerator;
    private final TokenService tokenService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    /**
     * Step 1 of payment flow: create a Razorpay order.
     * Returns order details so the frontend can open the Razorpay popup.
     */
    @Transactional
    public CreateOrderResponse createOrder(Long userId, CreateOrderRequest request) {
        Counter counter = counterService.find(request.counterId());
        int patientCount = request.patientCount() == null ? 1 : request.patientCount();

        // Validate scheduled date capacity if provided
        if (request.scheduledDate() != null) {
            if (!counter.getAvailableDates().contains(request.scheduledDate())) {
                throw new BadRequestException("The selected date is not available for booking on this counter");
            }
            long booked = tokenRepository.countByCounterIdAndScheduledDate(counter.getId(), request.scheduledDate());
            if (booked >= counter.getDailyCapacity()) {
                throw new CapacityExceededException("This date is fully booked for counter: " + counter.getCounterName());
            }
        }

        double originalAmount = pricingService.originalAmount(patientCount, counter.getBookingFee());
        double discount = pricingService.discountPercent(patientCount);
        double finalAmount = pricingService.finalAmount(patientCount, counter.getBookingFee());
        long paise = pricingService.toPaise(finalAmount);

        if (paise == 0) {
            return new CreateOrderResponse(
                    "free_order_" + System.currentTimeMillis(),
                    0.0, 0.0, 0.0, patientCount, razorpayKeyId
            );
        }

        try {
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", paise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "ql_" + userId + "_" + System.currentTimeMillis());

            Order order = razorpay.orders.create(orderRequest);
            String razorpayOrderId = order.get("id");

            Payment payment = Payment.builder()
                    .razorpayOrderId(razorpayOrderId)
                    .userId(userId)
                    .counterId(counter.getId())
                    .patientCount(patientCount)
                    .originalAmount(originalAmount)
                    .discountPercent(discount * 100)
                    .finalAmount(finalAmount)
                    .status("PENDING")
                    .build();
            paymentRepository.save(payment);

            log.info("PaymentService: Created Razorpay order {} for user {} amount ₹{}", razorpayOrderId, userId, finalAmount);

            return new CreateOrderResponse(
                    razorpayOrderId,
                    finalAmount,
                    originalAmount,
                    discount * 100,
                    patientCount,
                    razorpayKeyId
            );

        } catch (RazorpayException exception) {
            log.error("PaymentService: Failed to create Razorpay order for user {}", userId, exception);
            throw new BadRequestException("Could not initiate payment. Please try again.");
        }
    }

    /**
     * Step 2 of payment flow: verify Razorpay signature, then generate token + QR code.
     * Token is ONLY created here — never before payment is verified.
     */
    @Transactional
    public TokenResponse verifyAndBook(Long userId, VerifyPaymentRequest request) {
        Payment payment = paymentRepository.findByRazorpayOrderIdForUpdate(request.razorpayOrderId())
                .orElseThrow(() -> new BadRequestException("Payment order not found"));

        if (!"PENDING".equals(payment.getStatus())) {
            if ("SUCCESS".equals(payment.getStatus())) {
                Token token = tokenRepository.findTopByUserIdAndCounterIdAndStatusInOrderByBookingTimeDesc(
                        userId, payment.getCounterId(), java.util.Set.of(TokenStatus.WAITING, TokenStatus.CALLED))
                        .orElseThrow(() -> new BadRequestException("Payment successful but token not found"));
                return tokenService.toResponse(token);
            }
            throw new BadRequestException("This payment has already been processed");
        }

        // Verify HMAC-SHA256 signature
        boolean valid = verifySignature(request.razorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature());
        if (!valid) {
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
            log.error("PaymentService: Signature verification FAILED for order {}", request.razorpayOrderId());
            throw new PaymentVerificationException("Payment signature is invalid. Contact support with Order ID: " + request.razorpayOrderId());
        }

        payment.setRazorpayPaymentId(request.razorpayPaymentId());
        payment.setRazorpaySignature(request.razorpaySignature());
        payment.setStatus("SUCCESS");
        paymentRepository.save(payment);

        log.info("PaymentService: Signature verified for order {}. Generating token.", request.razorpayOrderId());

        return generateToken(userId, payment, request.scheduledDate());
    }

    /**
     * Webhook endpoint handler — generates token if payment.captured arrives
     * and the payment record is still PENDING (handles phone-death scenario).
     */
    @Transactional
    public void handleWebhook(String razorpayOrderId, String razorpayPaymentId, String webhookSignature, String webhookSecret, String payload) {
        try {
            boolean valid = verifyWebhookSignature(payload, webhookSignature, webhookSecret);
            if (!valid) {
                log.warn("PaymentService: Webhook signature invalid for order {}", razorpayOrderId);
                return;
            }

            Payment payment = paymentRepository.findByRazorpayOrderIdForUpdate(razorpayOrderId).orElse(null);
            if (payment == null || !"PENDING".equals(payment.getStatus())) {
                return; // Already processed or doesn't exist
            }

            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setStatus("SUCCESS");
            paymentRepository.save(payment);

            generateToken(payment.getUserId(), payment, null);
            log.info("PaymentService: Webhook successfully generated token for order {}", razorpayOrderId);

        } catch (Exception exception) {
            log.error("PaymentService: Webhook processing error for order {}", razorpayOrderId, exception);
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private TokenResponse generateToken(Long userId, Payment payment, LocalDate scheduledDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Counter counter = counterService.findForUpdate(payment.getCounterId());

        Instant now = Instant.now();
        LocalDate date = LocalDate.ofInstant(now, ZoneOffset.UTC);
        Instant start = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        long sequence = tokenRepository.countByCounterIdAndBookingTimeBetween(counter.getId(), start, end) + 1;
        String orgPrefix = counter.getOrganization().getName().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();

        String suffix = scheduledDate != null
                ? "-" + scheduledDate.format(DateTimeFormatter.BASIC_ISO_DATE)
                : "-" + date.format(DateTimeFormatter.BASIC_ISO_DATE);
        String tokenNumber = orgPrefix + "-C" + counter.getId() + "-" + String.format("%03d", sequence) + suffix;
        String qrPayload = "QLAI::" + tokenNumber + "::" + counter.getId();

        Token token = Token.builder()
                .tokenNumber(tokenNumber)
                .user(user)
                .counter(counter)
                .bookingTime(now)
                .status(TokenStatus.WAITING)
                .estimatedWaitTime(waitTimePredictionService.estimateWaitMinutes(counter.getId(), now))
                .qrPayload(qrPayload)
                .qrCodeData(qrCodeGenerator.generateDataUri(qrPayload))
                .scheduledDate(scheduledDate)
                .patientCount(payment.getPatientCount())
                .paymentStatus("SUCCESS")
                .totalAmountPaid(payment.getFinalAmount())
                .build();

        Token saved = tokenRepository.saveAndFlush(token);

        notificationService.notifyUser(
                user,
                "Booking Confirmed: " + saved.getTokenNumber(),
                "Your token " + saved.getTokenNumber() + " has been confirmed for " +
                        counter.getCounterName() + ".\n" +
                        "Patients: " + payment.getPatientCount() + "\n" +
                        "Amount Paid: ₹" + String.format("%.2f", payment.getFinalAmount()) +
                        (scheduledDate != null ? "\nScheduled Date: " + scheduledDate : "") +
                        "\nPlease show your QR code at the counter."
        );

        tokenService.publishTokenUpdatesAfterCommit(counter.getId(), userId, saved.getId(), scheduledDate);

        return tokenService.toResponse(saved);
    }

    private boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(), "HmacSHA256"));
            byte[] expectedBytes = mac.doFinal(payload.getBytes());
            byte[] actualBytes = hexToBytes(signature);
            return MessageDigest.isEqual(expectedBytes, actualBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            log.error("PaymentService: Signature verification error", exception);
            return false;
        }
    }

    private boolean verifyWebhookSignature(String payload, String signature, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
            byte[] expectedBytes = mac.doFinal(payload.getBytes());
            byte[] actualBytes = hexToBytes(signature);
            return MessageDigest.isEqual(expectedBytes, actualBytes);
        } catch (Exception exception) {
            return false;
        }
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
