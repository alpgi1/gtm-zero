package com.gtmzero.repository;

import com.gtmzero.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    long countByDocumentId(UUID documentId);

    List<DocumentChunk> findAllByIdIn(List<UUID> ids);

    List<DocumentChunk> findAllByDocumentIdOrderByChunkIndexAsc(UUID documentId);

    // Accepts the query embedding as a pre-formatted pgvector string "[x1,x2,...]".
    // String parameter avoids float[]-to-vector marshaling at the JDBC layer;
    // the explicit CAST lets PostgreSQL's vector_in() function handle parsing.
    @Query(value = """
        SELECT id FROM document_chunks
        ORDER BY embedding <=> CAST(:queryEmbedding AS vector)
        LIMIT :topK
        """, nativeQuery = true)
    List<UUID> findTopKByEmbedding(@Param("queryEmbedding") String queryEmbedding,
                                   @Param("topK") int topK);
}
