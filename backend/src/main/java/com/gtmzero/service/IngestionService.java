package com.gtmzero.service;

import com.gtmzero.dto.IngestDocumentRequest;
import com.gtmzero.dto.IngestResult;
import com.gtmzero.entity.AuditLog;
import com.gtmzero.entity.Document;
import com.gtmzero.entity.DocumentChunk;
import com.gtmzero.repository.AuditLogRepository;
import com.gtmzero.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates document ingestion: chunking → embedding → persistence.
 * Idempotent on document title.
 */
@Service
@Slf4j
public class IngestionService {

    private final DocumentRepository documentRepository;
    private final AuditLogRepository auditLogRepository;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;

    public IngestionService(DocumentRepository documentRepository,
                            AuditLogRepository auditLogRepository,
                            ChunkingService chunkingService,
                            EmbeddingService embeddingService) {
        this.documentRepository = documentRepository;
        this.auditLogRepository = auditLogRepository;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
    }

    @Transactional
    public IngestResult ingestDocument(IngestDocumentRequest request) {
        long start = System.currentTimeMillis();

        // 1. Idempotency check
        Optional<Document> existing = documentRepository.findByTitle(request.title());
        if (existing.isPresent()) {
            Document doc = existing.get();
            long elapsed = System.currentTimeMillis() - start;
            log.info("Document '{}' already exists (id={}), skipping ingestion",
                    request.title(), doc.getId());
            return new IngestResult(doc.getId(), doc.getTitle(),
                    doc.getChunkCount(), elapsed, true);
        }

        // 2. Chunk the raw content
        log.info("Chunking document '{}'...", request.title());
        List<String> chunkTexts = chunkingService.chunk(request.rawContent());
        log.info("Document '{}' split into {} chunks", request.title(), chunkTexts.size());

        // 3. Embed all chunks
        log.info("Embedding {} chunks for '{}'...", chunkTexts.size(), request.title());
        List<float[]> embeddings = embeddingService.embedBatch(chunkTexts);

        // 4. Build Document entity
        Document document = Document.builder()
                .title(request.title())
                .sourceType(request.sourceType())
                .sourcePath(request.sourcePath())
                .rawContent(request.rawContent())
                .charCount(request.rawContent().length())
                .chunkCount(chunkTexts.size())
                .ingestedAt(Instant.now())
                .build();

        // 5. Build DocumentChunk entities
        for (int i = 0; i < chunkTexts.size(); i++) {
            String chunkContent = chunkTexts.get(i);
            DocumentChunk chunk = DocumentChunk.builder()
                    .document(document)
                    .chunkIndex(i)
                    .content(chunkContent)
                    .embedding(embeddings.get(i))
                    .tokenCount(chunkContent.length() / 4)  // rough estimate
                    .metadata("{\"section\": null}")
                    .build();
            document.getChunks().add(chunk);
        }

        // 6. Persist (cascades to chunks)
        document = documentRepository.save(document);

        // 7. Audit log
        String summary = String.format("Ingested '%s' with %d chunks",
                request.title(), chunkTexts.size());
        AuditLog auditLog = AuditLog.builder()
                .eventType("DOCUMENT_INGESTED")
                .entityId(document.getId())
                .summary(summary)
                .build();
        auditLogRepository.save(auditLog);

        long elapsed = System.currentTimeMillis() - start;
        log.info("Ingestion of '{}' completed in {}ms — {} chunks, doc_id={}",
                request.title(), elapsed, chunkTexts.size(), document.getId());

        // 8. Return result
        return new IngestResult(document.getId(), document.getTitle(),
                chunkTexts.size(), elapsed, false);
    }
}
