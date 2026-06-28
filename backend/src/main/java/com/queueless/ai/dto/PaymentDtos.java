package com.queueless.ai.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class PaymentDtos {

    public record CreateOrderRequest(
            @NotNull Long counterId,
            @Min(1) Integer patientCount,
            LocalDate scheduledDate
    ) {
    }

    public record CreateOrderResponse(
            String razorpayOrderId,
            Double finalAmount,
            Double originalAmount,
            Double discountPercent,
            Integer patientCount,
            String razorpayKeyId
    ) {
    }

    public record VerifyPaymentRequest(
            @NotBlank String razorpayOrderId,
            @NotBlank String razorpayPaymentId,
            @NotBlank String razorpaySignature,
            LocalDate scheduledDate
    ) {
    }
}
