package com.gtmzero.service;

import com.gtmzero.dto.objection.CitationDto;
import com.gtmzero.entity.DocumentChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the system + user prompt pair for the objection-handling LLM call.
 *
 * <p>Prompts live as Java text blocks rather than separate resource files: keeping
 * them next to the code makes them grep-able with the rest of the service, and the
 * MVP has only one prompt — the abstraction cost of externalising it isn't yet earned.
 *
 * <p>Returned {@link CitationDto}s are pre-built with markers [1]…[N] mapped to chunk
 * metadata so the streaming layer can flush citations to the UI before the LLM emits
 * its first token (≈200ms head-start).
 */
@Component
public class PromptBuilder {

    private static final int SNIPPET_LENGTH = 200;

    /** Persona + answering rules. Constant — never templated with user input. */
    public static final String SYSTEM_PROMPT = """
            You are an AI Sales Engineer representing Regu, an EU AI Act compliance \
            platform. You answer technical objections from enterprise prospects (CTOs, \
            VPs of Engineering) during sales calls.

            YOUR ABSOLUTE RULES:
            1. Answer ONLY using the SOURCES provided below. If the sources don't \
            contain the answer, say "I don't have that information in our technical \
            documentation — let me follow up with our team after this call." Do not invent.
            2. CITATION RULE — STRICT:
               - Every factual sentence MUST end with a citation marker [1], [2], [3], \
            or [4] before the period.
               - If you use markdown headers or bullet points, the LAST line of that \
            section's content must carry a citation, AND any factual claim in the \
            header itself must include a marker \
            (e.g. "**Layer 1 — Input Sanitization [2]:**").
               - Compound sentences with multiple claims may carry multiple markers \
            (e.g. "We use AES-256 [3] and TLS 1.3 [4].").
               - If a sentence is purely structural (transition, summary header without \
            facts), you may omit the citation, but keep such sentences to a minimum.
            3. Be direct and technical. CTOs respect concision. No fluff, no "great \
            question", no hedging.
            4. Length: 80-200 words. Three short paragraphs maximum.
            5. Tone: confident, factual, peer-to-peer. Like a senior engineer talking \
            to another senior engineer.
            6. Never break character. You are NOT an AI assistant. You are Regu's \
            technical sales engineer.

            EXAMPLE OF CORRECT FORMAT:
            "Our system uses three layers of defense [1]. \
            **Layer 1 — Input Sanitization [1]:** All user input passes through a \
            regex-based filter that strips known injection patterns [2]. \
            **Layer 2 — Prompt Isolation [3]:** System and user messages are never \
            concatenated [3]. The combined effect reduces successful injection rates to \
            under 0.1% in our testing [4]."
            """;

    public BuiltPrompt build(String question, List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            // Defensive: surface a prompt that asks the model to honour the
            // "no information" rule rather than fabricating an answer.
            String userPrompt = """
                    SOURCES:
                    (no relevant sources retrieved)

                    PROSPECT QUESTION:
                    %s

                    Answer the prospect's question using ONLY the SOURCES above.
                    """.formatted(question);
            return new BuiltPrompt(SYSTEM_PROMPT, userPrompt, List.of());
        }

        StringBuilder sb = new StringBuilder(4096);
        sb.append("SOURCES:\n\n");

        List<CitationDto> citations = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            int markerNum = i + 1;
            String marker = "[" + markerNum + "]";
            DocumentChunk chunk = chunks.get(i);
            String docTitle = chunk.getDocument().getTitle();
            String sourceType = chunk.getDocument().getSourceType();

            sb.append(marker).append(" ").append(docTitle)
                    .append(" (").append(sourceType)
                    .append(", chunk ").append(chunk.getChunkIndex())
                    .append("):\n")
                    .append(chunk.getContent())
                    .append("\n\n");

            String snippet = chunk.getContent();
            if (snippet.length() > SNIPPET_LENGTH) {
                snippet = snippet.substring(0, SNIPPET_LENGTH) + "...";
            }

            citations.add(new CitationDto(
                    marker,
                    chunk.getId(),
                    docTitle,
                    sourceType,
                    chunk.getChunkIndex(),
                    snippet
            ));
        }

        sb.append("PROSPECT QUESTION:\n").append(question).append("\n\n");
        sb.append("Answer the prospect's question using ONLY the SOURCES above. ");
        sb.append("Cite every claim with ");
        for (int i = 0; i < chunks.size(); i++) {
            if (i > 0) sb.append(i == chunks.size() - 1 ? ", or " : ", ");
            sb.append("[").append(i + 1).append("]");
        }
        sb.append(".\n");

        return new BuiltPrompt(SYSTEM_PROMPT, sb.toString(), citations);
    }

    public record BuiltPrompt(String systemPrompt, String userPrompt, List<CitationDto> citations) {}
}
