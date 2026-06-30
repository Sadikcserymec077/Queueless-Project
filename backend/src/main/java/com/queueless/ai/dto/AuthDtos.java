package com.queueless.ai.dto;

import com.queueless.ai.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {
    public record RegisterRequest(
            @NotBlank String name,
            @Email @NotBlank String email,
            @Size(min = 6, max = 80) String password,
            @NotBlank String phone
    ) {
    }

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {
    }

    public record GoogleLoginRequest(
            @NotBlank String token
    ) {
    }

    public record ForgotPasswordRequest(
            @Email @NotBlank String email
    ) {
    }

    public record ResetPasswordRequest(
            @NotBlank String token,
            @Size(min = 6, max = 80) String newPassword
    ) {
    }

    public record UpdateProfileRequest(
            String phone
    ) {
    }

    public record UserResponse(
            Long id,
            String name,
            String email,
            String phone,
            Role role,
            boolean enabled,
            Long organizationId,
            String organizationName
    ) {
    }

    public record AuthResponse(
            String token,
            String type,
            UserResponse user
    ) {
    }
}
