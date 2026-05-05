package com.gtmzero.dto.outreach;

import java.time.Instant;
import java.util.UUID;

public record OutreachHistoryDto(
        UUID id,
        UUID prospectId,
        String prospectFullName,
        String prospectCompany,
        String subject,
        String bodyPreview,
        String status,
        long generationLatencyMs,
        Instant createdAt
) {}
