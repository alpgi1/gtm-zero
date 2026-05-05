package com.gtmzero.service;

import com.gtmzero.dto.objection.ObjectionRequest;
import com.gtmzero.dto.objection.ObjectionStreamEvent;
import com.gtmzero.dto.objection.ObjectionStreamEvent.Completed;
import com.gtmzero.dto.objection.ObjectionStreamEvent.Retrieved;
import com.gtmzero.dto.objection.ObjectionStreamEvent.Started;
import com.gtmzero.dto.objection.ObjectionStreamEvent.Token;
import com.gtmzero.entity.Document;
import com.gtmzero.entity.DocumentChunk;
import com.gtmzero.entity.ObjectionQuery;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Full-pipeline integration test against the real database.
 * EmbeddingModel + ChatModel are mocked — no calls to Voyage or Anthropic.
 *
 * <p>Inserts 4 fake chunks before each test, drops them after.
 * NOT @Transactional — the @Async persistence runs on a separate thread that
 * wouldn't see a test-scoped transaction.
 */
@SpringBootTest
@ActiveProfiles("dev")
class ObjectionServiceIntegrationTest {

    @Autowired private ObjectionService objectionService;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private ObjectionQueryRepository objectionQueryRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    @MockitoBean private EmbeddingModel embeddingModel;
    @MockitoBean private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        objectionQueryRepository.deleteAll();
        documentRepository.deleteAll();

        // Stub embeddings — fixed 1024-dim vector, deterministic per input position.
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

        // Insert a document with 4 chunks so retrieval has material to work with.
        Document doc = Document.builder()
                .title("Test Compliance Doc")
                .sourceType("TECHNICAL_DOC")
                .sourcePath("/test/compliance.md")
                .rawContent("Test raw content with sufficient length to satisfy the schema constraints.")
                .charCount(80)
                .chunkCount(4)
                .ingestedAt(Instant.now())
                .build();
        for (int i = 0; i < 4; i++) {
            float[] emb = new float[1024];
            Arrays.fill(emb, 0.05f * (i + 1));
            DocumentChunk chunk = DocumentChunk.builder()
                    .document(doc)
                    .chunkIndex(i)
                    .content("Chunk " + i + ": We encrypt all data at rest using AES-256 and TLS 1.3 in transit.")
                    .tokenCount(20)
                    .embedding(emb)
                    .metadata("{\"section\": null}")
                    .build();
            doc.getChunks().add(chunk);
        }
        documentRepository.save(doc);
    }

    @AfterEach
    void tearDown() {
        // Async persistence may still be in-flight on the task executor when the test
        // method returns. Drain it before deleting so we don't leave rows behind for
        // the next test class to trip over.
        try { Thread.sleep(300); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        objectionQueryRepository.deleteAll();
        documentRepository.deleteAll();
        auditLogRepository.findAllByEventTypeOrderByCreatedAtDesc("OBJECTION_ANSWERED")
                .forEach(auditLogRepository::delete);
    }

    @Test
    void streamingPipeline_emitsEventsInCorrectOrder() {
        // Stub Claude to emit three deterministic streaming chunks.
        ChatResponse r1 = chatResponse("We encrypt all data at rest using AES-256 [1]. ");
        ChatResponse r2 = chatResponse("Personal data lives in EU regions only [2]. ");
        ChatResponse r3 = chatResponse("Access is gated by SSO [3].");
        when(chatModel.stream(any(Prompt.class)))
                .thenReturn(Flux.just(r1, r2, r3));

        ObjectionRequest request = new ObjectionRequest(
                "How do you handle GDPR compliance?",
                UUID.randomUUID(),
                4
        );

        List<ObjectionStreamEvent> events = objectionService.handleObjection(request)
                .collectList()
                .block(Duration.ofSeconds(10));

        assertThat(events).isNotNull();
        assertThat(events).hasSizeGreaterThanOrEqualTo(5);

        // Expected order: Started, Retrieved, Token+, Completed.
        assertThat(events.get(0)).isInstanceOf(Started.class);
        assertThat(events.get(1)).isInstanceOf(Retrieved.class);

        Retrieved retrieved = (Retrieved) events.get(1);
        assertThat(retrieved.citations()).hasSize(4);
        assertThat(retrieved.citations().get(0).marker()).isEqualTo("[1]");

        long tokenCount = events.stream().filter(e -> e instanceof Token).count();
        assertThat(tokenCount).isEqualTo(3);

        ObjectionStreamEvent last = events.get(events.size() - 1);
        assertThat(last).isInstanceOf(Completed.class);

        Completed completed = (Completed) last;
        assertThat(completed.response().answer())
                .contains("AES-256")
                .contains("[1]")
                .contains("[2]")
                .contains("[3]");
        assertThat(completed.response().citations()).hasSize(4);
        assertThat(completed.response().retrievedCount()).isEqualTo(4);
        assertThat(completed.response().model()).isEqualTo("claude-sonnet-4-6");
    }

    @Test
    void persistsObjectionQueryAsynchronously() {
        when(chatModel.stream(any(Prompt.class)))
                .thenReturn(Flux.just(
                        chatResponse("AES-256 encryption everywhere [1]. "),
                        chatResponse("Logged via append-only audit trail [2].")
                ));

        UUID sessionId = UUID.randomUUID();
        ObjectionRequest request = new ObjectionRequest(
                "What's your audit logging?",
                sessionId,
                4
        );

        objectionService.handleObjection(request).blockLast(Duration.ofSeconds(10));

        // Async write — wait for the row to surface (default 2s window).
        await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    List<ObjectionQuery> rows = objectionQueryRepository
                            .findAllBySessionIdOrderByCreatedAtAsc(sessionId);
                    assertThat(rows).hasSize(1);
                });

        ObjectionQuery row = objectionQueryRepository
                .findAllBySessionIdOrderByCreatedAtAsc(sessionId)
                .getFirst();
        assertThat(row.getQuestion()).isEqualTo("What's your audit logging?");
        assertThat(row.getAnswer()).contains("[1]").contains("[2]");
        assertThat(row.getCitationCount()).isEqualTo(2);
        assertThat(row.getModel()).isEqualTo("claude-sonnet-4-6");
        assertThat(row.getRetrievedChunkIds()).hasSize(4);
        assertThat(row.getTotalLatencyMs()).isNotNull();
        assertThat(row.getFirstTokenLatencyMs()).isNotNull();

        // AuditLog companion entry.
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(
                        auditLogRepository.findAllByEventTypeOrderByCreatedAtDesc("OBJECTION_ANSWERED"))
                        .isNotEmpty());
    }

    @Test
    void defaultsTopKToFour_whenRequestOmitsIt() {
        when(chatModel.stream(any(Prompt.class)))
                .thenReturn(Flux.just(chatResponse("Short answer [1].")));

        ObjectionRequest request = new ObjectionRequest("Why?", UUID.randomUUID(), null);

        Optional<Completed> completed = objectionService.handleObjection(request)
                .collectList().block(Duration.ofSeconds(10))
                .stream()
                .filter(e -> e instanceof Completed)
                .map(e -> (Completed) e)
                .findFirst();

        assertThat(completed).isPresent();
        assertThat(completed.get().response().citations()).hasSize(4);
    }

    private static ChatResponse chatResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
