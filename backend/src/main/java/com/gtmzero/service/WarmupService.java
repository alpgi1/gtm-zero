package com.gtmzero.service;

import com.gtmzero.dto.WarmupResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Pre-warms the two slow upstreams — Voyage embeddings + Anthropic chat —
 * so the first user-visible question doesn't pay the cold-connection tax.
 *
 * Idempotent and cheap: results are cached for {@link #CACHE_TTL_SECONDS}s
 * to avoid hammering the upstreams when the page is reloaded or multiple
 * tabs are opened.
 */
@Service
@Slf4j
public class WarmupService {

    private static final long CACHE_TTL_SECONDS = 60;

    private final EmbeddingService embeddingService;
    private final ChatModel chatModel;
    private final String modelName;
    private final AtomicReference<WarmupResultDto> lastResult = new AtomicReference<>();

    public WarmupService(EmbeddingService embeddingService,
                         ChatModel chatModel,
                         @Value("${spring.ai.anthropic.chat.options.model:claude-sonnet-4-6}")
                         String modelName) {
        this.embeddingService = embeddingService;
        this.chatModel = chatModel;
        this.modelName = modelName;
    }

    public WarmupResultDto warmup() {
        WarmupResultDto cached = lastResult.get();
        if (cached != null
                && Duration.between(cached.warmedAt(), Instant.now()).getSeconds()
                        < CACHE_TTL_SECONDS) {
            log.debug("Warmup cache hit (warmedAt={})", cached.warmedAt());
            return new WarmupResultDto(
                    cached.embeddingWarm(),
                    cached.llmWarm(),
                    cached.embeddingLatencyMs(),
                    cached.llmLatencyMs(),
                    cached.totalMs(),
                    true,
                    cached.warmedAt());
        }

        long start = System.currentTimeMillis();

        CompletableFuture<long[]> embedFuture = CompletableFuture.supplyAsync(() -> {
            long t0 = System.currentTimeMillis();
            try {
                embeddingService.embedSingle("warmup");
                return new long[] {1L, System.currentTimeMillis() - t0};
            } catch (Exception e) {
                log.warn("Warmup: embedding failed (non-fatal)", e);
                return new long[] {0L, System.currentTimeMillis() - t0};
            }
        });

        CompletableFuture<long[]> llmFuture = CompletableFuture.supplyAsync(() -> {
            long t0 = System.currentTimeMillis();
            try {
                Prompt prompt = new Prompt(
                        List.of(new UserMessage("Reply with only the word OK.")),
                        AnthropicChatOptions.builder()
                                .model(modelName)
                                .temperature(0.0)
                                .maxTokens(5)
                                .build());
                chatModel.call(prompt);
                return new long[] {1L, System.currentTimeMillis() - t0};
            } catch (Exception e) {
                log.warn("Warmup: LLM failed (non-fatal)", e);
                return new long[] {0L, System.currentTimeMillis() - t0};
            }
        });

        long[] embed = embedFuture.join();
        long[] llm = llmFuture.join();
        long total = System.currentTimeMillis() - start;

        WarmupResultDto fresh = new WarmupResultDto(
                embed[0] == 1L,
                llm[0] == 1L,
                embed[1],
                llm[1],
                total,
                false,
                Instant.now());
        lastResult.set(fresh);
        log.info("Warmup completed in {}ms (embedding={}ms, llm={}ms)",
                total, embed[1], llm[1]);
        return fresh;
    }
}
