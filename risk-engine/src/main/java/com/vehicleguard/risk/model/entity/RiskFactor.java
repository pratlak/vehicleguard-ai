package com.vehicleguard.risk.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(schema = "risk_rules", name = "risk_factors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "serial")
    private Integer id;

    @Column(name = "factor_key", unique = true, nullable = false)
    private String factorKey;

    @Column(name = "factor_label", nullable = false)
    private String factorLabel;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "base_score_impact", nullable = false, precision = 5, scale = 2)
    private BigDecimal baseScoreImpact;

    @Column(name = "premium_multiplier", nullable = false, precision = 4, scale = 3)
    private BigDecimal premiumMultiplier;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
