package com.gtmzero.dto.dashboard;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DashboardActivityItemDto(
        UUID id,
        String eventType,
        String summary,
        Instant createdAt,
        List<DashboardCitationDto> citations
) {}
