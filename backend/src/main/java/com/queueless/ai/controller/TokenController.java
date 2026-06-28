package com.queueless.ai.controller;

import com.queueless.ai.dto.ApiResponse;
import com.queueless.ai.dto.PageResponse;
import com.queueless.ai.dto.TokenDtos.AdminQueueResponse;
import com.queueless.ai.dto.TokenDtos.QrVerificationRequest;
import com.queueless.ai.dto.TokenDtos.QueueStatusResponse;
import com.queueless.ai.dto.TokenDtos.TokenRequest;
import com.queueless.ai.dto.TokenDtos.TokenResponse;
import com.queueless.ai.entity.Role;
import com.queueless.ai.entity.TokenStatus;
import com.queueless.ai.security.SecurityUtils;
import com.queueless.ai.service.TokenService;
import jakarta.validation.Valid;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<TokenResponse> book(@Valid @RequestBody TokenRequest request) {
        return ApiResponse.success("Token booked", tokenService.bookToken(SecurityUtils.currentUser().getId(), request));
    }

    @GetMapping("/me/active")
    public ApiResponse<TokenResponse> activeToken() {
        return ApiResponse.success("Active token retrieved", tokenService.activeToken(SecurityUtils.currentUser().getId()));
    }

    @GetMapping("/{id}/status")
    public ApiResponse<QueueStatusResponse> status(@PathVariable Long id) {
        boolean admin = SecurityUtils.currentUser().getUser().getRole() == Role.SUPER_ADMIN || SecurityUtils.currentUser().getUser().getRole() == Role.ORG_ADMIN;
        return ApiResponse.success("Queue status retrieved", tokenService.getQueueStatus(id, SecurityUtils.currentUser().getId(), admin));
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<TokenResponse> cancel(@PathVariable Long id) {
        boolean admin = SecurityUtils.currentUser().getUser().getRole() == Role.SUPER_ADMIN || SecurityUtils.currentUser().getUser().getRole() == Role.ORG_ADMIN;
        return ApiResponse.success("Token cancelled", tokenService.cancel(id, SecurityUtils.currentUser().getId(), admin));
    }

    @GetMapping("/admin/search")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'SUB_ADMIN')")
    public ApiResponse<PageResponse<TokenResponse>> searchAdmin(
            @RequestParam(required = false) TokenStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success("Tokens retrieved", tokenService.searchAdmin(status, q, from, to, page, size));
    }

    @GetMapping("/counters/{counterId}/queue")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'SUB_ADMIN')")
    public ApiResponse<AdminQueueResponse> counterQueue(@PathVariable Long counterId) {
        return ApiResponse.success("Queue retrieved", tokenService.getCounterQueue(counterId));
    }

    @PostMapping("/counters/{counterId}/call-next")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'SUB_ADMIN', 'STAFF', 'DOCTOR')")
    public ApiResponse<TokenResponse> callNext(@PathVariable Long counterId) {
        return ApiResponse.success("Next token called", tokenService.callNext(counterId));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'SUB_ADMIN', 'STAFF', 'DOCTOR')")
    public ApiResponse<TokenResponse> complete(@PathVariable Long id) {
        return ApiResponse.success("Token completed", tokenService.complete(id));
    }

    @PatchMapping("/{id}/skip")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'SUB_ADMIN', 'STAFF', 'DOCTOR')")
    public ApiResponse<TokenResponse> skip(@PathVariable Long id) {
        return ApiResponse.success("Token skipped", tokenService.skip(id));
    }

    @PatchMapping("/{id}/requeue")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'SUB_ADMIN', 'STAFF', 'DOCTOR')")
    public ApiResponse<TokenResponse> requeue(@PathVariable Long id) {
        return ApiResponse.success("Token re-queued", tokenService.requeue(id));
    }

    @PostMapping("/{id}/request-cancel")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<Void> requestCancel(@PathVariable Long id) {
        tokenService.requestCancel(id, SecurityUtils.currentUser().getId());
        return ApiResponse.success("Cancellation request sent to organization", null);
    }

    @PostMapping("/{id}/request-delay")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<Void> requestDelay(@PathVariable Long id) {
        tokenService.requestDelay(id, SecurityUtils.currentUser().getId());
        return ApiResponse.success("Delay request sent to organization", null);
    }

    @PatchMapping("/{id}/delay")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'SUB_ADMIN', 'STAFF', 'DOCTOR')")
    public ApiResponse<TokenResponse> delay(@PathVariable Long id) {
        return ApiResponse.success("Token delayed", tokenService.delay(id));
    }

    @PostMapping("/verify-qr")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'SUB_ADMIN', 'STAFF', 'DOCTOR')")
    public ApiResponse<TokenResponse> verifyQr(@Valid @RequestBody QrVerificationRequest request) {
        return ApiResponse.success("Token verified", tokenService.verifyQr(request.qrPayload()));
    }
}
