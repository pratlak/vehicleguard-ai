package com.vehicleguard.risk.controller;

import com.vehicleguard.risk.model.dto.AssessRequest;
import com.vehicleguard.risk.model.dto.AssessResponse;
import com.vehicleguard.risk.service.RiskScoringService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskScoringService riskScoringService;

    @PostMapping("/assess")
    public ResponseEntity<AssessResponse> assess(@Valid @RequestBody AssessRequest request) {
        log.info("POST /api/v1/risk/assess driverAge={}", request.getDriverAge());
        AssessResponse response = riskScoringService.assess(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/quote/{quoteId}")
    public ResponseEntity<AssessResponse> getQuote(@PathVariable UUID quoteId) {
        log.info("GET /api/v1/risk/quote/{}", quoteId);
        return ResponseEntity.ok(riskScoringService.getQuote(quoteId));
    }
}
