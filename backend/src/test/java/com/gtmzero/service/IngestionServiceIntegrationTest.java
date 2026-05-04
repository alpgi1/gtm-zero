package com.gtmzero.service;

import com.gtmzero.dto.IngestDocumentRequest;
import com.gtmzero.dto.IngestResult;
import com.gtmzero.entity.AuditLog;
import com.gtmzero.entity.Document;
import com.gtmzero.repository.AuditLogRepository;
import com.gtmzero.repository.DocumentChunkRepository;
import com.gtmzero.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration test for IngestionService using a mocked EmbeddingModel
 * so we don't hit the real Voyage API. Runs against the real database.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class IngestionServiceIntegrationTest {

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentChunkRepository chunkRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private EmbeddingModel embeddingModel;

    private static final String TEST_CONTENT = """
            # Test Document

            This is the first paragraph of our test document. It contains enough text to be
            meaningful and to test the chunking service properly. We need at least 100 characters.

            This is the second paragraph. It discusses the EU AI Act and its implications for
            AI system providers operating in the European Union marketplace.

            This is the third paragraph about compliance requirements including risk management,
            data governance, and technical documentation per Annex IV of the regulation.
            """;

    private void stubEmbeddingModel() {
        // Return deterministic 1024-dim embeddings
        when(embeddingModel.call(any(org.springframework.ai.embedding.EmbeddingRequest.class)))
                .thenAnswer(invocation -> {
                    org.springframework.ai.embedding.EmbeddingRequest request = invocation.getArgument(0);
                    List<String> inputs = request.getInstructions();
                    List<org.springframework.ai.embedding.Embedding> embeddings = new java.util.ArrayList<>();
                    for (int i = 0; i < inputs.size(); i++) {
                        float[] vec = new float[1024];
                        java.util.Arrays.fill(vec, 0.01f * (i + 1));
                        embeddings.add(new org.springframework.ai.embedding.Embedding(vec, i));
                    }
                    return new org.springframework.ai.embedding.EmbeddingResponse(embeddings);
                });
    }

    @Test
    void ingestDocument_createsDocumentAndChunks() {
        stubEmbeddingModel();

        IngestDocumentRequest request = new IngestDocumentRequest(
                "Test Integration Doc",
                "TECHNICAL_DOC",
                "/test/path.md",
                TEST_CONTENT
        );

        IngestResult result = ingestionService.ingestDocument(request);

        assertNotNull(result.documentId());
        assertEquals("Test Integration Doc", result.title());
        assertTrue(result.chunkCount() > 0, "Should have at least 1 chunk");
        assertFalse(result.wasSkipped());
        assertTrue(result.totalLatencyMs() >= 0);

        // Verify document in DB
        Optional<Document> doc = documentRepository.findById(result.documentId());
        assertTrue(doc.isPresent());
        assertEquals(TEST_CONTENT.length(), doc.get().getCharCount());
        assertEquals(result.chunkCount(), doc.get().getChunkCount());

        // Verify chunks in DB
        long chunkCount = chunkRepository.countByDocumentId(result.documentId());
        assertEquals(result.chunkCount(), chunkCount);
    }

    @Test
    void ingestDocument_idempotent_skipsOnSecondCall() {
        stubEmbeddingModel();

        IngestDocumentRequest request = new IngestDocumentRequest(
                "Idempotency Test Doc",
                "README",
                null,
                TEST_CONTENT
        );

        // First ingestion
        IngestResult first = ingestionService.ingestDocument(request);
        assertFalse(first.wasSkipped());
        int firstChunkCount = first.chunkCount();

        // Second ingestion — should skip
        IngestResult second = ingestionService.ingestDocument(request);
        assertTrue(second.wasSkipped());
        assertEquals(first.documentId(), second.documentId());

        // Verify no duplicate documents
        long docCount = documentRepository.findAll().stream()
                .filter(d -> d.getTitle().equals("Idempotency Test Doc"))
                .count();
        assertEquals(1, docCount);

        // Verify chunk count didn't change
        long chunkCount = chunkRepository.countByDocumentId(first.documentId());
        assertEquals(firstChunkCount, chunkCount);
    }

    @Test
    void ingestDocument_createsAuditLogEntry() {
        stubEmbeddingModel();

        IngestDocumentRequest request = new IngestDocumentRequest(
                "Audit Log Test Doc",
                "ARCHITECTURE",
                null,
                TEST_CONTENT
        );

        IngestResult result = ingestionService.ingestDocument(request);

        List<AuditLog> logs = auditLogRepository.findAllByEventTypeOrderByCreatedAtDesc(
                "DOCUMENT_INGESTED");
        assertFalse(logs.isEmpty(), "Should have audit log entry");

        AuditLog latestLog = logs.getFirst();
        assertEquals("DOCUMENT_INGESTED", latestLog.getEventType());
        assertEquals(result.documentId(), latestLog.getEntityId());
        assertTrue(latestLog.getSummary().contains("Audit Log Test Doc"));
        assertTrue(latestLog.getSummary().contains("chunks"));
    }
}
