package com.vehicleguard.risk.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehicleguard.risk.model.dto.AppliedFactor;
import com.vehicleguard.risk.model.dto.AssessRequest;
import com.vehicleguard.risk.model.entity.RiskFactor;
import com.vehicleguard.risk.repository.QuoteRepository;
import com.vehicleguard.risk.repository.RiskFactorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskScoringServiceTest {

    @Mock private RiskFactorRepository riskFactorRepository;
    @Mock private QuoteRepository quoteRepository;
    @Mock private AIExplanationService aiExplanationService;
    @Mock private WebClient ratesWebClient;

    private RiskScoringService service;

    @BeforeEach
    void setUp() {
        service = new RiskScoringService(
                riskFactorRepository, quoteRepository, aiExplanationService,
                ratesWebClient, new ObjectMapper());
        when(riskFactorRepository.findByIsActiveTrue()).thenReturn(createTestFactors());
        service.loadRiskFactors();
    }

    @Test
    void evaluateFactors_driverUnder25_appliesYoungDriverFactor() {
        AssessRequest req = baseRequest().driverAge(22).build();
        List<AppliedFactor> factors = service.evaluateFactors(req, "sedan", 2024);

        assertThat(factors).anyMatch(f -> "driver_age_under_25".equals(f.getKey()));
        assertThat(factors).noneMatch(f -> "driver_age_over_70".equals(f.getKey()));
    }

    @Test
    void evaluateFactors_driverOver70_appliesSeniorFactor() {
        AssessRequest req = baseRequest().driverAge(72).build();
        List<AppliedFactor> factors = service.evaluateFactors(req, "sedan", 2024);

        assertThat(factors).anyMatch(f -> "driver_age_over_70".equals(f.getKey()));
        assertThat(factors).noneMatch(f -> "driver_age_under_25".equals(f.getKey()));
    }

    @Test
    void evaluateFactors_licenseUnder2Years_appliesFactor() {
        AssessRequest req = baseRequest().licenseYears(1).build();
        List<AppliedFactor> factors = service.evaluateFactors(req, "sedan", 2024);

        assertThat(factors).anyMatch(f -> "license_under_2yr".equals(f.getKey()));
    }

    @Test
    void evaluateFactors_oneViolation_appliesCount1Factor() {
        AssessRequest req = baseRequest().violationsLast5Yr(1).build();
        List<AppliedFactor> factors = service.evaluateFactors(req, "sedan", 2024);

        assertThat(factors).anyMatch(f -> "violation_count_1".equals(f.getKey()));
        assertThat(factors).noneMatch(f -> "violation_count_2plus".equals(f.getKey()));
    }

    @Test
    void evaluateFactors_twoViolations_appliesCount2PlusFactor() {
        AssessRequest req = baseRequest().violationsLast5Yr(2).build();
        List<AppliedFactor> factors = service.evaluateFactors(req, "sedan", 2024);

        assertThat(factors).anyMatch(f -> "violation_count_2plus".equals(f.getKey()));
        assertThat(factors).noneMatch(f -> "violation_count_1".equals(f.getKey()));
    }

    @Test
    void evaluateFactors_oneAccident_appliesCount1Factor() {
        AssessRequest req = baseRequest().accidentsLast5Yr(1).build();
        List<AppliedFactor> factors = service.evaluateFactors(req, "sedan", 2024);

        assertThat(factors).anyMatch(f -> "accident_count_1".equals(f.getKey()));
        assertThat(factors).noneMatch(f -> "accident_count_2plus".equals(f.getKey()));
    }

    @Test
    void evaluateFactors_twoAccidents_appliesCount2PlusFactor() {
        AssessRequest req = baseRequest().accidentsLast5Yr(2).build();
        List<AppliedFactor> factors = service.evaluateFactors(req, "sedan", 2024);

        assertThat(factors).anyMatch(f -> "accident_count_2plus".equals(f.getKey()));
        assertThat(factors).noneMatch(f -> "accident_count_1".equals(f.getKey()));
    }

    @Test
    void evaluateFactors_sportsCar_appliesVehicleSports() {
        AssessRequest req = baseRequest().build();
        List<AppliedFactor> factors = service.evaluateFactors(req, "sports", 2024);

        assertThat(factors).anyMatch(f -> "vehicle_sports".equals(f.getKey()));
    }

    @Test
    void evaluateFactors_luxuryCar_appliesVehicleLuxury() {
        AssessRequest req = baseRequest().build();
        List<AppliedFactor> factors = service.evaluateFactors(req, "luxury", 2024);

        assertThat(factors).anyMatch(f -> "vehicle_luxury".equals(f.getKey()));
    }

    @Test
    void evaluateFactors_oldVehicle_appliesAgeOver15() {
        AssessRequest req = baseRequest().vehicleYear(2005).build();
        List<AppliedFactor> factors = service.evaluateFactors(req, "sedan", 2024);

        assertThat(factors).anyMatch(f -> "vehicle_age_over_15".equals(f.getKey()));
    }

    @Test
    void evaluateFactors_newVehicle_doesNotApplyAgeOver15() {
        AssessRequest req = baseRequest().vehicleYear(2022).build();
        List<AppliedFactor> factors = service.evaluateFactors(req, "sedan", 2024);

        assertThat(factors).noneMatch(f -> "vehicle_age_over_15".equals(f.getKey()));
    }

    @Test
    void evaluateFactors_fullCoverage_appliesCoverageFull() {
        AssessRequest req = baseRequest().coverageType("FULL").build();
        List<AppliedFactor> factors = service.evaluateFactors(req, "sedan", 2024);

        assertThat(factors).anyMatch(f -> "coverage_full".equals(f.getKey()));
    }

    @Test
    void evaluateFactors_comprehensiveCoverage_appliesCoverageComprehensive() {
        AssessRequest req = baseRequest().coverageType("COMPREHENSIVE").build();
        List<AppliedFactor> factors = service.evaluateFactors(req, "sedan", 2024);

        assertThat(factors).anyMatch(f -> "coverage_comprehensive".equals(f.getKey()));
    }

    @Test
    void evaluateFactors_highDensityZip_appliesLocationFactor() {
        AssessRequest req = baseRequest().zipCode("10001").build();
        List<AppliedFactor> factors = service.evaluateFactors(req, "sedan", 2024);

        assertThat(factors).anyMatch(f -> "high_density_zip".equals(f.getKey()));
    }

    @Test
    void evaluateFactors_lowDensityZip_doesNotApplyLocationFactor() {
        AssessRequest req = baseRequest().zipCode("99999").build();
        List<AppliedFactor> factors = service.evaluateFactors(req, "sedan", 2024);

        assertThat(factors).noneMatch(f -> "high_density_zip".equals(f.getKey()));
    }

    @Test
    void evaluateFactors_cleanRecord_noRiskFactors() {
        AssessRequest req = AssessRequest.builder()
                .driverAge(35)
                .licenseYears(10)
                .violationsLast5Yr(0)
                .accidentsLast5Yr(0)
                .vehicleYear(2020)
                .zipCode("55555")
                .coverageType("LIABILITY")
                .build();

        List<AppliedFactor> factors = service.evaluateFactors(req, "sedan", 2024);
        assertThat(factors).isEmpty();
    }

    @Test
    void assignRiskTier_score0_returnsLow() {
        assertThat(service.assignRiskTier(0.0)).isEqualTo("LOW");
    }

    @Test
    void assignRiskTier_score25_returnsLow() {
        assertThat(service.assignRiskTier(25.0)).isEqualTo("LOW");
    }

    @Test
    void assignRiskTier_score26_returnsMedium() {
        assertThat(service.assignRiskTier(26.0)).isEqualTo("MEDIUM");
    }

    @Test
    void assignRiskTier_score50_returnsMedium() {
        assertThat(service.assignRiskTier(50.0)).isEqualTo("MEDIUM");
    }

    @Test
    void assignRiskTier_score51_returnsHigh() {
        assertThat(service.assignRiskTier(51.0)).isEqualTo("HIGH");
    }

    @Test
    void assignRiskTier_score75_returnsHigh() {
        assertThat(service.assignRiskTier(75.0)).isEqualTo("HIGH");
    }

    @Test
    void assignRiskTier_score76_returnsVeryHigh() {
        assertThat(service.assignRiskTier(76.0)).isEqualTo("VERY_HIGH");
    }

    @Test
    void assignRiskTier_score100_returnsVeryHigh() {
        assertThat(service.assignRiskTier(100.0)).isEqualTo("VERY_HIGH");
    }

    @Test
    void calculatePremium_withMultipliers_multipliesCorrectly() {
        List<AppliedFactor> factors = List.of(
                AppliedFactor.builder().key("k1").label("l1").scoreImpact(BigDecimal.TEN)
                        .multiplier(new BigDecimal("1.25")).build(),
                AppliedFactor.builder().key("k2").label("l2").scoreImpact(BigDecimal.TEN)
                        .multiplier(new BigDecimal("1.20")).build()
        );

        BigDecimal base = new BigDecimal("1000.00");
        BigDecimal result = service.calculatePremium(base, factors);

        assertThat(result).isEqualByComparingTo("1500.00");
    }

    @Test
    void calculatePremium_noFactors_returnsBaseRate() {
        BigDecimal result = service.calculatePremium(new BigDecimal("1100.00"), List.of());
        assertThat(result).isEqualByComparingTo("1100.00");
    }

    @Test
    void detectVehicleCategory_mustang_returnsSports() {
        assertThat(service.detectVehicleCategory("Ford", "Mustang")).isEqualTo("sports");
    }

    @Test
    void detectVehicleCategory_f150_returnsTruck() {
        assertThat(service.detectVehicleCategory("Ford", "F-150")).isEqualTo("truck");
    }

    @Test
    void detectVehicleCategory_tesla_returnsElectric() {
        assertThat(service.detectVehicleCategory("Tesla", "Model 3")).isEqualTo("electric");
    }

    @Test
    void detectVehicleCategory_camry_returnsSedanDefault() {
        assertThat(service.detectVehicleCategory("Toyota", "Camry")).isEqualTo("sedan");
    }

    @Test
    void detectVehicleCategory_rav4_returnsSuv() {
        assertThat(service.detectVehicleCategory("Toyota", "RAV4")).isEqualTo("suv");
    }

    @Test
    void detectVehicleCategory_bmw7Series_returnsLuxury() {
        assertThat(service.detectVehicleCategory("BMW", "7-Series")).isEqualTo("luxury");
    }

    private AssessRequest.AssessRequestBuilder baseRequest() {
        return AssessRequest.builder()
                .driverAge(35)
                .licenseYears(10)
                .violationsLast5Yr(0)
                .accidentsLast5Yr(0)
                .vehicleYear(2020)
                .zipCode("55555")
                .coverageType("LIABILITY");
    }

    private List<RiskFactor> createTestFactors() {
        return List.of(
                factor("driver_age_under_25", "Driver under 25 years old", 15.00, 1.250),
                factor("driver_age_over_70",  "Driver over 70 years old",  10.00, 1.150),
                factor("license_under_2yr",   "License held less than 2 years", 12.00, 1.200),
                factor("violation_count_1",   "1 traffic violation in last 5 years", 8.00, 1.100),
                factor("violation_count_2plus","2 or more violations in last 5 years", 18.00, 1.300),
                factor("accident_count_1",    "1 at-fault accident in last 5 years", 10.00, 1.150),
                factor("accident_count_2plus","2 or more at-fault accidents", 22.00, 1.450),
                factor("vehicle_sports",      "Sports or performance vehicle", 12.00, 1.200),
                factor("vehicle_luxury",      "Luxury vehicle", 8.00, 1.150),
                factor("vehicle_age_over_15", "Vehicle older than 15 years", 6.00, 1.100),
                factor("coverage_comprehensive","Comprehensive coverage selected", 0.00, 1.300),
                factor("coverage_collision",  "Collision coverage selected", 0.00, 1.150),
                factor("coverage_full",       "Full coverage selected", 0.00, 1.500),
                factor("high_density_zip",    "High-density urban area", 5.00, 1.080)
        );
    }

    private RiskFactor factor(String key, String label, double impact, double multiplier) {
        return RiskFactor.builder()
                .factorKey(key)
                .factorLabel(label)
                .baseScoreImpact(BigDecimal.valueOf(impact))
                .premiumMultiplier(BigDecimal.valueOf(multiplier))
                .isActive(true)
                .build();
    }
}
