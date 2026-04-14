package com.aigrs.backend.util;

import com.aigrs.backend.repository.GrievanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.UUID;

/**
 * Generates human-readable tracking IDs: {ORG_CODE}-{YEAR}-{6-digit-seq}
 * e.g. MUN-2026-000123
 */
@Component
@RequiredArgsConstructor
public class GrievanceIdGenerator {

    private final GrievanceRepository grievanceRepository;

    public synchronized String generate(String orgCode, UUID orgId) {
        int currentYear = Year.now().getValue();
        long count = grievanceRepository.countByOrgIdAndYear(orgId, currentYear);
        long nextSeq = count + 1;
        return String.format("%s-%d-%06d", orgCode.toUpperCase(), currentYear, nextSeq);
    }
}
