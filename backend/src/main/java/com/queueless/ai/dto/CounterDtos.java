package com.queueless.ai.dto;

import com.queueless.ai.entity.CounterStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CounterDtos {
    public record CounterRequest(
            @NotNull Long organizationId,
            @NotBlank String counterName,
            @NotNull @Min(1) Integer counterNumber,
            @NotBlank String serviceType,
            CounterStatus status
    ) {
    }

    public record CounterResponse(
            Long id,
            Long organizationId,
            String organizationName,
            String counterName,
            Integer counterNumber,
            String serviceType,
            CounterStatus status,
            long waitingTokens,
            String currentToken
    ) {
    }
}
