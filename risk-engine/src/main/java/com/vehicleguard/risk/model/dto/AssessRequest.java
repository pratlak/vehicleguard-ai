package com.vehicleguard.risk.model.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessRequest {

    @NotNull(message = "Driver age is required")
    @Min(value = 16, message = "Driver age must be at least 16")
    @Max(value = 120, message = "Driver age must be at most 120")
    private Integer driverAge;

    @NotNull(message = "License years is required")
    @Min(value = 0, message = "License years must be non-negative")
    private Integer licenseYears;

    @NotNull(message = "Violations count is required")
    @Min(value = 0, message = "Violations must be non-negative")
    private Integer violationsLast5Yr;

    @NotNull(message = "Accidents count is required")
    @Min(value = 0, message = "Accidents must be non-negative")
    private Integer accidentsLast5Yr;

    private String vehicleMake;
    private String vehicleModel;

    @Min(value = 1900, message = "Vehicle year must be 1900 or later")
    private Integer vehicleYear;

    @NotBlank(message = "ZIP code is required")
    private String zipCode;

    private String stateCode;

    @NotBlank(message = "Coverage type is required")
    private String coverageType;

    private String vehicleCategory;
}
