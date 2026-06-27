package com.queueless.ai.dto;

import com.queueless.ai.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UserManagementDtos {
    
    public record AddUserRequest(
            @NotBlank @Email String email,
            @NotNull Role role
    ) {}

}
