package com.queueless.ai.config;

import com.queueless.ai.entity.Counter;
import com.queueless.ai.entity.CounterStatus;
import com.queueless.ai.entity.Organization;
import com.queueless.ai.entity.OrganizationType;
import com.queueless.ai.entity.Role;
import com.queueless.ai.entity.User;
import com.queueless.ai.repository.CounterRepository;
import com.queueless.ai.repository.OrganizationRepository;
import com.queueless.ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final CounterRepository counterRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled}")
    private boolean seedEnabled;

    @Bean
    CommandLineRunner migrateOldUsers() {
        return args -> {
            var unverified = userRepository.findAll().stream().filter(u -> !u.isEmailVerified()).toList();
            if (!unverified.isEmpty()) {
                unverified.forEach(u -> u.setEmailVerified(true));
                userRepository.saveAll(unverified);
            }
        };
    }

    @Bean
    CommandLineRunner seedDemoData() {
        return args -> {
            if (!seedEnabled || userRepository.count() > 0) {
                return;
            }

            User superAdmin = userRepository.save(User.builder()
                    .name("Super Admin")
                    .email("superadmin@queueless.ai")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.SUPER_ADMIN)
                    .emailVerified(true)
                    .build());

            User orgAdminHospital = userRepository.save(User.builder()
                    .name("Hospital Admin")
                    .email("admin@citycare.example")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ORG_ADMIN)
                    .emailVerified(true)
                    .build());

            User doctor = userRepository.save(User.builder()
                    .name("Dr. Smith")
                    .email("smith@citycare.example")
                    .password(passwordEncoder.encode("user123"))
                    .role(Role.DOCTOR)
                    .emailVerified(true)
                    .build());

            User user = userRepository.save(User.builder()
                    .name("Demo User")
                    .email("user@queueless.ai")
                    .password(passwordEncoder.encode("user123"))
                    .role(Role.USER)
                    .emailVerified(true)
                    .build());

            Organization hospital = organizationRepository.save(Organization.builder()
                    .name("CityCare Hospital")
                    .type(OrganizationType.HOSPITAL)
                    .address("12 Health Avenue, Central City")
                    .contactNumber("+1-555-0101")
                    .email("desk@citycare.example")
                    .workingHours("08:00 AM - 08:00 PM")
                    .active(true)
                    .creator(orgAdminHospital)
                    .build());

            orgAdminHospital.setOrganization(hospital);
            doctor.setOrganization(hospital);
            userRepository.save(orgAdminHospital);
            userRepository.save(doctor);

            User orgAdminBank = userRepository.save(User.builder()
                    .name("Bank Admin")
                    .email("admin@metrotrust.example")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ORG_ADMIN)
                    .emailVerified(true)
                    .build());

            Organization bank = organizationRepository.save(Organization.builder()
                    .name("MetroTrust Bank")
                    .type(OrganizationType.BANK)
                    .address("44 Finance Street, Downtown")
                    .contactNumber("+1-555-0188")
                    .email("support@metrotrust.example")
                    .workingHours("09:00 AM - 05:00 PM")
                    .active(true)
                    .creator(orgAdminBank)
                    .build());

            orgAdminBank.setOrganization(bank);
            userRepository.save(orgAdminBank);

            counterRepository.save(Counter.builder()
                    .organization(hospital)
                    .counterName("General OPD")
                    .counterNumber(1)
                    .serviceType("Consultation")
                    .status(CounterStatus.ACTIVE)
                    .build());

            counterRepository.save(Counter.builder()
                    .organization(hospital)
                    .counterName("Diagnostics")
                    .counterNumber(2)
                    .serviceType("Lab Reports")
                    .status(CounterStatus.ACTIVE)
                    .build());

            counterRepository.save(Counter.builder()
                    .organization(bank)
                    .counterName("Account Services")
                    .counterNumber(1)
                    .serviceType("KYC and account updates")
                    .status(CounterStatus.ACTIVE)
                    .build());
        };
    }
}
