package com.gtmzero.service;

import com.gtmzero.dto.outreach.GenerateOutreachRequest;
import com.gtmzero.dto.outreach.OutreachResponse;
import com.gtmzero.entity.AuditLog;
import com.gtmzero.entity.OutreachMessage;
import com.gtmzero.entity.Prospect;
import com.gtmzero.exception.OutreachGenerationException;
import com.gtmzero.repository.AuditLogRepository;
import com.gtmzero.repository.OutreachMessageRepository;
import com.gtmzero.service.OutreachPromptBuilder.BuiltOutreachPrompt;
import com.gtmzero.service.ProspectUrlSignalExtractor.ExtractedSignals;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * One LLM call → one cold outreach message.
 *
 * <p>Pipeline:
 * <ol>
 *   <li>Validate identity (linkedin/github URL OR fullName + role)</li>
 *   <li>Extract URL handle signals (no HTTP fetching)</li>
 *   <li>Find-or-create the {@link Prospect}</li>
 *   <li>Build (system, user) prompt and call Claude (non-streaming, temp 0.7)</li>
 *   <li>Parse JSON envelope; on failure, retry once with a stricter nudge</li>
 *   <li>Persist {@link OutreachMessage} + {@link AuditLog}</li>
 * </ol>
 *
 * <p>This is intentionally NOT a RAG pipeline. The Regu value-prop lives in
 * the system prompt; we proved RAG works in Part 4 and duplicating it here
 * would be overengineering for a peer-voice cold email.
 */
@Service
@Slf4j
public class OutreachService {

    private static final String EVENT_GENERATED = "OUTREACH_GENERATED";
    private static final String EVENT_SENT_MOCK = "OUTREACH_SENT_MOCK";
    private static final String STATUS_GENERATED = "GENERATED";
    private static final String STATUS_SENT_MOCK = "SENT_MOCK";
    private static final double GENERATION_TEMPERATURE = 0.7;
    private static final int GENERATION_MAX_TOKENS = 500;

    private final ProspectService prospectService;
    private final ProspectUrlSignalExtractor signalExtractor;
    private final OutreachPromptBuilder promptBuilder;
    private final ChatModel chatModel;
    private final OutreachMessageRepository outreachMessageRepository;
    private final AuditLogRepository auditLogRepository;
    private final JsonMapper jsonMapper;
    private final String modelName;

    public OutreachService(ProspectService prospectService,
                           ProspectUrlSignalExtractor signalExtractor,
                           OutreachPromptBuilder promptBuilder,
                           ChatModel chatModel,
                           OutreachMessageRepository outreachMessageRepository,
                           AuditLogRepository auditLogRepository,
                           JsonMapper jsonMapper,
                           @Value("${spring.ai.anthropic.chat.options.model:claude-sonnet-4-6}") String modelName) {
        this.prospectService = prospectService;
        this.signalExtractor = signalExtractor;
        this.promptBuilder = promptBuilder;
        this.chatModel = chatModel;
        this.outreachMessageRepository = outreachMessageRepository;
        this.auditLogRepository = auditLogRepository;
        this.jsonMapper = jsonMapper;
        this.modelName = modelName;
    }

    @Transactional
    public OutreachResponse generate(GenerateOutreachRequest request) {
        if (!request.hasMinimumIdentity()) {
            throw new IllegalArgumentException(
                    "Request must include at least one of: linkedinUrl, githubUrl, "
                            + "or both fullName and role.");
        }

        ExtractedSignals signals = signalExtractor.extract(request.linkedinUrl(), request.githubUrl());
        Prospect prospect = prospectService.findOrCreate(request);
        BuiltOutreachPrompt built = promptBuilder.build(request, signals);

        long startNanos = System.nanoTime();
        ParsedOutreach parsed = callAndParse(built, false);
        long latencyMs = (System.nanoTime() - startNanos) / 1_000_000L;

        OutreachMessage saved = OutreachMessage.builder()
                .prospect(prospect)
                .subject(parsed.subject)
                .body(parsed.body)
                .generationModel(modelName)
                .generationPromptVersion(built.promptVersion())
                .generationLatencyMs((int) latencyMs)
                .status(STATUS_GENERATED)
                .build();
        saved = outreachMessageRepository.save(saved);

        String displayName = (prospect.getFullName() != null && !prospect.getFullName().isBlank())
                ? prospect.getFullName()
                : prospect.getCompanyName();

        AuditLog audit = AuditLog.builder()
                .eventType(EVENT_GENERATED)
                .entityId(saved.getId())
                .summary("Generated outreach to " + displayName + " at " + prospect.getCompanyName())
                .build();
        auditLogRepository.save(audit);

        log.info("Generated outreach {} for prospect {} in {}ms (basis: {})",
                saved.getId(), prospect.getId(), latencyMs, parsed.personalizationBasis);

        List<String> usedSignals = mergeSignals(request.techStackSignals(), signals.heuristicSignals());
        return toResponse(saved, prospect, usedSignals);
    }

    @Transactional
    public OutreachResponse markAsSent(UUID outreachId) {
        OutreachMessage msg = outreachMessageRepository.findById(outreachId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "OutreachMessage not found: " + outreachId));
        if (STATUS_SENT_MOCK.equals(msg.getStatus())) {
            log.info("Outreach {} already in SENT_MOCK — no-op", outreachId);
        } else {
            msg.setStatus(STATUS_SENT_MOCK);
            msg = outreachMessageRepository.save(msg);

            AuditLog audit = AuditLog.builder()
                    .eventType(EVENT_SENT_MOCK)
                    .entityId(msg.getId())
                    .summary("Mock-sent outreach to " + msg.getProspect().getCompanyName())
                    .build();
            auditLogRepository.save(audit);
            log.info("Outreach {} marked as SENT_MOCK", outreachId);
        }
        return toResponse(msg, msg.getProspect(), List.of());
    }

    /**
     * Calls the LLM and parses its JSON envelope. On parse failure, retries
     * exactly once with a stricter system instruction. A second failure
     * surfaces as {@link OutreachGenerationException} with code
     * {@code OUTPUT_FORMAT_INVALID}.
     */
    private ParsedOutreach callAndParse(BuiltOutreachPrompt built, boolean isRetry) {
        String systemText = isRetry
                ? built.systemPrompt() + "\n\nIMPORTANT: Your previous response was not valid JSON. "
                        + "Output ONLY the JSON object — no surrounding text, no markdown fences."
                : built.systemPrompt();

        // Build through the AnthropicChatOptions builder; the chained setters return
        // the parent ToolCallingChatOptions builder, so we widen to ChatOptions for
        // assignment. Prompt accepts the ChatOptions interface.
        ChatOptions options = AnthropicChatOptions.builder()
                .model(modelName)
                .temperature(GENERATION_TEMPERATURE)
                .maxTokens(GENERATION_MAX_TOKENS)
                .build();

        Prompt prompt = new Prompt(
                List.of(new SystemMessage(systemText), new UserMessage(built.userPrompt())),
                options);

        ChatResponse response;
        try {
            response = chatModel.call(prompt);
        } catch (RuntimeException e) {
            log.error("Anthropic call failed", e);
            throw new OutreachGenerationException(
                    OutreachGenerationException.LLM_UPSTREAM_FAILURE,
                    "LLM call failed: " + e.getMessage(),
                    e);
        }

        String raw = extractText(response);
        if (raw == null || raw.isBlank()) {
            if (!isRetry) {
                log.warn("LLM returned empty content — retrying once");
                return callAndParse(built, true);
            }
            throw new OutreachGenerationException(
                    OutreachGenerationException.OUTPUT_FORMAT_INVALID,
                    "LLM returned empty content even after retry.");
        }

        try {
            return parseJson(raw);
        } catch (OutreachGenerationException parseFail) {
            if (!isRetry) {
                log.warn("Outreach JSON parse failed — retrying once. Raw output: {}",
                        truncate(raw, 400));
                return callAndParse(built, true);
            }
            log.error("Outreach JSON parse failed after retry. Raw output: {}", truncate(raw, 800));
            throw parseFail;
        }
    }

    private ParsedOutreach parseJson(String raw) {
        String cleaned = stripCodeFences(raw).trim();
        try {
            JsonNode node = jsonMapper.readTree(cleaned);
            JsonNode subjectNode = node.get("subject");
            JsonNode bodyNode = node.get("body");
            JsonNode basisNode = node.get("personalization_basis");
            if (subjectNode == null || subjectNode.isNull()
                    || bodyNode == null || bodyNode.isNull()) {
                throw new OutreachGenerationException(
                        OutreachGenerationException.OUTPUT_FORMAT_INVALID,
                        "Parsed JSON missing required fields 'subject' or 'body'.");
            }
            String subject = subjectNode.asString().trim();
            String body = bodyNode.asString();
            String basis = basisNode == null || basisNode.isNull() ? "" : basisNode.asString();
            if (subject.isEmpty() || body.isBlank()) {
                throw new OutreachGenerationException(
                        OutreachGenerationException.OUTPUT_FORMAT_INVALID,
                        "Parsed JSON has empty 'subject' or 'body'.");
            }
            return new ParsedOutreach(subject, body, basis);
        } catch (JacksonException e) {
            throw new OutreachGenerationException(
                    OutreachGenerationException.OUTPUT_FORMAT_INVALID,
                    "LLM output was not valid JSON: " + e.getOriginalMessage(),
                    e);
        }
    }

    /**
     * Claude occasionally wraps responses in ```json … ``` fences despite
     * being told not to. We strip them defensively rather than trip the
     * retry path on a recoverable formatting hiccup.
     */
    private static String stripCodeFences(String s) {
        String trimmed = s.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) trimmed = trimmed.substring(firstNewline + 1);
            int closingFence = trimmed.lastIndexOf("```");
            if (closingFence >= 0) trimmed = trimmed.substring(0, closingFence);
        }
        return trimmed;
    }

    private static String extractText(ChatResponse resp) {
        if (resp == null || resp.getResult() == null || resp.getResult().getOutput() == null) {
            return null;
        }
        return resp.getResult().getOutput().getText();
    }

    private OutreachResponse toResponse(OutreachMessage msg, Prospect prospect, List<String> usedSignals) {
        return new OutreachResponse(
                msg.getId(),
                prospect.getId(),
                prospect.getFullName(),
                prospect.getRole(),
                prospect.getCompanyName(),
                usedSignals,
                msg.getSubject(),
                msg.getBody(),
                msg.getGenerationModel(),
                msg.getGenerationPromptVersion(),
                msg.getGenerationLatencyMs() == null ? 0L : msg.getGenerationLatencyMs(),
                msg.getStatus(),
                msg.getCreatedAt()
        );
    }

    private static List<String> mergeSignals(List<String> manual, List<String> extracted) {
        Set<String> ordered = new LinkedHashSet<>();
        if (manual != null) for (String s : manual) if (s != null && !s.isBlank()) ordered.add(s.trim());
        if (extracted != null) for (String s : extracted) if (s != null && !s.isBlank()) ordered.add(s.trim());
        return new ArrayList<>(ordered);
    }

    private static String truncate(String s, int n) {
        return s.length() > n ? s.substring(0, n) + "..." : s;
    }

    private record ParsedOutreach(String subject, String body, String personalizationBasis) {}
}
