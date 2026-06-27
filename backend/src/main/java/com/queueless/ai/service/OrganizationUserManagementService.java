package com.queueless.ai.service;

import com.queueless.ai.dto.UserManagementDtos.AddUserRequest;
import com.queueless.ai.dto.AuthDtos.UserResponse;
import com.queueless.ai.entity.Organization;
import com.queueless.ai.entity.Role;
import com.queueless.ai.entity.User;
import com.queueless.ai.exception.ResourceNotFoundException;
import com.queueless.ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationUserManagementService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponse addUserToOrganization(AddUserRequest request, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", adminEmail));

        Organization adminOrg = admin.getOrganization();
        if (adminOrg == null) {
            throw new RuntimeException("You do not belong to any organization.");
        }

        User targetUser = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.email()));

        if (targetUser.getOrganization() != null && !targetUser.getOrganization().getId().equals(adminOrg.getId())) {
            throw new RuntimeException("User already belongs to another organization.");
        }

        targetUser.setOrganization(adminOrg);
        targetUser.setRole(request.role());
        targetUser.setEmailVerified(true);
        
        targetUser = userRepository.save(targetUser);
        return new UserResponse(targetUser.getId(), targetUser.getName(), targetUser.getEmail(), targetUser.getRole(), targetUser.isEnabled());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getOrganizationUsers(String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", adminEmail));

        Organization adminOrg = admin.getOrganization();
        if (adminOrg == null) {
            throw new RuntimeException("You do not belong to any organization.");
        }
        
        return userRepository.findAll().stream()
                .filter(u -> u.getOrganization() != null && u.getOrganization().getId().equals(adminOrg.getId()))
                .map(u -> new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.isEnabled()))
                .toList();
    }

    @Transactional
    public UserResponse removeUserFromOrganization(String targetUserEmail, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", adminEmail));

        Organization adminOrg = admin.getOrganization();
        if (adminOrg == null) {
            throw new RuntimeException("You do not belong to any organization.");
        }

        User targetUser = userRepository.findByEmail(targetUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", targetUserEmail));

        if (targetUser.getOrganization() == null || !targetUser.getOrganization().getId().equals(adminOrg.getId())) {
            throw new RuntimeException("User does not belong to your organization.");
        }

        targetUser.setOrganization(null);
        targetUser.setRole(Role.USER); // Reset role
        targetUser = userRepository.save(targetUser);
        return new UserResponse(targetUser.getId(), targetUser.getName(), targetUser.getEmail(), targetUser.getRole(), targetUser.isEnabled());
    }
}
