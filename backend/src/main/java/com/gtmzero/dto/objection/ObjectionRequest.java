package com.gtmzero.dto.objection;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request payload for the objection-handling RAG pipeline.
 * Used by POST /api/v1/objections and POST /api/v1/objections/stream.
 */
public record ObjectionRequest(

        @NotBlank(message = "Question is required")
        @Size(min = 5, max = 1000, message = "Question must be between 5 and 1000 characters")
        String question,

        UUID sessionId,

        Integer topK
) {}
