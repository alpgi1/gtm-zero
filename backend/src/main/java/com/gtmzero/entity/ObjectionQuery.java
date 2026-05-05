package com.gtmzero.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * Implements {@link Persistable} because the ObjectionService pre-assigns the ID
 * (so the in-memory response and the persisted row share an identifier).
 * Without this, {@link EntityManager#persist} would treat any non-null-id entity
 * as "detached" and refuse to insert.
 */
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"question", "answer"})
@Entity
@Table(name = "objection_queries")
public class ObjectionQuery implements Persistable<UUID> {

    // No @GeneratedValue — IDs are pre-assigned by the orchestrator so the in-memory
    // ObjectionResponse and the persisted row share the same identifier. Hibernate
    // would otherwise treat any non-null-id new entity as "detached" on persist.
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "answer", nullable = false, columnDefinition = "TEXT")
    private String answer;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "retrieved_chunk_ids", nullable = false, columnDefinition = "uuid[]")
    private UUID[] retrievedChunkIds;

    @Column(name = "citation_count", nullable = false)
    private int citationCount;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "first_token_latency_ms")
    private Integer firstTokenLatencyMs;

    @Column(name = "total_latency_ms")
    private Integer totalLatencyMs;

    @Column(name = "token_count_input")
    private Integer tokenCountInput;

    @Column(name = "token_count_output")
    private Integer tokenCountOutput;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * True until Hibernate loads the entity. Set to false post-load via
     * {@link PostLoad} so subsequent re-persist of a fetched entity would
     * correctly route through merge.
     */
    @Builder.Default
    @Transient
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad @PostPersist
    private void markNotNew() {
        this.isNew = false;
    }
}
