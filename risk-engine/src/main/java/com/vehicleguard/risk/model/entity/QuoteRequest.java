package com.vehicleguard.risk.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(schema = "quotes", name = "quote_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "driver_age", nullable = false)
    private Integer driverAge;

    @Column(name = "license_years", nullable = false)
    private Integer licenseYears;

    @Column(name = "violations_last_5yr", nullable = false)
    private Integer violationsLast5yr;

    @Column(name = "accidents_last_5yr", nullable = false)
    private Integer accidentsLast5yr;

    @Column(name = "vehicle_make")
    private String vehicleMake;

    @Column(name = "vehicle_model")
    private String vehicleModel;

    @Column(name = "vehicle_year")
    private Integer vehicleYear;

    @Column(name = "vehicle_category")
    private String vehicleCategory;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "state_code", columnDefinition = "char(2)")
    private String stateCode;

    @Column(name = "coverage_type")
    private String coverageType;

    @Column(name = "risk_score", precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Column(name = "risk_tier")
    private String riskTier;

    @Column(name = "annual_premium_usd", precision = 10, scale = 2)
    private BigDecimal annualPremiumUsd;

    @Column(name = "monthly_premium_usd", precision = 10, scale = 2)
    private BigDecimal monthlyPremiumUsd;

    @Column(name = "applied_factors_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String appliedFactorsJson;

    @Column(name = "ai_explanation", columnDefinition = "text")
    private String aiExplanation;

    @Column(name = "input_payload_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String inputPayloadJson;
}
