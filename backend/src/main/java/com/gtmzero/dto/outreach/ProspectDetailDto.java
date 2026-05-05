package com.gtmzero.dto.outreach;

import java.util.List;

public record ProspectDetailDto(
        ProspectSummaryDto prospect,
        List<OutreachHistoryDto> outreachHistory
) {}
