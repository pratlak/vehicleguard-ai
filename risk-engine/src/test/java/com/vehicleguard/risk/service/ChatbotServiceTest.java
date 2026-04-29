package com.vehicleguard.risk.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehicleguard.risk.model.dto.ChatRequest;
import com.vehicleguard.risk.model.dto.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private AIExplanationService aiExplanationService;

    private ChatbotService chatbotService;
    private String sessionId;

    @BeforeEach
    void setUp() {
        chatbotService = new ChatbotService(aiExplanationService, new ObjectMapper());
        sessionId = UUID.randomUUID().toString();
    }

    @Test
    void handleMessage_greeting_returnsReply() {
        when(aiExplanationService.callClaudeWithHistory(anyString(), anyList()))
                .thenReturn("Hi! I'm VehicleGuard. How old are you?");

        ChatRequest request = ChatRequest.builder()
                .sessionId(sessionId)
                .message("Hello, I want a quote")
                .build();

        ChatResponse response = chatbotService.handleMessage(request);

        assertThat(response.getReply()).isNotNull().isNotBlank();
        assertThat(response.getSessionId()).isEqualTo(sessionId);
        assertThat(response.getSuggestedActions()).isNotNull();
    }

    @Test
    void handleMessage_detectsSubmitQuoteAction() {
        String submitQuoteJson = """
                {"action": "SUBMIT_QUOTE", "data": {
                    "driverAge": 28,
                    "licenseYears": 6,
                    "violationsLast5Yr": 0,
                    "accidentsLast5Yr": 0,
                    "vehicleMake": "Toyota",
                    "vehicleModel": "Camry",
                    "vehicleYear": 2020,
                    "zipCode": "90210",
                    "coverageType": "FULL"
                }}
                """;

        when(aiExplanationService.callClaudeWithHistory(anyString(), anyList()))
                .thenReturn(submitQuoteJson);

        ChatRequest request = ChatRequest.builder()
                .sessionId(sessionId)
                .message("That's all my info")
                .build();

        ChatResponse response = chatbotService.handleMessage(request);

        assertThat(response.getDetectedAction()).isEqualTo("SUBMIT_QUOTE");
        assertThat(response.getQuoteData()).isNotNull();
        assertThat(response.getQuoteData().getDriverAge()).isEqualTo(28);
        assertThat(response.getQuoteData().getVehicleMake()).isEqualTo("Toyota");
        assertThat(response.getQuoteData().getCoverageType()).isEqualTo("FULL");
        assertThat(response.getSuggestedActions()).contains("VIEW_QUOTE");
    }

    @Test
    void handleMessage_normalReply_noSubmitQuoteDetected() {
        when(aiExplanationService.callClaudeWithHistory(anyString(), anyList()))
                .thenReturn("Great, how many years have you had your license?");

        ChatRequest request = ChatRequest.builder()
                .sessionId(sessionId)
                .message("I'm 28 years old")
                .build();

        ChatResponse response = chatbotService.handleMessage(request);

        assertThat(response.getDetectedAction()).isNull();
        assertThat(response.getQuoteData()).isNull();
        assertThat(response.getReply()).contains("license");
    }

    @Test
    void handleMessage_conversationMaintainsHistory() {
        when(aiExplanationService.callClaudeWithHistory(anyString(), anyList()))
                .thenReturn("How old are you?")
                .thenReturn("How many years have you been licensed?");

        ChatRequest first = ChatRequest.builder()
                .sessionId(sessionId).message("Hi").build();
        ChatRequest second = ChatRequest.builder()
                .sessionId(sessionId).message("I'm 30").build();

        chatbotService.handleMessage(first);
        ChatResponse secondResp = chatbotService.handleMessage(second);

        assertThat(secondResp.getReply()).isNotNull();
        verify(aiExplanationService, times(2)).callClaudeWithHistory(anyString(), anyList());
    }

    @Test
    void handleMessage_withQuoteContext_includesContextInMessage() {
        when(aiExplanationService.callClaudeWithHistory(anyString(), anyList()))
                .thenReturn("Your premium is high because of your young age.");

        ChatRequest request = ChatRequest.builder()
                .sessionId(sessionId)
                .message("Why is my premium so high?")
                .quoteContext("Risk tier: HIGH, Score: 65")
                .build();

        ChatResponse response = chatbotService.handleMessage(request);

        assertThat(response.getReply()).isNotNull();
        verify(aiExplanationService).callClaudeWithHistory(anyString(), anyList());
    }

    @Test
    void handleMessage_claudeFailure_returnsFallbackMessage() {
        when(aiExplanationService.callClaudeWithHistory(anyString(), anyList()))
                .thenThrow(new RuntimeException("Claude API unavailable"));

        ChatRequest request = ChatRequest.builder()
                .sessionId(sessionId).message("Hello").build();

        ChatResponse response = chatbotService.handleMessage(request);

        assertThat(response.getReply()).isNotNull().contains("trouble connecting");
    }

    @Test
    void handleMessage_differentSessions_maintainSeparateHistory() {
        when(aiExplanationService.callClaudeWithHistory(anyString(), anyList()))
                .thenReturn("Response for session");

        String session1 = UUID.randomUUID().toString();
        String session2 = UUID.randomUUID().toString();

        chatbotService.handleMessage(ChatRequest.builder().sessionId(session1).message("Hello").build());
        chatbotService.handleMessage(ChatRequest.builder().sessionId(session2).message("Hi").build());

        verify(aiExplanationService, times(2)).callClaudeWithHistory(anyString(), anyList());
    }
}
