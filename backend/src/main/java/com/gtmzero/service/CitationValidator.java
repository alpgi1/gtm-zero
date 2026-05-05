package com.gtmzero.service;

import com.gtmzero.dto.objection.CitationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inline regex-based citation validator. NO LLM call.
 *
 * <p>Counts sentences vs cited sentences and detects markers that don't
 * map to a retrieved source. Markdown structural lines (headers, bold-only
 * labels, empty bullets) are excluded from the denominator so they don't
 * artificially deflate coverage. Coverage below the warning threshold is
 * logged but never blocks the response — the pitch can't have a
 * validator rejecting Claude on stage.
 */
@Component
@Slf4j
public class CitationValidator {

    /** Markers anywhere in a sentence: [1], [12], etc. */
    private static final Pattern MARKER_PATTERN = Pattern.compile("\\[(\\d+)]");

    /** Sentence end: punctuation followed by whitespace or end-of-string. */
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?])\\s+");

    /** ATX markdown header: # … */
    private static final Pattern MARKDOWN_HEADER = Pattern.compile("^#+\\s+.+$");

    /** Bold-only header line: **text**: — optionally followed by whitespace only. */
    private static final Pattern BOLD_HEADER = Pattern.compile("^\\s*\\*\\*[^*]+\\*\\*\\s*:?\\s*$");

    /** Pure list marker line: -, *, 1. / 1) with nothing after it. */
    private static final Pattern BARE_LIST_MARKER = Pattern.compile("^\\s*[-*]\\s*$|^\\s*\\d+[.):]\\s*$");

    private static final double WARN_THRESHOLD = 0.7;

    public ValidationResult validate(String answer, List<CitationDto> availableCitations) {
        if (answer == null || answer.isBlank()) {
            return new ValidationResult(false, Set.of(), Set.of(), 0, 0, 0.0, 0);
        }

        Set<String> validMarkers = new HashSet<>();
        for (CitationDto c : availableCitations) validMarkers.add(c.marker());

        Set<String> markersUsed = new HashSet<>();
        Set<String> invalidMarkers = new HashSet<>();
        Matcher allMatcher = MARKER_PATTERN.matcher(answer);
        while (allMatcher.find()) {
            String marker = "[" + allMatcher.group(1) + "]";
            markersUsed.add(marker);
            if (!validMarkers.contains(marker)) invalidMarkers.add(marker);
        }

        // Split into lines first; filter out structural lines before sentence-splitting.
        String[] lines = answer.split("\\r?\\n");
        int excludedStructuralLines = 0;
        StringBuilder contentOnly = new StringBuilder(answer.length());

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()
                    || MARKDOWN_HEADER.matcher(trimmed).matches()
                    || BOLD_HEADER.matcher(trimmed).matches()
                    || BARE_LIST_MARKER.matcher(trimmed).matches()) {
                excludedStructuralLines++;
                continue;
            }
            contentOnly.append(trimmed).append(" ");
        }

        String[] rawSentences = SENTENCE_SPLIT.split(contentOnly.toString().trim());
        int sentenceCount = 0;
        int citedSentenceCount = 0;

        for (String sentence : rawSentences) {
            String s = sentence.trim();
            if (s.isEmpty()) continue;
            sentenceCount++;
            if (sentenceContainsMarker(s)) citedSentenceCount++;
        }

        double coverage = sentenceCount == 0 ? 0.0 : (double) citedSentenceCount / sentenceCount;
        boolean valid = invalidMarkers.isEmpty() && coverage >= WARN_THRESHOLD;

        if (coverage < WARN_THRESHOLD) {
            log.warn("Citation coverage below threshold: {}/{} sentences cited ({})",
                    citedSentenceCount, sentenceCount,
                    String.format("%.2f", coverage));
        }
        if (!invalidMarkers.isEmpty()) {
            log.warn("Answer contains invalid citation markers: {}", invalidMarkers);
        }

        log.debug("Citation validation: {}/{} sentences cited (coverage={}), {} structural lines excluded, markers used={}",
                citedSentenceCount, sentenceCount,
                String.format("%.2f", coverage),
                excludedStructuralLines, markersUsed);

        return new ValidationResult(
                valid, markersUsed, invalidMarkers,
                sentenceCount, citedSentenceCount, coverage,
                excludedStructuralLines
        );
    }

    private static boolean sentenceContainsMarker(String sentence) {
        return MARKER_PATTERN.matcher(sentence).find();
    }

    public record ValidationResult(
            boolean valid,
            Set<String> markersUsed,
            Set<String> invalidMarkers,
            int sentenceCount,
            int citedSentenceCount,
            double citationCoverage,
            int excludedStructuralLines
    ) {}
}
