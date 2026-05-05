package com.gtmzero.dto.objection;

import java.time.Instant;
import java.util.UUID;

/**
 * Slim projection used by the "recent activity" feed (Part 6 dashboard).
 * Excludes the full retrieved-chunk-id array to keep payloads compact.
 */
public record RecentObjectionResponse(
        UUID id,
        UUID sessionId,
        String question,
        String answer,
        int citationCount,
        Integer firstTokenLatencyMs,
        Integer totalLatencyMs,
        String model,
        Instant createdAt
) {}
