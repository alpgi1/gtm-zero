package com.gtmzero.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure-heuristic URL inspection — never makes an HTTP call.
 *
 * <p>For the MVP we deliberately don't pretend to know more about a prospect
 * than the URL string itself reveals: a LinkedIn handle, a GitHub handle,
 * and a flag-style "this is a public profile" signal. The heavy lifting
 * for personalization comes from {@code contextNotes} + {@code techStackSignals}
 * supplied by the caller; these heuristics are light flavoring.
 *
 * <p>Malformed input never throws — we log a warning and return empty
 * fields. Callers can always fall back to manual identity (name + role).
 */
@Component
@Slf4j
public class ProspectUrlSignalExtractor {

    public record ExtractedSignals(
            String inferredHandle,
            String inferredCompanyHint,
            List<String> heuristicSignals
    ) {
        public static ExtractedSignals empty() {
            return new ExtractedSignals(null, null, List.of());
        }
    }

    public ExtractedSignals extract(String linkedinUrl, String githubUrl) {
        String handle = null;
        String companyHint = null;
        List<String> signals = new ArrayList<>(2);

        if (linkedinUrl != null && !linkedinUrl.isBlank()) {
            String[] parsed = parseLinkedin(linkedinUrl);
            if (parsed != null) {
                if (parsed[0] != null) handle = parsed[0];
                if (parsed[1] != null) companyHint = parsed[1];
                signals.add("linkedin-profile-public");
            }
        }

        if (githubUrl != null && !githubUrl.isBlank()) {
            String gh = parseGithubHandle(githubUrl);
            if (gh != null) {
                // Don't overwrite a LinkedIn handle if both URLs were supplied —
                // LinkedIn names tend to be closer to the prospect's real name.
                if (handle == null) handle = gh;
                signals.add("open-source-engineer");
            }
        }

        return new ExtractedSignals(handle, companyHint, List.copyOf(signals));
    }

    /**
     * Returns [handle, companyHint] or null if the URL is unparseable.
     * LinkedIn URLs come in two relevant flavours:
     *   linkedin.com/in/<handle>
     *   linkedin.com/company/<slug>
     */
    private String[] parseLinkedin(String url) {
        URI uri = safeParse(url);
        if (uri == null) return null;
        String host = uri.getHost();
        if (host == null || !host.toLowerCase().contains("linkedin.com")) {
            log.warn("URL does not look like LinkedIn: {}", url);
            return null;
        }
        String path = uri.getPath();
        if (path == null) return new String[]{null, null};

        String[] parts = path.split("/");
        String handle = null;
        String company = null;
        for (int i = 0; i < parts.length - 1; i++) {
            String seg = parts[i];
            if ("in".equalsIgnoreCase(seg) && !parts[i + 1].isBlank()) {
                handle = stripTrailing(parts[i + 1]);
            }
            if ("company".equalsIgnoreCase(seg) && !parts[i + 1].isBlank()) {
                company = stripTrailing(parts[i + 1]);
            }
        }
        return new String[]{handle, company};
    }

    private String parseGithubHandle(String url) {
        URI uri = safeParse(url);
        if (uri == null) return null;
        String host = uri.getHost();
        if (host == null || !host.toLowerCase().contains("github.com")) {
            log.warn("URL does not look like GitHub: {}", url);
            return null;
        }
        String path = uri.getPath();
        if (path == null || path.isBlank() || path.equals("/")) return null;
        String[] parts = path.split("/");
        for (String seg : parts) {
            if (!seg.isBlank()) return stripTrailing(seg);
        }
        return null;
    }

    private static URI safeParse(String url) {
        try {
            String normalized = url.contains("://") ? url : "https://" + url;
            return new URI(normalized);
        } catch (URISyntaxException e) {
            log.warn("Malformed URL ignored: {}", url);
            return null;
        }
    }

    private static String stripTrailing(String s) {
        int q = s.indexOf('?');
        if (q >= 0) s = s.substring(0, q);
        int h = s.indexOf('#');
        if (h >= 0) s = s.substring(0, h);
        return s;
    }
}
