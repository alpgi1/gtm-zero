package com.gtmzero.controller;

import com.gtmzero.dto.WarmupResultDto;
import com.gtmzero.service.WarmupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class WarmupController {

    private final WarmupService warmupService;

    public WarmupController(WarmupService warmupService) {
        this.warmupService = warmupService;
    }

    @PostMapping(value = "/api/v1/warmup", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WarmupResultDto> warmup() {
        WarmupResultDto result = warmupService.warmup();
        return ResponseEntity.ok(result);
    }
}
