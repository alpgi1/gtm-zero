package com.gtmzero.controller;

import com.gtmzero.dto.DocumentDetailDto;
import com.gtmzero.dto.DocumentListResponse;
import com.gtmzero.dto.IngestDocumentRequest;
import com.gtmzero.dto.IngestResult;
import com.gtmzero.entity.Document;
import com.gtmzero.entity.DocumentChunk;
import com.gtmzero.repository.DocumentChunkRepository;
import com.gtmzero.repository.DocumentRepository;
import com.gtmzero.seed.ReguSeedData;
import com.gtmzero.service.IngestionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Admin endpoints for document ingestion.
 * No auth for MVP — production would gate these behind admin role.
 */
@RestController
@RequestMapping("/api/v1/admin/documents")
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class IngestionController {

    private final IngestionService ingestionService;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ReguSeedData reguSeedData;

    public IngestionController(IngestionService ingestionService,
                               DocumentRepository documentRepository,
                               DocumentChunkRepository documentChunkRepository,
                               ReguSeedData reguSeedData) {
        this.ingestionService = ingestionService;
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.reguSeedData = reguSeedData;
    }

    /**
     * Ingest a single document into the RAG corpus.
     */
    @PostMapping
    public ResponseEntity<IngestResult> ingestDocument(
            @Valid @RequestBody IngestDocumentRequest request) {
        log.info("POST /api/v1/admin/documents — title='{}'", request.title());
        IngestResult result = ingestionService.ingestDocument(request);
        HttpStatus status = result.wasSkipped() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result);
    }

    /**
     * Seed the Regu demo documents.
     * Idempotent by default — skips already-ingested docs.
     * Pass {@code ?force=true} to delete and re-ingest existing documents by title.
     */
    @PostMapping("/seed")
    public ResponseEntity<List<IngestResult>> seedDocuments(
            @RequestParam(defaultValue = "false") boolean force) {
        log.info("POST /api/v1/admin/documents/seed — force={}", force);
        List<IngestResult> results = new ArrayList<>();
        for (IngestDocumentRequest request : reguSeedData.getSeedDocuments()) {
            IngestResult result = ingestionService.ingestDocument(request, force);
            results.add(result);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(results);
    }

    /**
     * List all ingested documents (metadata only, no raw content).
     */
    @GetMapping
    public ResponseEntity<List<DocumentListResponse>> listDocuments() {
        List<Document> documents = documentRepository.findAllByOrderByCreatedAtDesc();
        List<DocumentListResponse> response = documents.stream()
                .map(doc -> new DocumentListResponse(
                        doc.getId(),
                        doc.getTitle(),
                        doc.getSourceType(),
                        doc.getCharCount(),
                        doc.getChunkCount(),
                        doc.getIngestedAt(),
                        doc.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Detail view: a single document plus truncated chunk previews.
     * Snippet length is fixed at 240 chars to keep payload small.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentDetailDto> getDocument(@PathVariable UUID id) {
        return documentRepository.findById(id)
                .map(doc -> {
                    List<DocumentChunk> chunks = documentChunkRepository
                            .findAllByDocumentIdOrderByChunkIndexAsc(doc.getId());
                    List<DocumentDetailDto.ChunkPreview> previews = chunks.stream()
                            .map(c -> new DocumentDetailDto.ChunkPreview(
                                    c.getId(),
                                    c.getChunkIndex(),
                                    c.getTokenCount(),
                                    snippet(c.getContent())))
                            .toList();
                    return ResponseEntity.ok(new DocumentDetailDto(
                            doc.getId(),
                            doc.getTitle(),
                            doc.getSourceType(),
                            doc.getSourcePath(),
                            doc.getCharCount(),
                            doc.getChunkCount(),
                            doc.getIngestedAt(),
                            previews));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static String snippet(String content) {
        if (content == null) return "";
        String trimmed = content.strip();
        if (trimmed.length() <= 240) return trimmed;
        return trimmed.substring(0, 240) + "…";
    }

    /**
     * Delete a document and all its chunks (cascading delete).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
        log.info("DELETE /api/v1/admin/documents/{}", id);
        if (!documentRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        documentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
