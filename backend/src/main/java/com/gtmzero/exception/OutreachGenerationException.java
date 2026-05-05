package com.gtmzero.exception;

/**
 * Raised when the outreach-generation pipeline cannot produce a valid
 * subject + body pair from the LLM. Carries an error code so the
 * controller can map to an HTTP status without string-matching messages.
 *
 * <p>Codes:
 * <ul>
 *   <li>{@code LLM_UPSTREAM_FAILURE} — Anthropic API failed (network, 5xx,
 *       quota). Maps to 502.</li>
 *   <li>{@code OUTPUT_FORMAT_INVALID} — model returned text we couldn't
 *       parse as the required JSON envelope, even after one retry.
 *       Maps to 422.</li>
 * </ul>
 */
public class OutreachGenerationException extends RuntimeException {

    public static final String LLM_UPSTREAM_FAILURE = "LLM_UPSTREAM_FAILURE";
    public static final String OUTPUT_FORMAT_INVALID = "OUTPUT_FORMAT_INVALID";

    private final String code;

    public OutreachGenerationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public OutreachGenerationException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
