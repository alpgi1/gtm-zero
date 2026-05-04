package com.gtmzero.dto;

import java.util.UUID;

/**
 * Result of a document ingestion operation.
 */
public record IngestResult(
        UUID documentId,
        String title,
        int chunkCount,
        long totalLatencyMs,
        boolean wasSkipped
) {}
