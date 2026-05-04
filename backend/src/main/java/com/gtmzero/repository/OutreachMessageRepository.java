package com.gtmzero.repository;

import com.gtmzero.entity.OutreachMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutreachMessageRepository extends JpaRepository<OutreachMessage, UUID> {

    List<OutreachMessage> findAllByProspectIdOrderByCreatedAtDesc(UUID prospectId);

    List<OutreachMessage> findTop10ByOrderByCreatedAtDesc();
}
