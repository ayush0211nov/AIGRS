package com.aigrs.backend.service;

import com.aigrs.backend.entity.Organization;
import com.aigrs.backend.entity.User;
import com.aigrs.backend.enums.UserRole;
import com.aigrs.backend.exception.DuplicateResourceException;
import com.aigrs.backend.exception.ResourceNotFoundException;
import com.aigrs.backend.repository.GrievanceRepository;
import com.aigrs.backend.repository.OrganizationRepository;
import com.aigrs.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final GrievanceRepository grievanceRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Map<String, Object> createOrganization(Organization org, String adminEmail, String adminPhone, String adminPassword) {
        if (organizationRepository.existsByCode(org.getCode())) {
            throw new DuplicateResourceException("Organization code already exists");
        }

        // Set default SLA config
        if (org.getSlaConfig() == null) {
            org.setSlaConfig("{\"LOW\":72,\"MEDIUM\":48,\"HIGH\":24,\"CRITICAL\":4}");
        }
        org.setOrgId(null); // Super admin context, no parent org

        org = organizationRepository.save(org);

        // Create admin user for this org
        User admin = User.builder()
                .name(org.getName() + " Admin")
                .email(adminEmail)
                .phone(adminPhone)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(UserRole.ADMIN)
                .build();
        admin.setOrgId(org.getId());
        userRepository.save(admin);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("organization", org);
        result.put("adminUserId", admin.getId());
        return result;
    }

    public Organization getOrganization(UUID id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }

    @Transactional
    public Organization updateOrganization(UUID id, Organization update) {
        Organization org = getOrganization(id);
        if (update.getName() != null) org.setName(update.getName());
        if (update.getLogoUrl() != null) org.setLogoUrl(update.getLogoUrl());
        if (update.getAddress() != null) org.setAddress(update.getAddress());
        if (update.getContactEmail() != null) org.setContactEmail(update.getContactEmail());
        if (update.getContactPhone() != null) org.setContactPhone(update.getContactPhone());
        if (update.getSlaConfig() != null) org.setSlaConfig(update.getSlaConfig());
        if (update.getEscalationConfig() != null) org.setEscalationConfig(update.getEscalationConfig());
        return organizationRepository.save(org);
    }

    @Transactional
    public void suspendOrganization(UUID id) {
        Organization org = getOrganization(id);
        org.setIsActive(false);
        organizationRepository.save(org);
    }

    public Map<String, Object> getUsageStats(UUID id) {
        Organization org = getOrganization(id);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("organizationName", org.getName());
        stats.put("totalUsers", userRepository.countByOrgId(id));
        stats.put("totalGrievances", grievanceRepository.countByOrgIdAndDeletedAtIsNull(id));
        return stats;
    }
}
