package com.gtmzero.dto.dashboard;

public record DashboardMetricsDto(
        int outreachSent,
        int meetingsBooked,
        int meetingsBookedDelta,
        long pipelineCreatedEur
) {}
