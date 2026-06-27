package com.queueless.ai.dto;

import com.queueless.ai.entity.OrganizationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OrganizationDtos {
    public record OrganizationRequest(
            @NotBlank String name,
            @NotNull OrganizationType type,
            @NotBlank String address,
            @NotBlank String contactNumber,
            @Email @NotBlank String email,
            @NotBlank String workingHours,
            String adminName,
            String adminEmail,
            String adminPassword
    ) {
    }

    public record OrganizationResponse(
            Long id,
            String name,
            OrganizationType type,
            String address,
            String contactNumber,
            String email,
            String workingHours,
            boolean active,
            long activeCounters,
            long activeTokens,
            com.queueless.ai.entity.OrganizationStatus status
    ) {
    }
}
