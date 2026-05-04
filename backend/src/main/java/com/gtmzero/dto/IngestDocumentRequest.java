package com.gtmzero.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for ingesting a document into the RAG corpus.
 * Used by POST /api/v1/admin/documents.
 */
public record IngestDocumentRequest(

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Source type is required")
        @Pattern(regexp = "README|TECHNICAL_DOC|API_SPEC|ARCHITECTURE|LEGAL_CORPUS",
                 message = "Source type must be one of: README, TECHNICAL_DOC, API_SPEC, ARCHITECTURE, LEGAL_CORPUS")
        String sourceType,

        String sourcePath,

        @NotBlank(message = "Raw content is required")
        @Size(min = 100, message = "Raw content must be at least 100 characters")
        String rawContent
) {}
