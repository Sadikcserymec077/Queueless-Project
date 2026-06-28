package com.queueless.ai.service;

import com.queueless.ai.dto.PageResponse;
import com.queueless.ai.dto.TokenDtos.AdminQueueResponse;
import com.queueless.ai.dto.TokenDtos.QueueStatusResponse;
import com.queueless.ai.dto.TokenDtos.TokenRequest;
import com.queueless.ai.dto.TokenDtos.TokenResponse;
import com.queueless.ai.entity.Counter;
import com.queueless.ai.entity.CounterStatus;
import com.queueless.ai.entity.Token;
import com.queueless.ai.entity.TokenStatus;
import com.queueless.ai.entity.User;
import com.queueless.ai.exception.BadRequestException;
import com.queueless.ai.exception.CapacityExceededException;
import com.queueless.ai.exception.ResourceNotFoundException;
import com.queueless.ai.repository.TokenRepository;
import com.queueless.ai.repository.UserRepository;
import com.queueless.ai.util.QrCodeGenerator;
import com.queueless.ai.websocket.QueueEventPublisher;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenService {

    private static final Set<TokenStatus> ACTIVE_STATUSES = Set.of(TokenStatus.WAITING, TokenStatus.CALLED);

    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final CounterService counterService;
    private final WaitTimePredictionService waitTimePredictionService;
    private final NotificationService notificationService;
    private final QueueEventPublisher queueEventPublisher;
    private final QrCodeGenerator qrCodeGenerator;

    @Transactional
    public TokenResponse bookToken(Long userId, TokenRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Counter counter = counterService.find(request.counterId());
        if (!counter.getOrganization().isActive() || counter.getStatus() != CounterStatus.ACTIVE) {
            throw new BadRequestException("This counter is not accepting new tokens right now");
        }

        // Live booking check
        if (request.scheduledDate() == null) {
            if (tokenRepository.existsByUserIdAndCounterIdAndStatusIn(userId, counter.getId(), ACTIVE_STATUSES)) {
                throw new BadRequestException("You already have an active token for this counter");
            }
        } else {
            // Future booking validation
            if (!counter.getAvailableDates().contains(request.scheduledDate())) {
                throw new BadRequestException("The selected date is not available for booking");
            }
            long booked = tokenRepository.countByCounterIdAndScheduledDate(counter.getId(), request.scheduledDate());
            if (booked >= counter.getDailyCapacity()) {
                throw new CapacityExceededException("This date is fully booked for counter: " + counter.getCounterName());
            }
        }

        Instant now = Instant.now();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        Instant startOfDay = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        long todayBookings = tokenRepository.countByUserIdAndBookingTimeBetweenAndStatusNot(userId, startOfDay, endOfDay, TokenStatus.CANCELLED);
        if (todayBookings >= 10) {
            throw new BadRequestException("You can only book up to 10 tokens per day across all organizations");
        }
        
        String tokenNumber = generateTokenNumber(counter, now, request.scheduledDate());
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
                .scheduledDate(request.scheduledDate())
                .patientCount(1)
                .paymentStatus(counter.getBookingFee() > 0 ? "PENDING" : "SUCCESS")
                .build();

        Token saved = tokenRepository.saveAndFlush(token);
        
        notificationService.notifyUser(
                user,
                "Token booked: " + saved.getTokenNumber(),
                "Your token " + saved.getTokenNumber() + " has been booked for " + counter.getCounterName() + 
                (request.scheduledDate() != null ? " on " + request.scheduledDate() : "") + "."
        );

        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishQueue(counter.getId());
                    if (request.scheduledDate() == null || request.scheduledDate().equals(today)) {
                        queueEventPublisher.publishUserUpdate(userId, getQueueStatus(saved.getId(), userId, false));
                    }
                }
            }
        );
        
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<TokenResponse> history(Long userId, int page, int size) {
        Page<Token> tokens = tokenRepository.findByUserIdOrderByBookingTimeDesc(
                userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "bookingTime"))
        );
        return new PageResponse<>(
                tokens.getContent().stream().map(this::toResponse).toList(),
                tokens.getNumber(),
                tokens.getSize(),
                tokens.getTotalElements(),
                tokens.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public TokenResponse activeToken(Long userId) {
        return tokenRepository.findTopByUserIdAndStatusInOrderByBookingTimeDesc(userId, ACTIVE_STATUSES)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public QueueStatusResponse getQueueStatus(Long tokenId, Long requesterId, boolean admin) {
        Token token = find(tokenId);
        if (!admin && !token.getUser().getId().equals(requesterId)) {
            throw new AccessDeniedException("Token does not belong to current user");
        }
        TokenResponse tokenResponse = toResponse(token);
        String current = tokenRepository.findTopByCounterIdAndStatusOrderByCalledAtDesc(
                        token.getCounter().getId(),
                        TokenStatus.CALLED
                )
                .map(Token::getTokenNumber)
                .orElse(null);
                
        int peopleAhead = 0;
        int waitMinutes = 0;
        
        LocalDate today = LocalDate.ofInstant(Instant.now(), ZoneOffset.UTC);
        if (token.getStatus() == TokenStatus.WAITING && (token.getScheduledDate() == null || token.getScheduledDate().equals(today))) {
            peopleAhead = Math.toIntExact(tokenRepository.countByCounterIdAndStatusAndBookingTimeBefore(
                    token.getCounter().getId(),
                    TokenStatus.WAITING,
                    token.getBookingTime()
            ) + (current == null ? 0 : 1));
            waitMinutes = peopleAhead * waitTimePredictionService.averageServiceMinutes(token.getCounter().getId());
        }
        
        return new QueueStatusResponse(
                tokenResponse,
                current,
                peopleAhead,
                waitMinutes,
                Instant.now().plus(Duration.ofMinutes(waitMinutes))
        );
    }

    @Transactional
    public TokenResponse cancel(Long tokenId, Long userId, boolean admin) {
        Token token = find(tokenId);
        if (!admin && !token.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Token does not belong to current user");
        }
        if (!ACTIVE_STATUSES.contains(token.getStatus())) {
            throw new BadRequestException("Only waiting or called tokens can be cancelled");
        }
        token.setStatus(TokenStatus.CANCELLED);
        notificationService.notifyUser(token.getUser(), "Token cancelled: " + token.getTokenNumber(), "Token " + token.getTokenNumber() + " has been cancelled.");
        
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishQueue(token.getCounter().getId());
                    queueEventPublisher.publishUserUpdate(token.getUser().getId(), toResponse(token));
                }
            }
        );
        return toResponse(token);
    }

    @Transactional(readOnly = true)
    public void requestCancel(Long tokenId, Long userId) {
        Token token = find(tokenId);
        if (!token.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Token does not belong to current user");
        }
        if (token.getStatus() != TokenStatus.WAITING) {
            throw new BadRequestException("Can only request cancellation for waiting tokens");
        }
        
        queueEventPublisher.publishCounterUpdate(token.getCounter().getId(), 
            java.util.Map.of("type", "CANCEL_REQUEST", "token", toResponse(token)));
    }

    @Transactional(readOnly = true)
    public void requestDelay(Long tokenId, Long userId) {
        Token token = find(tokenId);
        if (!token.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Token does not belong to current user");
        }
        if (token.getStatus() != TokenStatus.WAITING) {
            throw new BadRequestException("Can only request to come late for waiting tokens");
        }
        
        queueEventPublisher.publishCounterUpdate(token.getCounter().getId(), 
            java.util.Map.of("type", "DELAY_REQUEST", "token", toResponse(token)));
    }

    @Transactional
    public TokenResponse delay(Long tokenId) {
        Token token = find(tokenId);
        if (token.getStatus() != TokenStatus.WAITING) {
            throw new BadRequestException("Only waiting tokens can be delayed");
        }
        token.setStatus(TokenStatus.WAITING);
        token.setBookingTime(Instant.now());
        notificationService.notifyUser(token.getUser(), "Token delayed: " + token.getTokenNumber(), "Your token " + token.getTokenNumber() + " has been moved to the end of the line.");
        
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishQueue(token.getCounter().getId());
                    queueEventPublisher.publishUserUpdate(token.getUser().getId(), toResponse(token));
                }
            }
        );
        return toResponse(token);
    }

    @Transactional
    public TokenResponse callNext(Long counterId) {
        Counter counter = counterService.find(counterId);
        tokenRepository.findTopByCounterIdAndStatusOrderByCalledAtDesc(counterId, TokenStatus.CALLED)
                .ifPresent(token -> {
                    throw new BadRequestException("Complete or skip the current token before calling the next one");
                });
                
        LocalDate today = LocalDate.ofInstant(Instant.now(), ZoneOffset.UTC);
        List<Token> liveTokens = tokenRepository.findLiveWaitingForCounter(counterId, today);
        if (liveTokens.isEmpty()) {
            throw new BadRequestException("No waiting tokens for " + counter.getCounterName());
        }
        
        Token next = liveTokens.get(0);
        next.setStatus(TokenStatus.CALLED);
        next.setCalledAt(Instant.now());
        next.setEstimatedWaitTime(0);
        notificationService.notifyUser(
                next.getUser(),
                "Your turn is ready: " + next.getTokenNumber(),
                "Token " + next.getTokenNumber() + " is now being served at " + counter.getCounterName() + "."
        );
        
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishQueue(counterId);
                    queueEventPublisher.publishUserUpdate(next.getUser().getId(), getQueueStatus(next.getId(), next.getUser().getId(), false));
                }
            }
        );
        return toResponse(next);
    }

    @Transactional
    public TokenResponse complete(Long tokenId) {
        Token token = find(tokenId);
        if (token.getStatus() != TokenStatus.CALLED) {
            throw new BadRequestException("Only called tokens can be completed");
        }
        token.setStatus(TokenStatus.COMPLETED);
        token.setCompletedAt(Instant.now());
        notificationService.notifyUser(token.getUser(), "Service completed: " + token.getTokenNumber(), "Token " + token.getTokenNumber() + " has been completed.");
        
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishQueue(token.getCounter().getId());
                    queueEventPublisher.publishUserUpdate(token.getUser().getId(), toResponse(token));
                }
            }
        );
        return toResponse(token);
    }

    @Transactional
    public TokenResponse skip(Long tokenId) {
        Token token = find(tokenId);
        if (!ACTIVE_STATUSES.contains(token.getStatus())) {
            throw new BadRequestException("Only waiting or called tokens can be skipped");
        }
        token.setStatus(TokenStatus.SKIPPED);
        notificationService.notifyUser(token.getUser(), "Token skipped: " + token.getTokenNumber(), "Token " + token.getTokenNumber() + " was skipped.");
        
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishQueue(token.getCounter().getId());
                    queueEventPublisher.publishUserUpdate(token.getUser().getId(), toResponse(token));
                }
            }
        );
        return toResponse(token);
    }

    @Transactional
    public TokenResponse requeue(Long tokenId) {
        Token token = find(tokenId);
        if (token.getStatus() != TokenStatus.SKIPPED) {
            throw new BadRequestException("Only skipped tokens can be re-queued");
        }
        token.setStatus(TokenStatus.WAITING);
        token.setBookingTime(Instant.now());
        notificationService.notifyUser(token.getUser(), "Token re-queued: " + token.getTokenNumber(), "Your token " + token.getTokenNumber() + " has been added back to the queue.");
        
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishQueue(token.getCounter().getId());
                    queueEventPublisher.publishUserUpdate(token.getUser().getId(), toResponse(token));
                }
            }
        );
        return toResponse(token);
    }

    @Transactional(readOnly = true)
    public AdminQueueResponse getCounterQueue(Long counterId) {
        Counter counter = counterService.find(counterId);
        TokenResponse current = tokenRepository.findTopByCounterIdAndStatusOrderByCalledAtDesc(counterId, TokenStatus.CALLED)
                .map(this::toResponse)
                .orElse(null);
                
        LocalDate today = LocalDate.ofInstant(Instant.now(), ZoneOffset.UTC);
        List<TokenResponse> waiting = tokenRepository.findLiveWaitingForCounter(counterId, today)
                .stream()
                .limit(20)
                .map(this::toResponse)
                .toList();
                
        List<TokenResponse> skipped = tokenRepository.findTop20ByCounterIdAndStatusOrderByBookingTimeAsc(counterId, TokenStatus.SKIPPED)
                .stream()
                .map(this::toResponse)
                .toList();
                
        return new AdminQueueResponse(
                counter.getId(),
                counter.getCounterName(),
                counter.getOrganization().getName(),
                current,
                waiting,
                skipped,
                tokenRepository.countByCounterIdAndStatus(counterId, TokenStatus.WAITING)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<TokenResponse> searchAdmin(TokenStatus status, String q, Instant from, Instant to, int page, int size) {
        Page<Token> tokens = tokenRepository.searchAdmin(
                status,
                q == null || q.isBlank() ? null : q.trim(),
                from,
                to,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "bookingTime"))
        );
        return new PageResponse<>(
                tokens.getContent().stream().map(this::toResponse).toList(),
                tokens.getNumber(),
                tokens.getSize(),
                tokens.getTotalElements(),
                tokens.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public TokenResponse verifyQr(String qrPayload) {
        String[] parts = qrPayload.split("::");
        if (parts.length != 3 || !"QLAI".equals(parts[0])) {
            throw new BadRequestException("Invalid QR token payload");
        }
        Token token = tokenRepository.findByTokenNumber(parts[1])
                .orElseThrow(() -> new ResourceNotFoundException("Token", parts[1]));
        if (!token.getCounter().getId().toString().equals(parts[2]) || !qrPayload.equals(token.getQrPayload())) {
            throw new BadRequestException("QR token does not match system records");
        }
        return toResponse(token);
    }

    public TokenResponse toResponse(Token token) {
        Integer queuePosition = token.getQueuePosition();
        Integer estimatedWait = token.getEstimatedWaitTime();
        Instant expectedTurnTime = null;

        LocalDate today = LocalDate.ofInstant(Instant.now(), ZoneOffset.UTC);

        if (token.getStatus() == TokenStatus.WAITING) {
            if (token.getScheduledDate() == null || token.getScheduledDate().equals(today)) {
                long waitingAhead = tokenRepository.countByCounterIdAndStatusAndBookingTimeBefore(
                        token.getCounter().getId(),
                        TokenStatus.WAITING,
                        token.getBookingTime()
                );
                long currentService = tokenRepository.findTopByCounterIdAndStatusOrderByCalledAtDesc(
                                token.getCounter().getId(),
                                TokenStatus.CALLED
                        )
                        .map(current -> 1L)
                        .orElse(0L);
                queuePosition = Math.toIntExact(waitingAhead + 1);
                estimatedWait = Math.toIntExact((waitingAhead + currentService) * waitTimePredictionService.averageServiceMinutes(token.getCounter().getId()));
                expectedTurnTime = Instant.now().plus(Duration.ofMinutes(estimatedWait));
            } else {
                // Future scheduled token
                queuePosition = null;
                estimatedWait = null;
                expectedTurnTime = null;
            }
        } else if (token.getStatus() == TokenStatus.CALLED) {
            queuePosition = 0;
            estimatedWait = 0;
            expectedTurnTime = Instant.now();
        }

        return new TokenResponse(
                token.getId(),
                token.getTokenNumber(),
                token.getUser().getId(),
                token.getUser().getName(),
                token.getUser().getPhone(),
                token.getCounter().getId(),
                token.getCounter().getCounterName(),
                token.getCounter().getOrganization().getId(),
                token.getCounter().getOrganization().getName(),
                token.getBookingTime(),
                queuePosition,
                token.getStatus(),
                estimatedWait,
                expectedTurnTime,
                token.getQrPayload(),
                token.getQrCodeData(),
                token.getScheduledDate(),
                token.getPatientCount()
        );
    }

    @Transactional(readOnly = true)
    public Token find(Long id) {
        return tokenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Token", id));
    }

    private String generateTokenNumber(Counter counter, Instant now, LocalDate scheduledDate) {
        LocalDate date = LocalDate.ofInstant(now, ZoneOffset.UTC);
        Instant start = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        long sequence = tokenRepository.countByCounterIdAndBookingTimeBetween(counter.getId(), start, end) + 1;
        String orgPrefix = counter.getOrganization().getName().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        
        String suffix = scheduledDate != null 
            ? "-" + scheduledDate.format(DateTimeFormatter.BASIC_ISO_DATE) 
            : "";
            
        return orgPrefix + "-C" + counter.getId() + "-" + String.format("%03d", sequence) + suffix;
    }

    private void publishQueue(Long counterId) {
        queueEventPublisher.publishCounterUpdate(counterId, getCounterQueue(counterId));
    }
}
