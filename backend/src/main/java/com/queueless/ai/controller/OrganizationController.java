package com.queueless.ai.controller;

import com.queueless.ai.dto.ApiResponse;
import com.queueless.ai.dto.OrganizationDtos.OrganizationRequest;
import com.queueless.ai.dto.OrganizationDtos.OrganizationResponse;
import com.queueless.ai.dto.PageResponse;
import com.queueless.ai.entity.OrganizationType;
import com.queueless.ai.entity.Role;
import com.queueless.ai.security.SecurityUtils;
import com.queueless.ai.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    public ApiResponse<PageResponse<OrganizationResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) OrganizationType type,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        boolean adminCanSeeInactive = includeInactive && SecurityUtils.currentUser().getUser().getRole() == Role.SUPER_ADMIN;
        return ApiResponse.success("Organizations retrieved", organizationService.search(q, type, adminCanSeeInactive, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrganizationResponse> get(@PathVariable Long id) {
        return ApiResponse.success("Organization retrieved", organizationService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<OrganizationResponse> create(@Valid @RequestBody OrganizationRequest request) {
        return ApiResponse.success("Organization created", organizationService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ORG_ADMIN')")
    public ApiResponse<OrganizationResponse> update(@PathVariable Long id, @Valid @RequestBody OrganizationRequest request) {
        return ApiResponse.success("Organization updated", organizationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        organizationService.delete(id);
        return ApiResponse.success("Organization disabled", null);
    }
    
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<OrganizationResponse> approve(@PathVariable Long id) {
        return ApiResponse.success("Organization approved", organizationService.approve(id));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<OrganizationResponse> reject(@PathVariable Long id) {
        return ApiResponse.success("Organization rejected", organizationService.reject(id));
    }
}
