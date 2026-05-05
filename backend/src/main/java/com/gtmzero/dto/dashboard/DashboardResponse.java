package com.gtmzero.dto.dashboard;

import java.util.List;

public record DashboardResponse(
        DashboardMetricsDto metrics,
        List<DashboardActivityItemDto> activityFeed
) {}
