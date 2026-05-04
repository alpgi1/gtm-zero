package com.gtmzero.repository;

import com.gtmzero.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findTop20ByOrderByCreatedAtDesc();

    List<AuditLog> findAllByEventTypeOrderByCreatedAtDesc(String eventType);
}
