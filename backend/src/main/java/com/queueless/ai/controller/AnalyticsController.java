package com.queueless.ai.controller;

import com.queueless.ai.dto.AnalyticsDtos.DashboardStatsResponse;
import com.queueless.ai.dto.ApiResponse;
import com.queueless.ai.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import com.queueless.ai.security.UserPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'SUB_ADMIN')")
    public ApiResponse<DashboardStatsResponse> dashboard(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success("Dashboard analytics retrieved", analyticsService.dashboard(principal));
    }
}
