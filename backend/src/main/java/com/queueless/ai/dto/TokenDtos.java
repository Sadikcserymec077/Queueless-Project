package com.queueless.ai.dto;

import com.queueless.ai.entity.TokenStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public class TokenDtos {
    public record TokenRequest(
            @NotNull Long counterId
    ) {
    }

    public record TokenResponse(
            Long id,
            String tokenNumber,
            Long userId,
            String userName,
            String userPhone,
            Long counterId,
            String counterName,
            Long organizationId,
            String organizationName,
            Instant bookingTime,
            Integer queuePosition,
            TokenStatus status,
            Integer estimatedWaitTimeMinutes,
            Instant expectedTurnTime,
            String qrPayload,
            String qrCodeData
    ) {
    }

    public record QueueStatusResponse(
            TokenResponse token,
            String currentTokenBeingServed,
            Integer peopleAhead,
            Integer estimatedWaitingTimeMinutes,
            Instant expectedTurnTime
    ) {
    }

    public record AdminQueueResponse(
            Long counterId,
            String counterName,
            String organizationName,
            TokenResponse currentToken,
            List<TokenResponse> waitingTokens,
            List<TokenResponse> skippedTokens,
            long waitingCount
    ) {
    }

    public record QrVerificationRequest(
            @NotBlank String qrPayload
    ) {
    }
}
