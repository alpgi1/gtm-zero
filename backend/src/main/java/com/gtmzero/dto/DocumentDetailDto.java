package com.gtmzero.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Detailed view of one document with truncated chunk previews.
 * Used by the Documents admin sheet — never exposes the raw embedding.
 */
public record DocumentDetailDto(
        UUID id,
        String title,
        String sourceType,
        String sourcePath,
        int charCount,
        int chunkCount,
        Instant ingestedAt,
        List<ChunkPreview> chunks
) {
    public record ChunkPreview(
            UUID id,
            int chunkIndex,
            Integer tokenCount,
            String snippet
    ) {}
}
