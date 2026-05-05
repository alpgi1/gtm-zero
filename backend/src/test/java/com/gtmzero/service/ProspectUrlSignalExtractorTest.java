package com.gtmzero.service;

import com.gtmzero.service.ProspectUrlSignalExtractor.ExtractedSignals;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-unit coverage for the URL heuristics. No Spring context, no I/O.
 */
class ProspectUrlSignalExtractorTest {

    private final ProspectUrlSignalExtractor extractor = new ProspectUrlSignalExtractor();

    @Test
    void linkedinOnly_extractsHandleAndSignal() {
        ExtractedSignals s = extractor.extract("https://linkedin.com/in/jane-doe", null);
        assertThat(s.inferredHandle()).isEqualTo("jane-doe");
        assertThat(s.heuristicSignals()).containsExactly("linkedin-profile-public");
        assertThat(s.inferredCompanyHint()).isNull();
    }

    @Test
    void linkedinCompanyPath_capturesCompanyHint() {
        ExtractedSignals s = extractor.extract("https://www.linkedin.com/company/acme-robotics/", null);
        assertThat(s.inferredCompanyHint()).isEqualTo("acme-robotics");
        assertThat(s.heuristicSignals()).containsExactly("linkedin-profile-public");
    }

    @Test
    void githubOnly_extractsHandleAndSignal() {
        ExtractedSignals s = extractor.extract(null, "https://github.com/octocat");
        assertThat(s.inferredHandle()).isEqualTo("octocat");
        assertThat(s.heuristicSignals()).containsExactly("open-source-engineer");
    }

    @Test
    void bothUrls_emitBothSignalsAndPreferLinkedinHandle() {
        ExtractedSignals s = extractor.extract(
                "https://linkedin.com/in/jane-doe",
                "https://github.com/jdoe");
        assertThat(s.inferredHandle()).isEqualTo("jane-doe");
        assertThat(s.heuristicSignals())
                .containsExactly("linkedin-profile-public", "open-source-engineer");
    }

    @Test
    void neitherUrl_returnsEmpty() {
        ExtractedSignals s = extractor.extract(null, "");
        assertThat(s.inferredHandle()).isNull();
        assertThat(s.inferredCompanyHint()).isNull();
        assertThat(s.heuristicSignals()).isEmpty();
    }

    @Test
    void malformedUrl_doesNotThrow_returnsEmpty() {
        ExtractedSignals s = extractor.extract("not a url at all >>", null);
        // Either malformed (no host) or our domain check rejects it — either way: no signals.
        assertThat(s.heuristicSignals()).isEmpty();
        assertThat(s.inferredHandle()).isNull();
    }

    @Test
    void wrongDomain_isRejected() {
        ExtractedSignals s = extractor.extract(
                "https://example.com/in/who-knows",
                "https://gitlab.com/octocat");
        assertThat(s.heuristicSignals()).isEmpty();
        assertThat(s.inferredHandle()).isNull();
    }

    @Test
    void linkedinUrl_stripsQueryString() {
        ExtractedSignals s = extractor.extract(
                "https://linkedin.com/in/jane-doe?ref=feed&utm=src", null);
        assertThat(s.inferredHandle()).isEqualTo("jane-doe");
    }
}
