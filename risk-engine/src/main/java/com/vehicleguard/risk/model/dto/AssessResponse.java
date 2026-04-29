package com.vehicleguard.risk.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessResponse {
    private UUID quoteId;
    private BigDecimal riskScore;
    private String riskTier;
    private BigDecimal annualPremiumUsd;
    private BigDecimal monthlyPremiumUsd;
    private String coverageType;
    private List<AppliedFactor> appliedFactors;
    private String aiExplanation;
    private LocalDateTime createdAt;
}
