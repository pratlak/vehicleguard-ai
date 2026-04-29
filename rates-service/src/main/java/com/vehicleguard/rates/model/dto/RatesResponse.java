package com.vehicleguard.rates.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatesResponse {
    private BigDecimal baseAnnualPremium;
    private String vehicleCategory;
    private String state;
}
