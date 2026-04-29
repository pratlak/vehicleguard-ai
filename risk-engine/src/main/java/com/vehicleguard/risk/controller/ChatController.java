package com.vehicleguard.risk.controller;

import com.vehicleguard.risk.model.dto.ChatRequest;
import com.vehicleguard.risk.model.dto.ChatResponse;
import com.vehicleguard.risk.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatbotService chatbotService;

    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(@Valid @RequestBody ChatRequest request) {
        log.info("POST /api/v1/chat/message sessionId={}", request.getSessionId());
        return ResponseEntity.ok(chatbotService.handleMessage(request));
    }
}
