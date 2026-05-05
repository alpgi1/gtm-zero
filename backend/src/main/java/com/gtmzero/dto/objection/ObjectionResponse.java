package com.gtmzero.dto.objection;

import java.util.List;
import java.util.UUID;

/**
 * Final, materialized response from the objection-handling pipeline.
 * Used as the JSON body of the non-streaming endpoint and as the payload
 * of the SSE Completed event.
 */
public record ObjectionResponse(
        UUID queryId,
        String question,
        String answer,
        List<CitationDto> citations,
        int retrievedCount,
        long firstTokenLatencyMs,
        long totalLatencyMs,
        String model
) {}
