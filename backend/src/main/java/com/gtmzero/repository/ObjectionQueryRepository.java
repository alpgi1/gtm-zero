package com.gtmzero.repository;

import com.gtmzero.entity.ObjectionQuery;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ObjectionQueryRepository extends JpaRepository<ObjectionQuery, UUID> {

    List<ObjectionQuery> findAllBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    long countBySessionId(UUID sessionId);

    List<ObjectionQuery> findTop20ByOrderByCreatedAtDesc();

    List<ObjectionQuery> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
