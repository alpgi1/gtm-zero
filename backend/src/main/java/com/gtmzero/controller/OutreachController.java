package com.gtmzero.controller;

import com.gtmzero.dto.outreach.GenerateOutreachRequest;
import com.gtmzero.dto.outreach.OutreachHistoryDto;
import com.gtmzero.dto.outreach.OutreachResponse;
import com.gtmzero.dto.outreach.ProspectDetailDto;
import com.gtmzero.dto.outreach.ProspectSummaryDto;
import com.gtmzero.entity.OutreachMessage;
import com.gtmzero.entity.Prospect;
import com.gtmzero.exception.OutreachGenerationException;
import com.gtmzero.repository.OutreachMessageRepository;
import com.gtmzero.repository.ProspectRepository;
import com.gtmzero.seed.OutreachSeedData;
import com.gtmzero.service.OutreachService;
import com.gtmzero.service.ProspectService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST endpoints for outreach generation + prospect browsing.
 *
 * <p>Two resources colocated for convenience: {@code /outreach/*} for
 * generation and history, {@code /prospects/*} for browsing. Both share
 * the same CORS profile as the rest of the API.
 */
@RestController
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class OutreachController {

    private final OutreachService outreachService;
    private final ProspectService prospectService;
    private final ProspectRepository prospectRepository;
    private final OutreachMessageRepository outreachMessageRepository;
    private final OutreachSeedData outreachSeedData;

    public OutreachController(OutreachService outreachService,
                              ProspectService prospectService,
                              ProspectRepository prospectRepository,
                              OutreachMessageRepository outreachMessageRepository,
                              OutreachSeedData outreachSeedData) {
        this.outreachService = outreachService;
        this.prospectService = prospectService;
        this.prospectRepository = prospectRepository;
        this.outreachMessageRepository = outreachMessageRepository;
        this.outreachSeedData = outreachSeedData;
    }

    @PostMapping("/api/v1/admin/outreach/reseed")
    public ResponseEntity<List<OutreachResponse>> reseed() {
        log.info("POST /api/v1/admin/outreach/reseed");
        return ResponseEntity.ok(outreachSeedData.reseed());
    }

    @PostMapping(value = "/api/v1/outreach/generate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OutreachResponse> generate(@Valid @RequestBody GenerateOutreachRequest request) {
        log.info("POST /api/v1/outreach/generate — company='{}', name='{}'",
                request.companyName(), request.fullName());
        if (!request.hasMinimumIdentity()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        OutreachResponse response = outreachService.generate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/api/v1/outreach/{id}/send-mock",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OutreachResponse> sendMock(@PathVariable("id") UUID id) {
        log.info("POST /api/v1/outreach/{}/send-mock", id);
        OutreachResponse response = outreachService.markAsSent(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/outreach/recent")
    public ResponseEntity<List<OutreachHistoryDto>> recent(
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        List<OutreachMessage> rows = outreachMessageRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, safeLimit));
        List<OutreachHistoryDto> response = rows.stream()
                .map(OutreachController::toHistoryDto)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/outreach/{id}")
    public ResponseEntity<OutreachResponse> getOne(@PathVariable("id") UUID id) {
        return outreachMessageRepository.findById(id)
                .map(msg -> ResponseEntity.ok(toResponse(msg)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/api/v1/prospects")
    public ResponseEntity<?> listProspects(
            @RequestParam(name = "linkedinUrl", required = false) String linkedinUrl) {
        if (linkedinUrl != null && !linkedinUrl.isBlank()) {
            return prospectService.findByLinkedinUrl(linkedinUrl)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }
        return ResponseEntity.ok(prospectService.listAll());
    }

    @GetMapping("/api/v1/prospects/{id}")
    public ResponseEntity<ProspectDetailDto> getProspect(@PathVariable("id") UUID id) {
        return prospectRepository.findById(id)
                .map(p -> {
                    int count = (int) outreachMessageRepository.countByProspectId(p.getId());
                    ProspectSummaryDto summary = prospectService.toSummary(p, count);
                    List<OutreachHistoryDto> history = outreachMessageRepository
                            .findAllByProspectIdOrderByCreatedAtDesc(p.getId())
                            .stream()
                            .map(OutreachController::toHistoryDto)
                            .toList();
                    return ResponseEntity.ok(new ProspectDetailDto(summary, history));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @ExceptionHandler(OutreachGenerationException.class)
    public ResponseEntity<Map<String, String>> handleGenerationFailure(OutreachGenerationException e) {
        HttpStatus status = OutreachGenerationException.LLM_UPSTREAM_FAILURE.equals(e.getCode())
                ? HttpStatus.BAD_GATEWAY
                : HttpStatus.UNPROCESSABLE_ENTITY;
        log.error("Outreach generation failed [{}]: {}", e.getCode(), e.getMessage());
        return ResponseEntity.status(status).body(Map.of(
                "code", e.getCode(),
                "message", e.getMessage() == null ? "Outreach generation failed" : e.getMessage()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArg(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "code", "INVALID_REQUEST",
                "message", e.getMessage() == null ? "Invalid request" : e.getMessage()
        ));
    }

    private static OutreachHistoryDto toHistoryDto(OutreachMessage msg) {
        Prospect p = msg.getProspect();
        String body = msg.getBody() == null ? "" : msg.getBody();
        String preview = body.length() > 200 ? body.substring(0, 200) : body;
        return new OutreachHistoryDto(
                msg.getId(),
                p.getId(),
                p.getFullName(),
                p.getCompanyName(),
                msg.getSubject(),
                preview,
                msg.getStatus(),
                msg.getGenerationLatencyMs() == null ? 0L : msg.getGenerationLatencyMs(),
                msg.getCreatedAt()
        );
    }

    private static OutreachResponse toResponse(OutreachMessage msg) {
        Prospect p = msg.getProspect();
        return new OutreachResponse(
                msg.getId(),
                p.getId(),
                p.getFullName(),
                p.getRole(),
                p.getCompanyName(),
                p.getCompanyDomain(),
                p.getLinkedinUrl(),
                p.getTechStackSignals() == null ? List.of() : List.of(p.getTechStackSignals()),
                msg.getSubject(),
                msg.getBody(),
                msg.getPersonalizationBasis(),
                msg.getGenerationModel(),
                msg.getGenerationPromptVersion(),
                msg.getGenerationLatencyMs() == null ? 0L : msg.getGenerationLatencyMs(),
                msg.getStatus(),
                msg.getCreatedAt()
        );
    }
}
