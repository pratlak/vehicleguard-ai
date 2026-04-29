package com.vehicleguard.risk.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppliedFactor {
    private String key;
    private String label;
    private BigDecimal scoreImpact;
    private BigDecimal multiplier;
}
