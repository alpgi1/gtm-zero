package com.gtmzero.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"question", "answer"})
@Entity
@Table(name = "objection_queries")
public class ObjectionQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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
}
