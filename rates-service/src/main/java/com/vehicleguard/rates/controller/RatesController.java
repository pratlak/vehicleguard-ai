package com.vehicleguard.rates.controller;

import com.vehicleguard.rates.model.dto.RatesResponse;
import com.vehicleguard.rates.service.RatesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RatesController {

    private final RatesService ratesService;

    @GetMapping("/rates")
    public ResponseEntity<RatesResponse> getRates(
            @RequestParam String vehicleCategory,
            @RequestParam(required = false) String state) {
        log.info("GET /rates vehicleCategory={} state={}", vehicleCategory, state);
        return ResponseEntity.ok(ratesService.getRates(vehicleCategory, state));
    }
}
