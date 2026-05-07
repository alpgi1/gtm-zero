package com.gtmzero.dto;

import java.time.Instant;

/**
 * Response from POST /warmup. Reports which subsystems are now hot and how
 * long each took on the cold path. Always 200 OK — partial failure is fine.
 */
public record WarmupResultDto(
        boolean embeddingWarm,
        boolean llmWarm,
        long embeddingLatencyMs,
        long llmLatencyMs,
        long totalMs,
        boolean cached,
        Instant warmedAt
) {}
