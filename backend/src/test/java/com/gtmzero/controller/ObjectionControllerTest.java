package com.gtmzero.controller;

import com.gtmzero.dto.objection.ObjectionRequest;
import com.gtmzero.entity.Document;
import com.gtmzero.entity.DocumentChunk;
import com.gtmzero.repository.AuditLogRepository;
import com.gtmzero.repository.DocumentRepository;
import com.gtmzero.repository.ObjectionQueryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ObjectionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private ObjectionQueryRepository objectionQueryRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    @MockitoBean private EmbeddingModel embeddingModel;
    @MockitoBean private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        objectionQueryRepository.deleteAll();
        documentRepository.deleteAll();

        when(embeddingModel.call(any(EmbeddingRequest.class)))
                .thenAnswer(inv -> {
                    EmbeddingRequest req = inv.getArgument(0);
                    List<Embedding> outs = new ArrayList<>();
                    for (int i = 0; i < req.getInstructions().size(); i++) {
                        float[] v = new float[1024];
                        Arrays.fill(v, 0.05f * (i + 1));
                        outs.add(new Embedding(v, i));
                    }
                    return new EmbeddingResponse(outs);
                });

        Document doc = Document.builder()
                .title("Controller Compliance Doc")
                .sourceType("TECHNICAL_DOC")
                .sourcePath("/test/c.md")
                .rawContent("Controller test raw content with sufficient length for the constraints.")
                .charCount(80)
                .chunkCount(4)
                .ingestedAt(Instant.now())
                .build();
        for (int i = 0; i < 4; i++) {
            float[] emb = new float[1024];
            Arrays.fill(emb, 0.05f * (i + 1));
            doc.getChunks().add(DocumentChunk.builder()
                    .document(doc)
                    .chunkIndex(i)
                    .content("Source " + i + ": data is encrypted at rest with AES-256.")
                    .tokenCount(15)
                    .embedding(emb)
                    .metadata("{}")
                    .build());
        }
        documentRepository.save(doc);
    }

    @AfterEach
    void tearDown() {
        // Async persistence may still be in-flight when the test method returns.
        try { Thread.sleep(300); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        objectionQueryRepository.deleteAll();
        documentRepository.deleteAll();
        auditLogRepository.findAllByEventTypeOrderByCreatedAtDesc("OBJECTION_ANSWERED")
                .forEach(auditLogRepository::delete);
    }

    @Test
    void postObjection_returnsCitedAnswer() throws Exception {
        when(chatModel.stream(any(Prompt.class)))
                .thenReturn(Flux.just(
                        chatResponse("We encrypt at rest with AES-256 [1]. "),
                        chatResponse("All access is SSO-gated [2]."))
                );

        ObjectionRequest request = new ObjectionRequest(
                "How do you protect customer data?",
                UUID.randomUUID(),
                4
        );

        mockMvc.perform(post("/api/v1/objections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queryId").isNotEmpty())
                .andExpect(jsonPath("$.question").value("How do you protect customer data?"))
                .andExpect(jsonPath("$.answer").isNotEmpty())
                .andExpect(jsonPath("$.citations").isArray())
                .andExpect(jsonPath("$.citations.length()").value(4))
                .andExpect(jsonPath("$.citations[0].marker").value("[1]"))
                .andExpect(jsonPath("$.retrievedCount").value(4))
                .andExpect(jsonPath("$.model").value("claude-sonnet-4-6"))
                .andExpect(jsonPath("$.totalLatencyMs").isNumber());
    }

    @Test
    void postObjection_returns400_onShortQuestion() throws Exception {
        ObjectionRequest request = new ObjectionRequest("hi", UUID.randomUUID(), 4);

        mockMvc.perform(post("/api/v1/objections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRecent_returnsArray() throws Exception {
        mockMvc.perform(get("/api/v1/objections/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    private static ChatResponse chatResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
