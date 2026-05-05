package com.gtmzero.controller;

import com.gtmzero.dto.dashboard.DashboardResponse;
import com.gtmzero.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/v1/dashboard")
    public ResponseEntity<DashboardResponse> get() {
        return ResponseEntity.ok(dashboardService.build());
    }
}
