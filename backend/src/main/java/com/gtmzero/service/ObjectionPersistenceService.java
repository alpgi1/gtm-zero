package com.gtmzero.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtmzero.dto.objection.CitationDto;
import com.gtmzero.dto.objection.ObjectionRequest;
import com.gtmzero.dto.objection.ObjectionResponse;
import com.gtmzero.entity.AuditLog;
import com.gtmzero.entity.ObjectionQuery;
import com.gtmzero.repository.AuditLogRepository;
import com.gtmzero.repository.ObjectionQueryRepository;
import com.gtmzero.service.CitationValidator.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Off-thread writer for ObjectionQuery + AuditLog. Failures here must
 * NEVER affect the user-visible response — wrap everything and log loudly.
 *
 * <p>{@link ObjectionQuery} implements {@code Persistable} so {@code save()}
 * routes to {@code persist()} (insert) rather than {@code merge()} — necessary
 * because the orchestrator pre-assigns the queryId so the in-memory response
 * and the row share an identifier.
 */
@Service
@Slf4j
public class ObjectionPersistenceService {

    private static final UUID DEFAULT_SESSION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final ObjectionQueryRepository objectionQueryRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ObjectionPersistenceService(ObjectionQueryRepository objectionQueryRepository,
                                       AuditLogRepository auditLogRepository) {
        this.objectionQueryRepository = objectionQueryRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Async
    @Transactional
    public void persist(UUID queryId,
                        ObjectionRequest request,
                        ObjectionResponse response,
                        ValidationResult validation,
                        List<UUID> retrievedChunkIds) {
        try {
            UUID sessionId = request.sessionId() != null ? request.sessionId() : DEFAULT_SESSION_ID;

            ObjectionQuery row = ObjectionQuery.builder()
                    .id(queryId)
                    .sessionId(sessionId)
                    .question(response.question())
                    .answer(response.answer())
                    .retrievedChunkIds(retrievedChunkIds.toArray(UUID[]::new))
                    .citationCount(validation.markersUsed().size())
                    .model(response.model())
                    .firstTokenLatencyMs((int) response.firstTokenLatencyMs())
                    .totalLatencyMs((int) response.totalLatencyMs())
                    .build();
            objectionQueryRepository.save(row);

            String preview = response.question().length() > 80
                    ? response.question().substring(0, 80) + "..."
                    : response.question();

            AuditLog audit = AuditLog.builder()
                    .eventType("OBJECTION_ANSWERED")
                    .entityId(queryId)
                    .summary("Answered: " + preview)
                    .metadata(buildCitationMetadata(response.citations()))
                    .build();
            auditLogRepository.save(audit);

            log.info("Persisted ObjectionQuery {} (sentences={}, cited={}, coverage={}, invalidMarkers={})",
                    queryId,
                    validation.sentenceCount(),
                    validation.citedSentenceCount(),
                    String.format("%.2f", validation.citationCoverage()),
                    validation.invalidMarkers());

        } catch (Exception e) {
            log.error("Failed to persist ObjectionQuery {} — user response was unaffected", queryId, e);
        }
    }

    /**
     * Serializes up to 2 citation entries into the AuditLog metadata column.
     * Schema: {"citations": [{"marker": "[1]", "documentTitle": "...", "chunkIndex": 0}, ...]}.
     * Returns null on serialization failure (audit row is still useful without it).
     */
    private String buildCitationMetadata(List<CitationDto> citations) {
        if (citations == null || citations.isEmpty()) return null;
        try {
            List<Map<String, Object>> compact = citations.stream()
                    .limit(2)
                    .map(c -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("marker", c.marker());
                        m.put("documentTitle", c.documentTitle());
                        m.put("chunkIndex", c.chunkIndex());
                        return m;
                    })
                    .toList();
            return objectMapper.writeValueAsString(Map.of("citations", compact));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize citation metadata for AuditLog: {}", e.getMessage());
            return null;
        }
    }
}
