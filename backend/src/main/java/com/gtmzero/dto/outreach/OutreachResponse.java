package com.gtmzero.dto.outreach;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Final response from POST /outreach/generate and POST /outreach/{id}/send-mock.
 */
public record OutreachResponse(
        UUID outreachId,
        UUID prospectId,
        String prospectFullName,
        String prospectRole,
        String prospectCompany,
        List<String> usedSignals,
        String subject,
        String body,
        String model,
        String generationPromptVersion,
        long generationLatencyMs,
        String status,
        Instant createdAt
) {}
