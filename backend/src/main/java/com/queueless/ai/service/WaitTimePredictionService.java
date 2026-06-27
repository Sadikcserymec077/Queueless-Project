package com.queueless.ai.service;

import com.queueless.ai.entity.Token;
import com.queueless.ai.entity.TokenStatus;
import com.queueless.ai.repository.TokenRepository;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WaitTimePredictionService {

    private final TokenRepository tokenRepository;

    @Value("${app.queue.default-service-minutes}")
    private int defaultServiceMinutes;

    @Transactional(readOnly = true)
    public int estimateWaitMinutes(Long counterId, Instant bookingTime) {
        long waitingAhead = tokenRepository.countByCounterIdAndStatusAndBookingTimeBefore(
                counterId,
                TokenStatus.WAITING,
                bookingTime
        );
        long currentService = tokenRepository.findTopByCounterIdAndStatusOrderByCalledAtDesc(counterId, TokenStatus.CALLED)
                .map(token -> 1L)
                .orElse(0L);
        return Math.toIntExact((waitingAhead + currentService) * averageServiceMinutes(counterId));
    }

    @Transactional(readOnly = true)
    public int averageServiceMinutes(Long counterId) {
        return tokenRepository.findCompletedTokensForCounter(counterId)
                .stream()
                .limit(50)
                .mapToLong(this::serviceMinutes)
                .filter(minutes -> minutes > 0)
                .average()
                .stream()
                .mapToInt(value -> (int) Math.ceil(value))
                .findFirst()
                .orElse(defaultServiceMinutes);
    }

    private long serviceMinutes(Token token) {
        return Duration.between(token.getCalledAt(), token.getCompletedAt()).toMinutes();
    }
}
