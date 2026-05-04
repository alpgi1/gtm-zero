package com.gtmzero.service;

import com.gtmzero.exception.EmbeddingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps the EmbeddingModel bean (Voyage AI via OpenAI-compatible client).
 * Provides single and batch embedding with retry logic.
 */
@Service
@Slf4j
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * Embed a single text string. Used for query embedding in Part 4.
     */
    public float[] embedSingle(String text) {
        long start = System.currentTimeMillis();
        try {
            float[] result = callWithRetry(List.of(text)).getFirst();
            long elapsed = System.currentTimeMillis() - start;
            log.debug("embedSingle completed in {}ms", elapsed);
            return result;
        } catch (Exception e) {
            throw new EmbeddingException("Failed to embed single text", e);
        }
    }

    /**
     * Embed a batch of texts. Voyage supports multiple inputs per call.
     * Spring AI's EmbeddingModel.call() accepts a list of documents,
     * so we leverage that for efficient batching.
     */
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        long start = System.currentTimeMillis();
        log.info("Embedding batch of {} texts...", texts.size());

        // Voyage allows up to 128 inputs per call.
        // For safety, batch in groups of 96.
        int batchSize = 96;
        List<float[]> allEmbeddings = new ArrayList<>();

        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);

            List<float[]> batchResult = callWithRetry(batch);
            allEmbeddings.addAll(batchResult);

            // Rate-limit-friendly delay between batches (not needed for single batch)
            if (end < texts.size()) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new EmbeddingException("Interrupted during batch embedding", e);
                }
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("Embedded {} texts in {}ms (avg {}ms/text)",
                texts.size(), elapsed, texts.isEmpty() ? 0 : elapsed / texts.size());

        return allEmbeddings;
    }

    /**
     * Calls the embedding model with one retry on transient failure.
     * 1s backoff between attempts. On second failure, throws EmbeddingException.
     */
    private List<float[]> callWithRetry(List<String> texts) {
        Exception lastException = null;

        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                EmbeddingResponse response = embeddingModel.call(
                        new EmbeddingRequest(texts, null));

                List<float[]> results = new ArrayList<>();
                response.getResults().forEach(embedding ->
                        results.add(embedding.getOutput()));

                if (response.getMetadata() != null) {
                    log.debug("Embedding response metadata: usage={}",
                            response.getMetadata().getUsage());
                }

                return results;

            } catch (Exception e) {
                lastException = e;
                log.warn("Embedding call attempt {} failed: {}", attempt + 1, e.getMessage());
                if (attempt == 0) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new EmbeddingException("Interrupted during retry backoff", ie);
                    }
                }
            }
        }

        throw new EmbeddingException(
                "Embedding failed after 2 attempts for " + texts.size() + " texts",
                lastException);
    }
}
