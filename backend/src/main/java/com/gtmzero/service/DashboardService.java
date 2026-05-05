package com.gtmzero.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtmzero.dto.dashboard.DashboardActivityItemDto;
import com.gtmzero.dto.dashboard.DashboardCitationDto;
import com.gtmzero.dto.dashboard.DashboardMetricsDto;
import com.gtmzero.dto.dashboard.DashboardResponse;
import com.gtmzero.entity.AuditLog;
import com.gtmzero.repository.AuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregates the metric headline + recent activity feed for the dashboard.
 *
 * <p>Metrics are static demo constants — the MVP does not yet have a
 * pipeline-tracking system. Activity feed reads the last 10 audit log
 * rows and hydrates citation chips when metadata is present.
 */
@Service
@Slf4j
public class DashboardService {

    private static final DashboardMetricsDto DEMO_METRICS =
            new DashboardMetricsDto(127, 14, 27, 1_200_000L);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DashboardService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public DashboardResponse build() {
        List<AuditLog> recent = auditLogRepository.findTop20ByOrderByCreatedAtDesc();
        List<DashboardActivityItemDto> feed = recent.stream()
                .limit(10)
                .map(this::toItemDto)
                .toList();
        return new DashboardResponse(DEMO_METRICS, feed);
    }

    private DashboardActivityItemDto toItemDto(AuditLog row) {
        return new DashboardActivityItemDto(
                row.getId(),
                row.getEventType(),
                row.getSummary(),
                row.getCreatedAt(),
                parseCitations(row.getMetadata())
        );
    }

    private List<DashboardCitationDto> parseCitations(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) return List.of();
        try {
            JsonNode root = objectMapper.readTree(metadataJson);
            JsonNode arr = root.get("citations");
            if (arr == null || !arr.isArray()) return List.of();
            List<DashboardCitationDto> out = new ArrayList<>(arr.size());
            for (JsonNode node : arr) {
                String marker = node.path("marker").asText(null);
                String title = node.path("documentTitle").asText(null);
                int idx = node.path("chunkIndex").asInt(0);
                if (marker == null || title == null) continue;
                out.add(new DashboardCitationDto(marker, title, idx));
                if (out.size() >= 2) break;
            }
            return out;
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse citation metadata: {}", e.getMessage());
            return List.of();
        }
    }
}
