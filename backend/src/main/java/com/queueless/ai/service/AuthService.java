package com.queueless.ai.service;

import com.queueless.ai.dto.AuthDtos.AuthResponse;
import com.queueless.ai.dto.AuthDtos.LoginRequest;
import com.queueless.ai.dto.AuthDtos.RegisterRequest;
import com.queueless.ai.dto.AuthDtos.UserResponse;
import com.queueless.ai.dto.AuthDtos.ForgotPasswordRequest;
import com.queueless.ai.dto.AuthDtos.ResetPasswordRequest;
import com.queueless.ai.entity.Role;
import com.queueless.ai.entity.User;
import com.queueless.ai.exception.BadRequestException;
import com.queueless.ai.repository.UserRepository;
import com.queueless.ai.security.JwtService;
import com.queueless.ai.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final NotificationService notificationService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(request.email().trim().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .emailVerificationToken(UUID.randomUUID().toString())
                .build();

        User saved = userRepository.save(user);
        
        try {
            String verifyUrl = "http://localhost:5555/verify-email?token=" + saved.getEmailVerificationToken();
            notificationService.notifyUser(saved, "Verify your email - QueueLess AI", "Hi " + saved.getName() + ",\n\nWelcome to QueueLess AI! Please verify your email by clicking the link below:\n\n" + verifyUrl + "\n\nBest Regards,\nQueueLess AI Team");
        } catch (Exception e) {
            // Ignore email errors so registration still succeeds
        }
        
        String token = jwtService.generateToken(new UserPrincipal(saved));
        return new AuthResponse(token, "Bearer", toUserResponse(saved));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password())
        );
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        
        if (!principal.getUser().isEmailVerified()) {
            throw new BadRequestException("Please verify your email before logging in.");
        }
        
        String token = jwtService.generateToken(principal);
        return new AuthResponse(token, "Bearer", toUserResponse(principal.getUser()));
    }

    public UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isEnabled());
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new BadRequestException("Email not found"));

        user.setPasswordResetToken(UUID.randomUUID().toString());
        user.setPasswordResetTokenExpiry(Instant.now().plus(1, ChronoUnit.HOURS));

        try {
            String resetUrl = "http://localhost:5555/reset-password?token=" + user.getPasswordResetToken();
            notificationService.notifyUser(user, "Password Reset Request", "Hi " + user.getName() + ",\n\nYou requested a password reset. Click the link below to set a new password:\n\n" + resetUrl + "\n\nIf you did not request this, please ignore this email.\n\nBest Regards,\nQueueLess AI Team");
        } catch (Exception e) {
            // Ignore email errors
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.token())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (user.getPasswordResetTokenExpiry() == null || user.getPasswordResetTokenExpiry().isBefore(Instant.now())) {
            throw new BadRequestException("Reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
    }
}
