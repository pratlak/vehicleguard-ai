package com.vehicleguard.rates.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(schema = "risk_rules", name = "vehicle_base_rates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleBaseRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "serial")
    private Integer id;

    @Column(name = "vehicle_category", unique = true, nullable = false)
    private String vehicleCategory;

    @Column(name = "base_annual_premium", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseAnnualPremium;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
