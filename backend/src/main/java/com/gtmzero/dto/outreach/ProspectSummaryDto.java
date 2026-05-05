package com.gtmzero.dto.outreach;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProspectSummaryDto(
        UUID id,
        String fullName,
        String role,
        String companyName,
        String companyDomain,
        String linkedinUrl,
        String githubUrl,
        List<String> techStackSignals,
        int outreachCount,
        Instant createdAt
) {}
