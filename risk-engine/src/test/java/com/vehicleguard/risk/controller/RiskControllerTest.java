package com.vehicleguard.risk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehicleguard.risk.exception.QuoteNotFoundException;
import com.vehicleguard.risk.model.dto.AppliedFactor;
import com.vehicleguard.risk.model.dto.AssessRequest;
import com.vehicleguard.risk.model.dto.AssessResponse;
import com.vehicleguard.risk.service.RiskScoringService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RiskController.class)
class RiskControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private RiskScoringService riskScoringService;

    private final UUID quoteId = UUID.randomUUID();

    @Test
    void assess_validRequest_returns201() throws Exception {
        AssessResponse response = buildMockResponse("HIGH", 62.5, 3240.00, 270.00);
        when(riskScoringService.assess(any(AssessRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/risk/assess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quoteId", is(quoteId.toString())))
                .andExpect(jsonPath("$.riskTier", is("HIGH")))
                .andExpect(jsonPath("$.riskScore", is(62.5)))
                .andExpect(jsonPath("$.annualPremiumUsd", is(3240.00)))
                .andExpect(jsonPath("$.monthlyPremiumUsd", is(270.00)))
                .andExpect(jsonPath("$.appliedFactors", hasSize(2)))
                .andExpect(jsonPath("$.aiExplanation", notNullValue()));
    }

    @Test
    void assess_missingDriverAge_returns400() throws Exception {
        AssessRequest invalid = AssessRequest.builder()
                .licenseYears(5).violationsLast5Yr(0).accidentsLast5Yr(0)
                .zipCode("90210").coverageType("FULL").build();

        mockMvc.perform(post("/api/v1/risk/assess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void assess_driverAgeBelowMin_returns400() throws Exception {
        AssessRequest invalid = buildValidRequest();
        invalid.setDriverAge(10);

        mockMvc.perform(post("/api/v1/risk/assess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assess_missingZipCode_returns400() throws Exception {
        AssessRequest invalid = buildValidRequest();
        invalid.setZipCode(null);

        mockMvc.perform(post("/api/v1/risk/assess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getQuote_existingId_returns200() throws Exception {
        AssessResponse response = buildMockResponse("MEDIUM", 38.5, 1540.00, 128.33);
        when(riskScoringService.getQuote(quoteId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/risk/quote/" + quoteId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteId", is(quoteId.toString())))
                .andExpect(jsonPath("$.riskTier", is("MEDIUM")));
    }

    @Test
    void getQuote_nonExistentId_returns404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(riskScoringService.getQuote(unknownId))
                .thenThrow(new QuoteNotFoundException("Quote not found: " + unknownId));

        mockMvc.perform(get("/api/v1/risk/quote/" + unknownId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    private AssessRequest buildValidRequest() {
        return AssessRequest.builder()
                .driverAge(23)
                .licenseYears(2)
                .violationsLast5Yr(1)
                .accidentsLast5Yr(0)
                .vehicleMake("Ford")
                .vehicleModel("Mustang")
                .vehicleYear(2021)
                .zipCode("98004")
                .coverageType("FULL")
                .build();
    }

    private AssessResponse buildMockResponse(String tier, double score, double annual, double monthly) {
        return AssessResponse.builder()
                .quoteId(quoteId)
                .riskScore(BigDecimal.valueOf(score))
                .riskTier(tier)
                .annualPremiumUsd(BigDecimal.valueOf(annual))
                .monthlyPremiumUsd(BigDecimal.valueOf(monthly))
                .coverageType("FULL")
                .appliedFactors(List.of(
                        AppliedFactor.builder().key("driver_age_under_25").label("Driver under 25 years old")
                                .scoreImpact(BigDecimal.valueOf(15)).multiplier(BigDecimal.valueOf(1.25)).build(),
                        AppliedFactor.builder().key("vehicle_sports").label("Sports or performance vehicle")
                                .scoreImpact(BigDecimal.valueOf(12)).multiplier(BigDecimal.valueOf(1.20)).build()
                ))
                .aiExplanation("Your premium is higher due to your age and vehicle type.")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
