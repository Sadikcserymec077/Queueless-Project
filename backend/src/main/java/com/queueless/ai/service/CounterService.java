package com.queueless.ai.service;

import com.queueless.ai.dto.CounterDtos.CounterRequest;
import com.queueless.ai.dto.CounterDtos.CounterResponse;
import com.queueless.ai.entity.Counter;
import com.queueless.ai.entity.CounterStatus;
import com.queueless.ai.entity.Organization;
import com.queueless.ai.entity.TokenStatus;
import com.queueless.ai.exception.BadRequestException;
import com.queueless.ai.exception.ResourceNotFoundException;
import com.queueless.ai.repository.CounterRepository;
import com.queueless.ai.repository.TokenRepository;
import java.util.List;
import com.queueless.ai.security.SecurityUtils;
import com.queueless.ai.entity.Role;
import com.queueless.ai.entity.User;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CounterService {

    private final CounterRepository counterRepository;
    private final TokenRepository tokenRepository;
    private final OrganizationService organizationService;

    @Transactional(readOnly = true)
    public List<CounterResponse> findByOrganization(Long organizationId) {
        return counterRepository.findByOrganizationIdOrderByCounterNumberAsc(organizationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CounterResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public CounterResponse create(CounterRequest request) {
        User currentUser = SecurityUtils.currentUser().getUser();
        if (currentUser.getRole() == Role.ORG_ADMIN) {
            if (currentUser.getOrganization() == null || !currentUser.getOrganization().getId().equals(request.organizationId())) {
                throw new AccessDeniedException("Not authorized to create counters for this organization");
            }
        }
        
        Organization organization = organizationService.find(request.organizationId());
        if (!organization.isActive()) {
            throw new BadRequestException("Cannot add counters to an inactive organization");
        }
        Counter counter = Counter.builder()
                .organization(organization)
                .counterName(request.counterName().trim())
                .counterNumber(request.counterNumber())
                .serviceType(request.serviceType().trim())
                .bookingFee(request.bookingFee() == null ? 0.0 : request.bookingFee())
                .status(request.status() == null ? CounterStatus.ACTIVE : request.status())
                .build();
        return toResponse(counterRepository.save(counter));
    }

    @Transactional
    public CounterResponse update(Long id, CounterRequest request) {
        Counter counter = find(id);
        
        User currentUser = SecurityUtils.currentUser().getUser();
        if (currentUser.getRole() == Role.ORG_ADMIN) {
            if (currentUser.getOrganization() == null || !currentUser.getOrganization().getId().equals(counter.getOrganization().getId()) || !currentUser.getOrganization().getId().equals(request.organizationId())) {
                throw new AccessDeniedException("Not authorized to modify this organization's counters");
            }
        }

        Organization organization = organizationService.find(request.organizationId());
        counter.setOrganization(organization);
        counter.setCounterName(request.counterName().trim());
        counter.setCounterNumber(request.counterNumber());
        counter.setServiceType(request.serviceType().trim());
        counter.setBookingFee(request.bookingFee() == null ? 0.0 : request.bookingFee());
        counter.setStatus(request.status() == null ? counter.getStatus() : request.status());
        return toResponse(counter);
    }

    @Transactional
    public CounterResponse disable(Long id) {
        Counter counter = find(id);
        
        User currentUser = SecurityUtils.currentUser().getUser();
        if (currentUser.getRole() == Role.ORG_ADMIN) {
            if (currentUser.getOrganization() == null || !currentUser.getOrganization().getId().equals(counter.getOrganization().getId())) {
                throw new AccessDeniedException("Not authorized to modify this organization's counters");
            }
        }

        counter.setStatus(CounterStatus.INACTIVE);
        return toResponse(counter);
    }

    @Transactional(readOnly = true)
    public Counter find(Long id) {
        return counterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Counter", id));
    }

    @Transactional
    public Counter findForUpdate(Long id) {
        return counterRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Counter", id));
    }

    public CounterResponse toResponse(Counter counter) {
        String currentToken = tokenRepository.findTopByCounterIdAndStatusOrderByCalledAtDesc(counter.getId(), TokenStatus.CALLED)
                .map(token -> token.getTokenNumber())
                .orElse(null);
        return new CounterResponse(
                counter.getId(),
                counter.getOrganization().getId(),
                counter.getOrganization().getName(),
                counter.getCounterName(),
                counter.getCounterNumber(),
                counter.getServiceType(),
                counter.getStatus(),
                tokenRepository.countByCounterIdAndStatus(counter.getId(), TokenStatus.WAITING),
                currentToken,
                counter.getBookingFee()
        );
    }
}
