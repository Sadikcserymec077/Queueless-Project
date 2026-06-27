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
        if (tokenRepository.existsByUserIdAndCounterIdAndStatusIn(userId, counter.getId(), ACTIVE_STATUSES)) {
            throw new BadRequestException("You already have an active token for this counter");
        }

        Instant now = Instant.now();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        Instant startOfDay = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        long todayBookings = tokenRepository.countByUserIdAndBookingTimeBetween(userId, startOfDay, endOfDay);
        if (todayBookings >= 10) {
            throw new BadRequestException("You can only book up to 10 tokens per day across all organizations");
        }
        String tokenNumber = generateTokenNumber(counter, now);
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
                .build();

        Token saved = tokenRepository.save(token);
        notificationService.notifyUser(
                user,
                "Token booked",
                "Your token " + saved.getTokenNumber() + " has been booked for " + counter.getCounterName() + "."
        );
        publishQueue(counter.getId());
        queueEventPublisher.publishUserUpdate(userId, getQueueStatus(saved.getId(), userId, false));
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
        int peopleAhead = token.getStatus() == TokenStatus.WAITING
                ? Math.toIntExact(tokenRepository.countByCounterIdAndStatusAndBookingTimeBefore(
                        token.getCounter().getId(),
                        TokenStatus.WAITING,
                        token.getBookingTime()
                ) + (current == null ? 0 : 1))
                : 0;
        int waitMinutes = token.getStatus() == TokenStatus.WAITING
                ? peopleAhead * waitTimePredictionService.averageServiceMinutes(token.getCounter().getId())
                : 0;
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
        notificationService.notifyUser(token.getUser(), "Token cancelled", "Token " + token.getTokenNumber() + " has been cancelled.");
        publishQueue(token.getCounter().getId());
        queueEventPublisher.publishUserUpdate(token.getUser().getId(), toResponse(token));
        return toResponse(token);
    }

    @Transactional
    public TokenResponse callNext(Long counterId) {
        Counter counter = counterService.find(counterId);
        tokenRepository.findTopByCounterIdAndStatusOrderByCalledAtDesc(counterId, TokenStatus.CALLED)
                .ifPresent(token -> {
                    throw new BadRequestException("Complete or skip the current token before calling the next one");
                });
        Token next = tokenRepository.findTopByCounterIdAndStatusOrderByBookingTimeAsc(counterId, TokenStatus.WAITING)
                .orElseThrow(() -> new BadRequestException("No waiting tokens for " + counter.getCounterName()));
        next.setStatus(TokenStatus.CALLED);
        next.setCalledAt(Instant.now());
        next.setEstimatedWaitTime(0);
        notificationService.notifyUser(
                next.getUser(),
                "Your turn is ready",
                "Token " + next.getTokenNumber() + " is now being served at " + counter.getCounterName() + "."
        );
        publishQueue(counterId);
        queueEventPublisher.publishUserUpdate(next.getUser().getId(), getQueueStatus(next.getId(), next.getUser().getId(), false));
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
        notificationService.notifyUser(token.getUser(), "Service completed", "Token " + token.getTokenNumber() + " has been completed.");
        publishQueue(token.getCounter().getId());
        queueEventPublisher.publishUserUpdate(token.getUser().getId(), toResponse(token));
        return toResponse(token);
    }

    @Transactional
    public TokenResponse skip(Long tokenId) {
        Token token = find(tokenId);
        if (!ACTIVE_STATUSES.contains(token.getStatus())) {
            throw new BadRequestException("Only waiting or called tokens can be skipped");
        }
        token.setStatus(TokenStatus.SKIPPED);
        notificationService.notifyUser(token.getUser(), "Token skipped", "Token " + token.getTokenNumber() + " was skipped.");
        publishQueue(token.getCounter().getId());
        queueEventPublisher.publishUserUpdate(token.getUser().getId(), toResponse(token));
        return toResponse(token);
    }

    @Transactional(readOnly = true)
    public AdminQueueResponse getCounterQueue(Long counterId) {
        Counter counter = counterService.find(counterId);
        TokenResponse current = tokenRepository.findTopByCounterIdAndStatusOrderByCalledAtDesc(counterId, TokenStatus.CALLED)
                .map(this::toResponse)
                .orElse(null);
        List<TokenResponse> waiting = tokenRepository.findTop20ByCounterIdAndStatusOrderByBookingTimeAsc(counterId, TokenStatus.WAITING)
                .stream()
                .map(this::toResponse)
                .toList();
        return new AdminQueueResponse(
                counter.getId(),
                counter.getCounterName(),
                counter.getOrganization().getName(),
                current,
                waiting,
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
        Integer queuePosition = null;
        Integer estimatedWait = token.getEstimatedWaitTime();
        Instant expectedTurnTime = null;

        if (token.getStatus() == TokenStatus.WAITING) {
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
                token.getQrCodeData()
        );
    }

    @Transactional(readOnly = true)
    public Token find(Long id) {
        return tokenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Token", id));
    }

    private String generateTokenNumber(Counter counter, Instant now) {
        LocalDate date = LocalDate.ofInstant(now, ZoneOffset.UTC);
        Instant start = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        long sequence = tokenRepository.countByCounterIdAndBookingTimeBetween(counter.getId(), start, end) + 1;
        return "QL-" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-C" + counter.getCounterNumber() + "-"
                + String.format("%03d", sequence);
    }

    private void publishQueue(Long counterId) {
        queueEventPublisher.publishCounterUpdate(counterId, getCounterQueue(counterId));
    }
}
