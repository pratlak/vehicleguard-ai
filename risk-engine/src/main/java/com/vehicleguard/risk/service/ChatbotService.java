package com.vehicleguard.risk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehicleguard.risk.model.dto.AssessRequest;
import com.vehicleguard.risk.model.dto.ChatRequest;
import com.vehicleguard.risk.model.dto.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ChatbotService {

    private static final String SYSTEM_PROMPT =
            "You are VehicleGuard, a friendly vehicle insurance assistant. " +
                    "Collect these 7 fields one at a time: " +
                    "1) driverAge (number), " +
                    "2) licenseYears (number), " +
                    "3) violationsLast5Yr (number), " +
                    "4) accidentsLast5Yr (number), " +
                    "5) vehicleMake (string), " +
                    "6) vehicleModel (string), " +
                    "7) vehicleYear (number), " +
                    "8) zipCode (string), " +
                    "9) coverageType (LIABILITY, COLLISION, COMPREHENSIVE, or FULL). " +
                    "Ask one question at a time. " +
                    "CRITICAL: As soon as you have ALL 9 fields, you MUST respond with ONLY this JSON and nothing else - no explanation, no text before or after: " +
                    "{\"action\": \"SUBMIT_QUOTE\", \"data\": {\"driverAge\": <number>, \"licenseYears\": <number>, \"violationsLast5Yr\": <number>, \"accidentsLast5Yr\": <number>, \"vehicleMake\": \"<string>\", \"vehicleModel\": \"<string>\", \"vehicleYear\": <number>, \"zipCode\": \"<string>\", \"coverageType\": \"<LIABILITY|COLLISION|COMPREHENSIVE|FULL>\"}}";

    private static final Pattern SUBMIT_QUOTE_PATTERN =
            Pattern.compile(
                    "(?:```json\\s*)?\\{\\s*\"action\"\\s*:\\s*\"SUBMIT_QUOTE\".*?\\}\\s*\\}(?:\\s*```)?",
                    Pattern.DOTALL
            );

    private final AIExplanationService aiExplanationService;
    private final ObjectMapper objectMapper;
    private final Map<String, List<Map<String, String>>> sessions = new ConcurrentHashMap<>();

    public ChatbotService(AIExplanationService aiExplanationService, ObjectMapper objectMapper) {
        this.aiExplanationService = aiExplanationService;
        this.objectMapper = objectMapper;
    }

    public ChatResponse handleMessage(ChatRequest request) {
        String sessionId = request.getSessionId();
        String userMessage = request.getMessage();
        log.info("Chat message received: sessionId={}", sessionId);

        List<Map<String, String>> history = sessions.computeIfAbsent(sessionId, k -> new ArrayList<>());

        if (request.getQuoteContext() != null && !request.getQuoteContext().isBlank()) {
            userMessage = userMessage + "\n\n[Quote Context: " + request.getQuoteContext() + "]";
        }

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        history.add(userMsg);

        String rawReply = callClaude(history);
        log.info("Claude raw reply: {}", rawReply);

        Map<String, String> assistantMsg = new HashMap<>();
        assistantMsg.put("role", "assistant");
        assistantMsg.put("content", rawReply);
        history.add(assistantMsg);

        AssessRequest quoteData = detectAndParseSubmitQuote(rawReply);
        String detectedAction = quoteData != null ? "SUBMIT_QUOTE" : null;
        String cleanReply = quoteData != null
                ? "I've collected all the information needed. Submitting your quote now!"
                : rawReply;

        return ChatResponse.builder()
                .reply(cleanReply)
                .sessionId(sessionId)
                .suggestedActions(buildSuggestedActions(detectedAction))
                .detectedAction(detectedAction)
                .quoteData(quoteData)
                .build();
    }

    private String callClaude(List<Map<String, String>> history) {
        try {
            return aiExplanationService.callClaudeWithHistory(SYSTEM_PROMPT, history);
        } catch (Exception e) {
            log.error("Claude API call failed in chatbot: {}", e.getMessage());
            return "I apologize, I'm having trouble connecting right now. Please try again in a moment.";
        }
    }

    private AssessRequest detectAndParseSubmitQuote(String reply) {
        try {
            // Strip markdown code blocks if present
            String cleaned = reply.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            }

            // Check if it contains SUBMIT_QUOTE action
            if (!cleaned.contains("SUBMIT_QUOTE")) {
                return null;
            }

            // Parse the JSON directly
            JsonNode root = objectMapper.readTree(cleaned);
            if (!"SUBMIT_QUOTE".equals(root.path("action").asText())) {
                return null;
            }

            JsonNode data = root.path("data");
            AssessRequest request = new AssessRequest();
            request.setDriverAge(data.path("driverAge").asInt());
            request.setLicenseYears(data.path("licenseYears").asInt());
            request.setViolationsLast5Yr(data.path("violationsLast5Yr").asInt());
            request.setAccidentsLast5Yr(data.path("accidentsLast5Yr").asInt());
            request.setVehicleMake(data.path("vehicleMake").asText());
            request.setVehicleModel(data.path("vehicleModel").asText());
            request.setVehicleYear(data.path("vehicleYear").asInt());
            request.setZipCode(data.path("zipCode").asText());
            request.setCoverageType(data.path("coverageType").asText());

            log.info("SUBMIT_QUOTE detected and parsed successfully");
            return request;

        } catch (Exception e) {
            log.warn("Failed to parse SUBMIT_QUOTE: {}", e.getMessage());
            return null;
        }
    }

    private Integer getInt(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n != null && !n.isNull()) ? n.asInt() : null;
    }

    private String getString(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n != null && !n.isNull()) ? n.asText() : null;
    }

    private List<String> buildSuggestedActions(String detectedAction) {
        if ("SUBMIT_QUOTE".equals(detectedAction)) {
            return List.of("VIEW_QUOTE", "MODIFY_COVERAGE");
        }
        return Collections.emptyList();
    }
}
