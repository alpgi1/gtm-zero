package com.gtmzero.repository;

import com.gtmzero.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Optional<Document> findByTitle(String title);

    List<Document> findAllByOrderByCreatedAtDesc();
}
