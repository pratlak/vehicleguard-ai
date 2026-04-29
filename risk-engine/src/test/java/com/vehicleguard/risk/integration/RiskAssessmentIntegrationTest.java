package com.vehicleguard.risk.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehicleguard.risk.model.dto.AppliedFactor;
import com.vehicleguard.risk.model.dto.AssessRequest;
import com.vehicleguard.risk.model.dto.AssessResponse;
import com.vehicleguard.risk.repository.QuoteRepository;
import com.vehicleguard.risk.service.AIExplanationService;
import com.vehicleguard.risk.service.RiskScoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RiskAssessmentIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private RiskScoringService riskScoringService;

    @MockBean private AIExplanationService aiExplanationService;
    @MockBean @Qualifier("ratesWebClient") private WebClient ratesWebClient;

    @SuppressWarnings({"rawtypes", "unchecked"})
    @BeforeEach
    void setupMocks() {
        when(aiExplanationService.generateExplanation(any(), any(), any(), any()))
                .thenReturn("Mock AI explanation: Your premium reflects identified risk factors.");

        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(ratesWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.just(Map.of("baseAnnualPremium", 1800.0, "vehicleCategory", "sports")));

        riskScoringService.loadRiskFactors();
    }

    @Test
    void assess_highRiskDriverSportsCar_returnsHighTierWithFactors() throws Exception {
        // driverAge=23 (+15), licenseYears=1 (+12), violations=1 (+8), sports (+12) = 47/90 = 52.2% -> HIGH
        AssessRequest request = AssessRequest.builder()
                .driverAge(23).licenseYears(1).violationsLast5Yr(1).accidentsLast5Yr(0)
                .vehicleMake("Ford").vehicleModel("Mustang").vehicleYear(2021)
                .zipCode("98004").coverageType("FULL").build();

        MvcResult result = mockMvc.perform(post("/api/v1/risk/assess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.riskScore", notNullValue()))
                .andExpect(jsonPath("$.riskTier", not(emptyString())))
                .andExpect(jsonPath("$.annualPremiumUsd", greaterThan(0)))
                .andExpect(jsonPath("$.appliedFactors", not(empty())))
                .andExpect(jsonPath("$.aiExplanation", notNullValue()))
                .andReturn();

        AssessResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AssessResponse.class);

        // Verify quote was persisted
        assertThat(quoteRepository.findById(response.getQuoteId())).isPresent();

        // Verify applied factors are not empty
        assertThat(response.getAppliedFactors()).isNotEmpty();

        // Verify aiExplanation
        assertThat(response.getAiExplanation()).isNotBlank();

        // Verify riskTier is HIGH or above (47/90 = 52%)
        assertThat(response.getRiskTier()).isIn("HIGH", "VERY_HIGH");
    }

    @Test
    void assess_returnedQuoteCanBeRetrievedById() throws Exception {
        AssessRequest request = AssessRequest.builder()
                .driverAge(30).licenseYears(8).violationsLast5Yr(0).accidentsLast5Yr(0)
                .vehicleMake("Ford").vehicleModel("Mustang").vehicleYear(2022)
                .zipCode("55555").coverageType("COLLISION").build();

        MvcResult assessResult = mockMvc.perform(post("/api/v1/risk/assess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        AssessResponse created = objectMapper.readValue(
                assessResult.getResponse().getContentAsString(), AssessResponse.class);

        mockMvc.perform(get("/api/v1/risk/quote/" + created.getQuoteId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteId", is(created.getQuoteId().toString())))
                .andExpect(jsonPath("$.riskTier", is(created.getRiskTier())));
    }

    @Test
    void assess_invalidRequest_returns400() throws Exception {
        AssessRequest invalid = AssessRequest.builder()
                .driverAge(10) // Too young
                .licenseYears(-1) // Negative
                .zipCode("").coverageType("").build();

        mockMvc.perform(post("/api/v1/risk/assess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void assess_lowRiskDriver_returnsLowTier() throws Exception {
        // age=35, sedan, no violations, no accidents -> no score impact -> LOW
        when(ratesWebClient.get()).thenReturn(mockGetForBaseRate(1100.0));

        AssessRequest request = AssessRequest.builder()
                .driverAge(35).licenseYears(12).violationsLast5Yr(0).accidentsLast5Yr(0)
                .vehicleMake("Toyota").vehicleModel("Camry").vehicleYear(2021)
                .zipCode("55555").coverageType("LIABILITY").build();

        mockMvc.perform(post("/api/v1/risk/assess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.riskTier", is("LOW")))
                .andExpect(jsonPath("$.riskScore", is(0.0)));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void assess_veryHighRiskDriver_returnsVeryHighTier() throws Exception {
        // age=22 (+15), licenseYears=1 (+12), violations=2 (+18), accidents=2 (+22), sports (+12), NYC zip (+5) = 84/90 = 93.3%
        WebClient.RequestHeadersUriSpec spec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec hSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec rSpec = mock(WebClient.ResponseSpec.class);
        when(ratesWebClient.get()).thenReturn(spec);
        when(spec.uri(any(Function.class))).thenReturn(hSpec);
        when(hSpec.retrieve()).thenReturn(rSpec);
        when(rSpec.bodyToMono(Map.class)).thenReturn(Mono.just(Map.of("baseAnnualPremium", 1800.0)));

        AssessRequest request = AssessRequest.builder()
                .driverAge(22).licenseYears(1).violationsLast5Yr(2).accidentsLast5Yr(2)
                .vehicleMake("Ford").vehicleModel("Mustang").vehicleYear(2021)
                .zipCode("10001").coverageType("FULL").build();

        mockMvc.perform(post("/api/v1/risk/assess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.riskTier", is("VERY_HIGH")));
    }

    @Test
    void getQuote_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/risk/quote/" + UUID.randomUUID())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void actuatorHealth_returns200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private WebClient.RequestHeadersUriSpec<?> mockGetForBaseRate(double rate) {
        WebClient.RequestHeadersUriSpec spec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec hSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec rSpec = mock(WebClient.ResponseSpec.class);
        when(spec.uri(any(Function.class))).thenReturn(hSpec);
        when(hSpec.retrieve()).thenReturn(rSpec);
        when(rSpec.bodyToMono(Map.class)).thenReturn(Mono.just(Map.of("baseAnnualPremium", rate)));
        return spec;
    }
}
