package com.gtmzero.service;

import com.gtmzero.dto.objection.CitationDto;
import com.gtmzero.service.CitationValidator.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CitationValidatorTest {

    private final CitationValidator validator = new CitationValidator();

    private static final List<CitationDto> FOUR_SOURCES = List.of(
            citation("[1]"),
            citation("[2]"),
            citation("[3]"),
            citation("[4]")
    );

    private static CitationDto citation(String marker) {
        return new CitationDto(marker, UUID.randomUUID(), "doc", "TECHNICAL_DOC", 0, "snippet");
    }

    // ── Original cases ──────────────────────────────────────────────────────

    @Test
    void perfectCitation_everySentenceCited() {
        String answer = "We encrypt all data at rest using AES-256 [1]. " +
                "Personal data lives in EU regions only [2]. " +
                "Access is gated by SSO [3].";

        ValidationResult r = validator.validate(answer, FOUR_SOURCES);

        assertThat(r.sentenceCount()).isEqualTo(3);
        assertThat(r.citedSentenceCount()).isEqualTo(3);
        assertThat(r.citationCoverage()).isEqualTo(1.0);
        assertThat(r.invalidMarkers()).isEmpty();
        assertThat(r.markersUsed()).containsExactlyInAnyOrder("[1]", "[2]", "[3]");
        assertThat(r.valid()).isTrue();
    }

    @Test
    void noCitations_coverageZero_invalid() {
        String answer = "We have strong security. Our system is robust. Trust us.";

        ValidationResult r = validator.validate(answer, FOUR_SOURCES);

        assertThat(r.sentenceCount()).isEqualTo(3);
        assertThat(r.citedSentenceCount()).isZero();
        assertThat(r.citationCoverage()).isZero();
        assertThat(r.markersUsed()).isEmpty();
        assertThat(r.valid()).isFalse();
    }

    @Test
    void mixedCitation_partialCoverage() {
        String answer = "We encrypt at rest [1]. We are robust. Personal data stays in EU [2]. Trust us.";

        ValidationResult r = validator.validate(answer, FOUR_SOURCES);

        assertThat(r.sentenceCount()).isEqualTo(4);
        assertThat(r.citedSentenceCount()).isEqualTo(2);
        assertThat(r.citationCoverage()).isEqualTo(0.5);
        assertThat(r.invalidMarkers()).isEmpty();
        assertThat(r.valid()).isFalse();
    }

    @Test
    void invalidMarker_detected() {
        String answer = "First claim [1]. Second claim [5]. Third claim [2].";

        ValidationResult r = validator.validate(answer, FOUR_SOURCES);

        assertThat(r.markersUsed()).contains("[5]");
        assertThat(r.invalidMarkers()).containsExactly("[5]");
        assertThat(r.valid()).isFalse();
    }

    @Test
    void markerBeforePeriod_stillCounted() {
        String answer = "Personal data is encrypted with AES-256 [1]. " +
                "Token storage uses HSM-backed keys for additional protection [2]. " +
                "All access events are audit-logged [3].";

        ValidationResult r = validator.validate(answer, FOUR_SOURCES);

        assertThat(r.sentenceCount()).isEqualTo(3);
        assertThat(r.citedSentenceCount()).isEqualTo(3);
        assertThat(r.citationCoverage()).isEqualTo(1.0);
    }

    @Test
    void emptyAnswer_invalid() {
        ValidationResult r = validator.validate("", FOUR_SOURCES);

        assertThat(r.valid()).isFalse();
        assertThat(r.sentenceCount()).isZero();
        assertThat(r.citedSentenceCount()).isZero();
        assertThat(r.citationCoverage()).isZero();
        assertThat(r.markersUsed()).isEmpty();
    }

    @Test
    void nullAnswer_invalid() {
        ValidationResult r = validator.validate(null, FOUR_SOURCES);
        assertThat(r.valid()).isFalse();
    }

    // ── New cases: markdown structural-line exclusion ────────────────────────

    @Test
    void markdownBoldHeaders_excludedFromDenominator() {
        // 3 bold headers + 5 sentences (4 cited, 1 not) → coverage = 4/5 = 0.8
        String answer = "**Layer 1 — Input Sanitization:**\n" +
                "All input is sanitized before reaching the LLM [1].\n" +
                "Regex rules catch known injection patterns [2].\n" +
                "**Layer 2 — Prompt Isolation:**\n" +
                "System and user messages are never concatenated [3].\n" +
                "**Layer 3 — Output Validation:**\n" +
                "Output must conform to a strict JSON schema [4].\n" +
                "This is our defence in depth approach.";

        ValidationResult r = validator.validate(answer, FOUR_SOURCES);

        // The 3 bold header lines are excluded from sentence counting.
        assertThat(r.excludedStructuralLines()).isEqualTo(3);
        assertThat(r.sentenceCount()).isEqualTo(5);
        assertThat(r.citedSentenceCount()).isEqualTo(4);
        assertThat(r.citationCoverage()).isEqualTo(0.8);
        assertThat(r.valid()).isTrue();
    }

    @Test
    void bulletListWithContent_bulletMarkersExcluded_contentCounted() {
        // Bare bullet markers ("-") excluded; the content lines are regular sentences.
        String answer = "We protect data through:\n" +
                "- \n" +
                "AES-256 encryption at rest [1].\n" +
                "- \n" +
                "TLS 1.3 for all transit [2].\n" +
                "- \n" +
                "Customer-managed keys on enterprise tier [3].";

        ValidationResult r = validator.validate(answer, FOUR_SOURCES);

        // 3 bare "-" lines are excluded.
        assertThat(r.excludedStructuralLines()).isGreaterThanOrEqualTo(3);
        // 4 content sentences: the intro + 3 data sentences.
        assertThat(r.sentenceCount()).isGreaterThanOrEqualTo(3);
        // At least the 3 cited content sentences count.
        assertThat(r.citedSentenceCount()).isGreaterThanOrEqualTo(3);
        assertThat(r.citationCoverage()).isGreaterThan(0.7);
    }

    @Test
    void mixedMarkdown_atxHeaderAndBoldAndSentences_headersExcluded() {
        // ATX heading excluded; bold header excluded; 2 sentences (both cited).
        String answer = "# Security Overview\n" +
                "**Encryption [1]:** We use AES-256-GCM at rest and TLS 1.3 in transit [1].\n" +
                "All API keys are stored in HashiCorp Vault [2].";

        ValidationResult r = validator.validate(answer, FOUR_SOURCES);

        // "# Security Overview" → 1 ATX header excluded.
        // "**Encryption [1]:**" → 1 bold-only header excluded.
        assertThat(r.excludedStructuralLines()).isGreaterThanOrEqualTo(1);
        // Remaining sentences carry citations.
        assertThat(r.citationCoverage()).isGreaterThanOrEqualTo(0.7);
        assertThat(r.valid()).isTrue();
    }
}
