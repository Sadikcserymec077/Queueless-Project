package com.queueless.ai.controller;

import com.queueless.ai.dto.ApiResponse;
import com.queueless.ai.dto.AuthDtos.UserResponse;
import com.queueless.ai.dto.UserManagementDtos.AddUserRequest;
import com.queueless.ai.security.SecurityUtils;
import com.queueless.ai.service.OrganizationUserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
public class OrganizationUserManagementController {

    private final OrganizationUserManagementService service;

    @PostMapping("/add")
    public ApiResponse<UserResponse> addUserToOrganization(@Valid @RequestBody AddUserRequest request) {
        String adminEmail = SecurityUtils.currentUser().getUsername();
        return ApiResponse.success("User added to organization", service.addUserToOrganization(request, adminEmail));
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> getOrganizationUsers() {
        String adminEmail = SecurityUtils.currentUser().getUsername();
        return ApiResponse.success("Organization users retrieved", service.getOrganizationUsers(adminEmail));
    }

    @DeleteMapping("/remove")
    public ApiResponse<UserResponse> removeUserFromOrganization(@RequestParam String targetUserEmail) {
        String adminEmail = SecurityUtils.currentUser().getUsername();
        return ApiResponse.success("User removed from organization", service.removeUserFromOrganization(targetUserEmail, adminEmail));
    }
}
