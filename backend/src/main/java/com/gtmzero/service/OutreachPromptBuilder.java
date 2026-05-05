package com.gtmzero.service;

import com.gtmzero.dto.outreach.GenerateOutreachRequest;
import com.gtmzero.service.ProspectUrlSignalExtractor.ExtractedSignals;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Composes the (system, user) prompt pair for outreach generation.
 *
 * <p>The system prompt is loaded once from
 * {@code resources/prompts/outreach.system.txt} so it can be edited without
 * rebuilding source. The user prompt is built per request from the
 * prospect fields + URL-derived signals.
 *
 * <p>{@link BuiltOutreachPrompt#promptVersion()} is hardcoded to {@code v1.0}
 * and persisted on every {@code OutreachMessage} — this lets us track
 * generation-quality drift across prompt revisions over time.
 */
@Component
@Slf4j
public class OutreachPromptBuilder {

    public static final String PROMPT_VERSION = "v1.1";
    private static final String SYSTEM_PROMPT_RESOURCE = "prompts/outreach.system.txt";

    private String systemPrompt;

    @PostConstruct
    void loadSystemPrompt() {
        try (var in = new ClassPathResource(SYSTEM_PROMPT_RESOURCE).getInputStream()) {
            this.systemPrompt = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
            log.info("Loaded outreach system prompt ({} chars) from {}",
                    systemPrompt.length(), SYSTEM_PROMPT_RESOURCE);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load outreach system prompt from " + SYSTEM_PROMPT_RESOURCE, e);
        }
    }

    public BuiltOutreachPrompt build(GenerateOutreachRequest request, ExtractedSignals signals) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("PROSPECT:\n");
        sb.append("- Name: ").append(orUnknown(request.fullName())).append('\n');
        sb.append("- Role: ").append(orUnknown(request.role())).append('\n');
        sb.append("- Company: ").append(request.companyName()).append('\n');
        sb.append("- Company domain: ").append(orUnknown(request.companyDomain())).append('\n');
        sb.append("- LinkedIn: ").append(orNotProvided(request.linkedinUrl())).append('\n');
        sb.append("- GitHub: ").append(orNotProvided(request.githubUrl())).append('\n');
        sb.append("- Tech stack signals: ").append(joinOrNone(request.techStackSignals())).append('\n');
        sb.append("- Context notes: ").append(orNone(request.contextNotes())).append("\n\n");

        sb.append("EXTRACTED HANDLE SIGNALS (from URL parsing only, may be sparse):\n");
        sb.append("- ").append(joinOrNone(signals.heuristicSignals())).append("\n\n");

        sb.append("Write the outreach now. Output JSON only.");

        return new BuiltOutreachPrompt(systemPrompt, sb.toString(), PROMPT_VERSION);
    }

    public record BuiltOutreachPrompt(String systemPrompt, String userPrompt, String promptVersion) {}

    // Visible for tests.
    String getSystemPrompt() {
        return systemPrompt;
    }

    private static String orUnknown(String s) {
        return (s == null || s.isBlank()) ? "unknown" : s;
    }

    private static String orNotProvided(String s) {
        return (s == null || s.isBlank()) ? "not provided" : s;
    }

    private static String orNone(String s) {
        return (s == null || s.isBlank()) ? "none" : s;
    }

    private static String joinOrNone(List<String> values) {
        if (values == null || values.isEmpty()) return "none provided";
        return String.join(", ", values);
    }
}
