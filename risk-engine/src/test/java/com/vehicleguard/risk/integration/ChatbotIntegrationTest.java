package com.vehicleguard.risk.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehicleguard.risk.model.dto.ChatRequest;
import com.vehicleguard.risk.model.dto.ChatResponse;
import com.vehicleguard.risk.service.AIExplanationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChatbotIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AIExplanationService aiExplanationService;
    @MockBean @Qualifier("ratesWebClient") private WebClient ratesWebClient;

    @Test
    void sendMessage_greeting_returnsFirstQuestion() throws Exception {
        when(aiExplanationService.callClaudeWithHistory(anyString(), anyList()))
                .thenReturn("Hi there! I'd love to help you get a quote. How old are you?");

        ChatRequest request = ChatRequest.builder()
                .sessionId(UUID.randomUUID().toString())
                .message("Hello, I want insurance")
                .build();

        mockMvc.perform(post("/api/v1/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", notNullValue()))
                .andExpect(jsonPath("$.sessionId", notNullValue()));
    }

    @Test
    void sendMessage_fullConversation_detectsSubmitQuote() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        String submitQuoteJson = """
                {"action": "SUBMIT_QUOTE", "data": {
                    "driverAge": 30,
                    "licenseYears": 8,
                    "violationsLast5Yr": 0,
                    "accidentsLast5Yr": 0,
                    "vehicleMake": "Toyota",
                    "vehicleModel": "Camry",
                    "vehicleYear": 2021,
                    "zipCode": "55555",
                    "coverageType": "FULL"
                }}""";

        // Set up Claude to respond conversationally for first 6 messages, then SUBMIT_QUOTE on 7th
        when(aiExplanationService.callClaudeWithHistory(anyString(), anyList()))
                .thenReturn("How old are you?")
                .thenReturn("How many years have you been licensed?")
                .thenReturn("How many traffic violations in the last 5 years?")
                .thenReturn("How many at-fault accidents in the last 5 years?")
                .thenReturn("What is your vehicle make and model?")
                .thenReturn("What year is your vehicle?")
                .thenReturn(submitQuoteJson);

        String[] messages = {
                "I want a quote",
                "I am 30 years old",
                "8 years licensed",
                "0 violations",
                "0 accidents",
                "Toyota Camry",
                "2021 and my zip is 55555, full coverage"
        };

        ChatResponse lastResponse = null;
        for (String msg : messages) {
            ChatRequest request = ChatRequest.builder()
                    .sessionId(sessionId)
                    .message(msg)
                    .build();

            MvcResult result = mockMvc.perform(post("/api/v1/chat/message")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            lastResponse = objectMapper.readValue(
                    result.getResponse().getContentAsString(), ChatResponse.class);
        }

        assertThat(lastResponse).isNotNull();
        assertThat(lastResponse.getDetectedAction()).isEqualTo("SUBMIT_QUOTE");
        assertThat(lastResponse.getQuoteData()).isNotNull();
        assertThat(lastResponse.getQuoteData().getDriverAge()).isEqualTo(30);
        assertThat(lastResponse.getQuoteData().getVehicleMake()).isEqualTo("Toyota");
    }

    @Test
    void sendMessage_followUpQuestion_returnsExplanation() throws Exception {
        when(aiExplanationService.callClaudeWithHistory(anyString(), anyList()))
                .thenReturn("Your premium is higher because you are under 25 and drive a sports car, " +
                            "both of which statistically correlate with higher claims rates.");

        ChatRequest request = ChatRequest.builder()
                .sessionId(UUID.randomUUID().toString())
                .message("Why is my premium so high?")
                .quoteContext("Risk tier: HIGH, Score: 65/100, Factors: Driver under 25, Sports vehicle")
                .build();

        mockMvc.perform(post("/api/v1/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", containsString("premium")));
    }

    @Test
    void sendMessage_missingSessionId_returns400() throws Exception {
        ChatRequest invalid = ChatRequest.builder()
                .message("Hello").build();

        mockMvc.perform(post("/api/v1/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }
}
