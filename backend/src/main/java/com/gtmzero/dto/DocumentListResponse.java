package com.gtmzero.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for listing documents. Does not expose raw_content
 * or chunk embeddings — only metadata for admin views.
 */
public record DocumentListResponse(
        UUID id,
        String title,
        String sourceType,
        int charCount,
        int chunkCount,
        Instant ingestedAt,
        Instant createdAt
) {}
