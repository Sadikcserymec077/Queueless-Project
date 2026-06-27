package com.queueless.ai.service;

import com.queueless.ai.dto.OrganizationDtos.OrganizationRequest;
import com.queueless.ai.dto.OrganizationDtos.OrganizationResponse;
import com.queueless.ai.dto.PageResponse;
import com.queueless.ai.entity.CounterStatus;
import com.queueless.ai.entity.Organization;
import com.queueless.ai.entity.OrganizationType;
import com.queueless.ai.exception.ResourceNotFoundException;
import com.queueless.ai.repository.CounterRepository;
import com.queueless.ai.repository.OrganizationRepository;
import com.queueless.ai.repository.TokenRepository;
import com.queueless.ai.repository.UserRepository;
import com.queueless.ai.entity.User;
import com.queueless.ai.entity.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final CounterRepository counterRepository;
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PageResponse<OrganizationResponse> search(String q, OrganizationType type, boolean includeInactive, int page, int size) {
        Page<Organization> organizations = organizationRepository.search(
                blankToNull(q),
                type,
                includeInactive,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"))
        );
        return new PageResponse<>(
                organizations.getContent().stream().map(this::toResponse).toList(),
                organizations.getNumber(),
                organizations.getSize(),
                organizations.getTotalElements(),
                organizations.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public OrganizationResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public OrganizationResponse create(OrganizationRequest request) {
        Organization organization = Organization.builder()
                .name(request.name().trim())
                .type(request.type())
                .address(request.address().trim())
                .contactNumber(request.contactNumber().trim())
                .email(request.email().trim().toLowerCase())
                .workingHours(request.workingHours().trim())
                .active(true)
                .build();
                
        Organization savedOrganization = organizationRepository.save(organization);
        
        if (request.adminEmail() != null && !request.adminEmail().isBlank() && request.adminPassword() != null) {
            if (userRepository.existsByEmail(request.adminEmail().trim().toLowerCase())) {
                throw new IllegalArgumentException("User with email " + request.adminEmail() + " already exists.");
            }
            User admin = User.builder()
                    .name(request.adminName() != null ? request.adminName().trim() : "Org Admin")
                    .email(request.adminEmail().trim().toLowerCase())
                    .password(passwordEncoder.encode(request.adminPassword()))
                    .role(Role.ORG_ADMIN)
                    .organization(savedOrganization)
                    .emailVerified(true)
                    .build();
            userRepository.save(admin);
            savedOrganization.setCreator(admin);
            savedOrganization = organizationRepository.save(savedOrganization);
        }
        
        return toResponse(savedOrganization);
    }

    @Transactional
    public OrganizationResponse update(Long id, OrganizationRequest request) {
        Organization organization = find(id);
        organization.setName(request.name().trim());
        organization.setType(request.type());
        organization.setAddress(request.address().trim());
        organization.setContactNumber(request.contactNumber().trim());
        organization.setEmail(request.email().trim().toLowerCase());
        organization.setWorkingHours(request.workingHours().trim());
        return toResponse(organization);
    }

    @Transactional
    public void delete(Long id) {
        Organization organization = find(id);
        organization.setActive(false);
    }

    @Transactional
    public OrganizationResponse approve(Long id) {
        Organization organization = find(id);
        organization.setStatus(com.queueless.ai.entity.OrganizationStatus.APPROVED);
        return toResponse(organization);
    }

    @Transactional
    public OrganizationResponse reject(Long id) {
        Organization organization = find(id);
        organization.setStatus(com.queueless.ai.entity.OrganizationStatus.REJECTED);
        return toResponse(organization);
    }

    @Transactional(readOnly = true)
    public Organization find(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", id));
    }

    private OrganizationResponse toResponse(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getType(),
                organization.getAddress(),
                organization.getContactNumber(),
                organization.getEmail(),
                organization.getWorkingHours(),
                organization.isActive(),
                counterRepository.countByOrganizationIdAndStatus(organization.getId(), CounterStatus.ACTIVE),
                tokenRepository.countActiveByOrganizationId(organization.getId()),
                organization.getStatus()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
