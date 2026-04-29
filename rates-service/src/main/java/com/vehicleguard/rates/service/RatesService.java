package com.vehicleguard.rates.service;

import com.vehicleguard.rates.exception.RatesNotFoundException;
import com.vehicleguard.rates.model.dto.RatesResponse;
import com.vehicleguard.rates.model.entity.VehicleBaseRate;
import com.vehicleguard.rates.repository.VehicleBaseRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatesService {

    private final VehicleBaseRateRepository repository;

    @Transactional(readOnly = true)
    public RatesResponse getRates(String vehicleCategory, String state) {
        log.info("Fetching rates for category={}, state={}", vehicleCategory, state);
        String normalizedCategory = vehicleCategory.toLowerCase().trim();

        VehicleBaseRate rate = repository.findByVehicleCategory(normalizedCategory)
                .orElseThrow(() -> new RatesNotFoundException(
                        "No base rate found for vehicle category: " + vehicleCategory));

        return RatesResponse.builder()
                .baseAnnualPremium(rate.getBaseAnnualPremium())
                .vehicleCategory(rate.getVehicleCategory())
                .state(state)
                .build();
    }
}
