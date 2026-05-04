package com.gtmzero;

import com.gtmzero.entity.*;
import com.gtmzero.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class SchemaIntegrationTest {

    @Autowired DocumentRepository      documentRepo;
    @Autowired DocumentChunkRepository chunkRepo;
    @Autowired ProspectRepository      prospectRepo;
    @Autowired OutreachMessageRepository outreachRepo;
    @Autowired ObjectionQueryRepository  objectionRepo;
    @Autowired AuditLogRepository        auditLogRepo;

    @Test
    void schemaLoads() {
        // All tables exist and are empty
        assertEquals(0, documentRepo.count());
        assertEquals(0, chunkRepo.count());
        assertEquals(0, prospectRepo.count());
        assertEquals(0, outreachRepo.count());
        assertEquals(0, objectionRepo.count());
        assertEquals(0, auditLogRepo.count());

        // ── Persist a Document ───────────────────────────────────────
        Document doc = Document.builder()
                .title("GTM-Zero README")
                .sourceType("README")
                .rawContent("AI Sales Engineer for technical founders.")
                .charCount(42)
                .chunkCount(1)
                .build();
        doc = documentRepo.save(doc);
        assertNotNull(doc.getId());

        // ── Persist a DocumentChunk with a real 1024-dim embedding ───
        float[] embedding = new float[1024];
        Arrays.fill(embedding, 0.01f);

        DocumentChunk chunk = DocumentChunk.builder()
                .document(doc)
                .chunkIndex(0)
                .content("AI Sales Engineer for technical founders.")
                .tokenCount(8)
                .embedding(embedding)
                .build();
        chunk = chunkRepo.save(chunk);
        assertNotNull(chunk.getId());

        // ── Round-trip: retrieve document + verify chunk count ───────
        Optional<Document> found = documentRepo.findById(doc.getId());
        assertTrue(found.isPresent());
        assertEquals("GTM-Zero README", found.get().getTitle());

        long countForDoc = chunkRepo.countByDocumentId(doc.getId());
        assertEquals(1, countForDoc);

        // ── Persist a Prospect ───────────────────────────────────────
        Prospect prospect = Prospect.builder()
                .companyName("Acme Corp")
                .fullName("Jane Doe")
                .role("VP of Engineering")
                .techStackSignals(new String[]{"Java", "Kubernetes"})
                .build();
        prospect = prospectRepo.save(prospect);
        assertNotNull(prospect.getId());

        // ── Persist an OutreachMessage ───────────────────────────────
        OutreachMessage msg = OutreachMessage.builder()
                .prospect(prospect)
                .subject("GTM-Zero for Acme")
                .body("Hi Jane, saw your Java + K8s stack...")
                .generationModel("claude-sonnet-4-6")
                .build();
        outreachRepo.save(msg);
        assertEquals(1, outreachRepo.findAllByProspectIdOrderByCreatedAtDesc(prospect.getId()).size());

        // ── Persist an ObjectionQuery ────────────────────────────────
        ObjectionQuery objection = ObjectionQuery.builder()
                .sessionId(UUID.randomUUID())
                .question("How does it handle latency?")
                .answer("Sub-2s p99 via streaming.")
                .retrievedChunkIds(new UUID[]{chunk.getId()})
                .citationCount(1)
                .model("claude-sonnet-4-6")
                .build();
        objectionRepo.save(objection);
        assertEquals(1, objectionRepo.count());

        // ── Persist an AuditLog entry ────────────────────────────────
        AuditLog log = AuditLog.builder()
                .eventType("DOCUMENT_INGESTED")
                .entityId(doc.getId())
                .summary("Ingested GTM-Zero README (1 chunk)")
                .build();
        auditLogRepo.save(log);
        assertEquals(1, auditLogRepo.findTop20ByOrderByCreatedAtDesc().size());
    }
}
