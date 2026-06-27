package com.queueless.ai.controller;

import com.queueless.ai.dto.ApiResponse;
import com.queueless.ai.dto.AuthDtos.UserResponse;
import com.queueless.ai.dto.PageResponse;
import com.queueless.ai.dto.TokenDtos.TokenResponse;
import com.queueless.ai.security.SecurityUtils;
import com.queueless.ai.service.AuthService;
import com.queueless.ai.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final TokenService tokenService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.success("Profile retrieved", authService.toUserResponse(SecurityUtils.currentUser().getUser()));
    }

    @GetMapping("/history")
    public ApiResponse<PageResponse<TokenResponse>> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(
                "Booking history retrieved",
                tokenService.history(SecurityUtils.currentUser().getId(), page, size)
        );
    }
}
