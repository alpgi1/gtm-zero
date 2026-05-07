package com.gtmzero.controller;

import com.gtmzero.dto.objection.ObjectionRequest;
import com.gtmzero.dto.objection.ObjectionResponse;
import com.gtmzero.dto.objection.ObjectionStreamEvent;
import com.gtmzero.dto.objection.ObjectionStreamEvent.Completed;
import com.gtmzero.dto.objection.ObjectionStreamEvent.Failed;
import com.gtmzero.dto.objection.RecentObjectionResponse;
import com.gtmzero.entity.ObjectionQuery;
import com.gtmzero.repository.ObjectionQueryRepository;
import com.gtmzero.service.ObjectionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Public objection-handling endpoints.
 *
 * <ul>
 *   <li>{@code POST /stream} — SSE; emits {@code started}, {@code retrieved},
 *       {@code token}, {@code completed}, {@code failed} events.</li>
 *   <li>{@code POST /} — non-streaming; collects the same flux and returns the
 *       final {@link ObjectionResponse}. Used by tests and as a demo fallback.</li>
 *   <li>{@code GET /recent} — last N persisted ObjectionQuery rows for the
 *       dashboard activity feed.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/objections")
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class ObjectionController {

    private static final long SSE_TIMEOUT_MS = 60_000L;

    private final ObjectionService objectionService;
    private final ObjectionQueryRepository objectionQueryRepository;

    public ObjectionController(ObjectionService objectionService,
                               ObjectionQueryRepository objectionQueryRepository) {
        this.objectionService = objectionService;
        this.objectionQueryRepository = objectionQueryRepository;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamObjection(@Valid @RequestBody ObjectionRequest request) {
        log.info("POST /api/v1/objections/stream — question='{}'", preview(request.question()));
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        objectionService.handleObjection(request)
                .subscribe(
                        event -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name(event.eventName())
                                        .data(event, MediaType.APPLICATION_JSON));
                            } catch (IOException e) {
                                // Client disconnected — abort the subscription.
                                emitter.completeWithError(e);
                            }
                        },
                        err -> {
                            log.error("Unhandled SSE error", err);
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("failed")
                                        .data(new Failed(err.getMessage(), "STREAM_ERROR"),
                                                MediaType.APPLICATION_JSON));
                            } catch (IOException ignore) { /* client gone */ }
                            emitter.completeWithError(err);
                        },
                        emitter::complete
                );

        return emitter;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ObjectionResponse> handleObjection(@Valid @RequestBody ObjectionRequest request) {
        log.info("POST /api/v1/objections — question='{}'", preview(request.question()));

        List<ObjectionStreamEvent> events = objectionService.handleObjection(request)
                .collectList()
                .block();

        if (events == null || events.isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Walk the stream from the back — Completed (or Failed) is the terminal event.
        for (int i = events.size() - 1; i >= 0; i--) {
            ObjectionStreamEvent ev = events.get(i);
            if (ev instanceof Completed c) {
                return ResponseEntity.ok(c.response());
            }
            if (ev instanceof Failed f) {
                log.error("Objection pipeline returned Failed: {} ({})", f.message(), f.code());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    @GetMapping("/recent")
    public ResponseEntity<List<RecentObjectionResponse>> recent(
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        List<ObjectionQuery> rows = objectionQueryRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(0, safeLimit));
        List<RecentObjectionResponse> response = rows.stream()
                .map(q -> new RecentObjectionResponse(
                        q.getId(),
                        q.getSessionId(),
                        q.getQuestion(),
                        q.getAnswer(),
                        q.getCitationCount(),
                        q.getFirstTokenLatencyMs(),
                        q.getTotalLatencyMs(),
                        q.getModel(),
                        q.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<RecentObjectionResponse>> sessionHistory(
            @PathVariable UUID sessionId) {
        List<ObjectionQuery> rows = objectionQueryRepository
                .findAllBySessionIdOrderByCreatedAtAsc(sessionId);
        List<RecentObjectionResponse> response = rows.stream()
                .map(q -> new RecentObjectionResponse(
                        q.getId(),
                        q.getSessionId(),
                        q.getQuestion(),
                        q.getAnswer(),
                        q.getCitationCount(),
                        q.getFirstTokenLatencyMs(),
                        q.getTotalLatencyMs(),
                        q.getModel(),
                        q.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(response);
    }

    private static String preview(String s) {
        if (s == null) return "null";
        return s.length() > 80 ? s.substring(0, 80) + "..." : s;
    }
}
