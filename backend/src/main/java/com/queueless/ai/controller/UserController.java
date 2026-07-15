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
    private final com.queueless.ai.repository.UserRepository userRepository;

    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.success("Profile retrieved", authService.toUserResponse(SecurityUtils.currentUser().getUser()));
    }

    @org.springframework.web.bind.annotation.PutMapping("/me")
    @org.springframework.transaction.annotation.Transactional
    public ApiResponse<UserResponse> updateProfile(@org.springframework.web.bind.annotation.RequestBody com.queueless.ai.dto.AuthDtos.UpdateProfileRequest request) {
        com.queueless.ai.entity.User user = userRepository.findById(SecurityUtils.currentUser().getId())
                .orElseThrow(() -> new com.queueless.ai.exception.BadRequestException("User not found"));
        user.setPhone(request.phone() != null ? request.phone().trim() : null);
        user = userRepository.save(user);
        
        // Update the principal in SecurityContext if needed, though JWT is stateless.
        SecurityUtils.currentUser().getUser().setPhone(user.getPhone());

        return ApiResponse.success("Profile updated", authService.toUserResponse(user));
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
