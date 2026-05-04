package com.vehicleguard.risk.service;

import com.vehicleguard.risk.exception.AIServiceException;
import com.vehicleguard.risk.model.dto.AppliedFactor;
import com.vehicleguard.risk.model.dto.AssessRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIExplanationServiceTest {

    @Mock private WebClient anthropicWebClient;
    @Mock private WebClient.RequestBodyUriSpec uriSpec;
    @Mock private WebClient.RequestBodySpec bodySpec;
    @Mock private WebClient.ResponseSpec responseSpec;

    private AIExplanationService service;

    @BeforeEach
    void setUp() {
        service = new AIExplanationService(anthropicWebClient, "claude-sonnet-4-20250514");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void setupSuccessfulMock(Map<String, Object> apiResponse) {
        when(anthropicWebClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        doReturn(bodySpec).when(bodySpec).bodyValue(any());
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(apiResponse));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void setupErrorMock(Throwable error) {
        when(anthropicWebClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        doReturn(bodySpec).when(bodySpec).bodyValue(any());
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.error(error));
    }

    @Test
    void callClaudeAPI_success_returnsTextContent() {
        Map<String, Object> content = Map.of("type", "text", "text", "Your premium is higher due to young driver status.");
        Map<String, Object> apiResponse = Map.of("content", List.of(content));

        setupSuccessfulMock(apiResponse);

        String result = service.callClaudeAPI("system prompt", "user message");

        assertThat(result).isEqualTo("Your premium is higher due to young driver status.");
    }

    @Test
    void callClaudeAPI_emptyResponse_throwsAIServiceException() {
        when(anthropicWebClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        doReturn(bodySpec).when(bodySpec).bodyValue(any());
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        //noinspection unchecked
        when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.empty());

        assertThatThrownBy(() -> service.callClaudeAPI("system", "user"))
                .isInstanceOf(AIServiceException.class);
    }

    @Test
    void generateExplanation_apiFailure_returnsFallbackExplanation() {
        setupErrorMock(new RuntimeException("API down"));

        AssessRequest req = AssessRequest.builder()
                .driverAge(22).licenseYears(1).violationsLast5Yr(0).accidentsLast5Yr(0)
                .zipCode("10001").coverageType("FULL").build();

        List<AppliedFactor> factors = List.of(
                AppliedFactor.builder().key("driver_age_under_25").label("Driver under 25 years old")
                        .scoreImpact(BigDecimal.valueOf(15)).multiplier(BigDecimal.valueOf(1.25)).build()
        );

        String explanation = service.generateExplanation(req, factors, BigDecimal.valueOf(50), "HIGH");

        assertThat(explanation).isNotNull().isNotBlank();
        assertThat(explanation).doesNotContain("driver_age_under_25");
        assertThat(explanation).contains("Driver under 25 years old");
    }

    @Test
    void generateExplanation_emptyFactors_returnsFallbackWithLowRiskMessage() {
        setupErrorMock(new RuntimeException("test"));

        AssessRequest req = AssessRequest.builder()
                .driverAge(35).licenseYears(10).violationsLast5Yr(0).accidentsLast5Yr(0)
                .zipCode("55555").coverageType("LIABILITY").build();

        String result = service.generateExplanation(req, List.of(), BigDecimal.valueOf(0), "LOW");

        assertThat(result).isNotNull();
        assertThat(result).containsIgnoringCase("low risk");
    }

    @Test
    void generateExplanation_success_returnsAIGeneratedExplanation() {
        Map<String, Object> content = Map.of("type", "text", "text", "AI generated explanation.");
        Map<String, Object> apiResponse = Map.of("content", List.of(content));
        setupSuccessfulMock(apiResponse);

        AssessRequest req = AssessRequest.builder()
                .driverAge(23).licenseYears(2).violationsLast5Yr(1).accidentsLast5Yr(0)
                .vehicleMake("Ford").vehicleModel("Mustang").vehicleYear(2021)
                .zipCode("98004").coverageType("FULL").build();

        List<AppliedFactor> factors = List.of(
                AppliedFactor.builder().key("driver_age_under_25").label("Driver under 25 years old")
                        .scoreImpact(BigDecimal.valueOf(15)).multiplier(BigDecimal.valueOf(1.25)).build()
        );

        String result = service.generateExplanation(req, factors, BigDecimal.valueOf(62.5), "HIGH");

        assertThat(result).isEqualTo("AI generated explanation.");
    }
}
