package com.gtmzero.dto.objection;

import java.util.UUID;

/**
 * One source citation displayed alongside an objection answer.
 * The marker (e.g. "[1]") corresponds to the inline marker used in the answer text.
 */
public record CitationDto(
        String marker,
        UUID chunkId,
        String documentTitle,
        String sourceType,
        int chunkIndex,
        String snippet
) {}
