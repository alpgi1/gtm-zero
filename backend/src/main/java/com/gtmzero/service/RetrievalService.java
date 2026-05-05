package com.gtmzero.service;

import com.gtmzero.entity.Document;
import com.gtmzero.entity.DocumentChunk;
import com.gtmzero.repository.DocumentChunkRepository;
import com.gtmzero.util.VectorFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Top-K nearest-neighbour retrieval from pgvector.
 * Embeds the query, runs cosine search, hydrates chunks, and re-orders
 * the hydrated rows back to relevance order (JPA's IN clause returns
 * rows in arbitrary order — we must preserve pgvector's ranking).
 */
@Service
@Slf4j
public class RetrievalService {

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository documentChunkRepository;

    public RetrievalService(EmbeddingService embeddingService,
                            DocumentChunkRepository documentChunkRepository) {
        this.embeddingService = embeddingService;
        this.documentChunkRepository = documentChunkRepository;
    }

    @Transactional(readOnly = true)
    public RetrievalResult retrieveTopK(String query, int topK) {
        long start = System.nanoTime();

        float[] queryEmbedding = embeddingService.embedSingle(query);
        String pgvector = VectorFormatter.toPgVectorString(queryEmbedding);

        List<UUID> rankedIds = documentChunkRepository.findTopKByEmbedding(pgvector, topK);
        if (rankedIds.isEmpty()) {
            long ms = (System.nanoTime() - start) / 1_000_000;
            log.warn("Retrieved 0 chunks in {}ms for query '{}'", ms, preview(query));
            return new RetrievalResult(List.of(), ms);
        }

        List<DocumentChunk> hydrated = documentChunkRepository.findAllByIdIn(rankedIds);

        // JPA's IN clause returns rows in undefined order — re-rank to match pgvector output.
        Map<UUID, DocumentChunk> byId = new HashMap<>(hydrated.size() * 2);
        for (DocumentChunk c : hydrated) byId.put(c.getId(), c);

        List<DocumentChunk> ordered = rankedIds.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        // Document is fetched LAZY, but PromptBuilder runs on a different thread / outside
        // the session. Force-initialize the fields the prompt needs while the session is open.
        for (DocumentChunk c : ordered) {
            Document d = c.getDocument();
            d.getTitle();
            d.getSourceType();
        }

        long ms = (System.nanoTime() - start) / 1_000_000;
        log.info("Retrieved {} chunks in {}ms for query '{}'",
                ordered.size(), ms, preview(query));

        return new RetrievalResult(ordered, ms);
    }

    private static String preview(String s) {
        if (s == null) return "null";
        return s.length() > 60 ? s.substring(0, 60) + "..." : s;
    }

    public record RetrievalResult(List<DocumentChunk> chunks, long latencyMs) {}
}
