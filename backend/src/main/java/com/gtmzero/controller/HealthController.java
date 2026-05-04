package com.gtmzero.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "gtm-zero");
        body.put("timestamp", Instant.now().toString());
        body.put("db", dbStatus());
        return ResponseEntity.ok(body);
    }

    private String dbStatus() {
        try {
            entityManager.createNativeQuery("SELECT 1").getSingleResult();
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}
