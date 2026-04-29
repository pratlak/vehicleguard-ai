package com.vehicleguard.risk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehicleguard.risk.model.dto.ChatRequest;
import com.vehicleguard.risk.model.dto.ChatResponse;
import com.vehicleguard.risk.service.ChatbotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private ChatbotService chatbotService;

    @Test
    void sendMessage_validRequest_returns200() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        ChatResponse response = ChatResponse.builder()
                .reply("Sure! How old are you?")
                .sessionId(sessionId)
                .suggestedActions(List.of())
                .build();

        when(chatbotService.handleMessage(any(ChatRequest.class))).thenReturn(response);

        ChatRequest request = ChatRequest.builder()
                .sessionId(sessionId)
                .message("I want a quote")
                .build();

        mockMvc.perform(post("/api/v1/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", is("Sure! How old are you?")))
                .andExpect(jsonPath("$.sessionId", is(sessionId)));
    }

    @Test
    void sendMessage_withSubmitQuoteAction_returnsActionInResponse() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        ChatResponse response = ChatResponse.builder()
                .reply("Submitting your quote now!")
                .sessionId(sessionId)
                .suggestedActions(List.of("VIEW_QUOTE", "MODIFY_COVERAGE"))
                .detectedAction("SUBMIT_QUOTE")
                .build();

        when(chatbotService.handleMessage(any(ChatRequest.class))).thenReturn(response);

        ChatRequest request = ChatRequest.builder()
                .sessionId(sessionId)
                .message("That's all the info you need")
                .build();

        mockMvc.perform(post("/api/v1/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detectedAction", is("SUBMIT_QUOTE")))
                .andExpect(jsonPath("$.suggestedActions", hasItem("VIEW_QUOTE")));
    }

    @Test
    void sendMessage_missingSessionId_returns400() throws Exception {
        ChatRequest invalid = ChatRequest.builder()
                .message("Hello")
                .build();

        mockMvc.perform(post("/api/v1/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendMessage_missingMessage_returns400() throws Exception {
        ChatRequest invalid = ChatRequest.builder()
                .sessionId(UUID.randomUUID().toString())
                .build();

        mockMvc.perform(post("/api/v1/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendMessage_withQuoteContext_processesCorrectly() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        ChatResponse response = ChatResponse.builder()
                .reply("Your premium is high because you are under 25.")
                .sessionId(sessionId)
                .suggestedActions(List.of())
                .build();

        when(chatbotService.handleMessage(any(ChatRequest.class))).thenReturn(response);

        ChatRequest request = ChatRequest.builder()
                .sessionId(sessionId)
                .message("Why is my premium high?")
                .quoteContext("Risk tier: HIGH, Score: 65")
                .build();

        mockMvc.perform(post("/api/v1/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", containsString("premium")));
    }
}
