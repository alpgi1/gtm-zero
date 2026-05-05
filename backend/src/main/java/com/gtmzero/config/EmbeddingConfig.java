package com.gtmzero.config;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires Voyage AI as the project's EmbeddingModel using Spring AI's
 * OpenAI-compatible client. Voyage exposes an OpenAI-compatible
 * REST API at https://api.voyageai.com/v1, so we reuse OpenAiApi +
 * OpenAiEmbeddingModel rather than hand-rolling an HTTP client.
 */
@Configuration
public class EmbeddingConfig {

    @Bean
    public EmbeddingModel voyageEmbeddingModel(
            @Value("${VOYAGE_API_KEY}") String apiKey) {

        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl("https://api.voyageai.com")
                .build();

        // voyage-3-large outputs 1024 dims by default; Voyage's API does NOT accept
        // the 'dimensions' parameter (unlike OpenAI's text-embedding-3-* models).
        return new OpenAiEmbeddingModel(
                openAiApi,
                MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder()
                        .model("voyage-3-large")
                        .build()
        );
    }
}
