package com.vehicleguard.risk.service;

import com.vehicleguard.risk.exception.AIServiceException;
import com.vehicleguard.risk.model.dto.AppliedFactor;
import com.vehicleguard.risk.model.dto.AssessRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AIExplanationService {

    private static final String EXPLANATION_SYSTEM_PROMPT =
            "You are a senior auto insurance underwriter. Given a risk assessment breakdown, " +
            "write a clear, professional 2-3 sentence explanation for a customer explaining which " +
            "factors most influenced their premium and why those factors matter from a risk perspective. " +
            "Never use internal factor keys. Use plain English only. Do not mention exact dollar amounts.";

    private final WebClient anthropicWebClient;
    private final String model;

    public AIExplanationService(@Qualifier("anthropicWebClient") WebClient anthropicWebClient,
                                 @Value("${anthropic.model:claude-sonnet-4-20250514}") String model) {
        this.anthropicWebClient = anthropicWebClient;
        this.model = model;
    }

    public String generateExplanation(AssessRequest request, List<AppliedFactor> factors,
                                       BigDecimal riskScore, String riskTier) {
        try {
            String userMessage = buildExplanationPrompt(request, factors, riskScore, riskTier);
            String response = callClaudeAPI(EXPLANATION_SYSTEM_PROMPT, userMessage);
            log.info("AI explanation generated for riskTier={}", riskTier);
            return response;
        } catch (Exception e) {
            log.warn("AI explanation failed, using fallback: {}", e.getMessage());
            return buildFallbackExplanation(factors, riskTier);
        }
    }

    /**
     * Single-turn Claude API call — used for generating explanations.
     */
    @SuppressWarnings("unchecked")
    public String callClaudeAPI(String systemPrompt, String userMessage) {
        List<Map<String, Object>> messages = List.of(
                Map.of("role", "user", "content", userMessage)
        );
        return callClaudeInternal(systemPrompt, messages, 512);
    }

    /**
     * Multi-turn Claude API call — used for the chatbot conversation.
     */
    @SuppressWarnings("unchecked")
    public String callClaudeWithHistory(String systemPrompt, List<Map<String, String>> history) {
        List<Map<String, Object>> messages = history.stream()
                .map(m -> Map.<String, Object>of("role", m.get("role"), "content", m.get("content")))
                .collect(Collectors.toList());
        return callClaudeInternal(systemPrompt, messages, 1024);
    }

    @SuppressWarnings("unchecked")
    private String callClaudeInternal(String systemPrompt, List<Map<String, Object>> messages, int maxTokens) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("system", systemPrompt);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", maxTokens);

        try {
            Map<String, Object> response = anthropicWebClient.post()
                    .uri("/messages")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response == null) {
                throw new AIServiceException("Empty response from Claude API");
            }
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            if (content == null || content.isEmpty()) {
                throw new AIServiceException("No content in Claude API response");
            }
            return content.get(0).get("text").toString();
        } catch (WebClientResponseException e) {
            log.error("Claude API HTTP error: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AIServiceException("Claude API request failed: " + e.getMessage(), e);
        }
    }

    private String buildExplanationPrompt(AssessRequest request, List<AppliedFactor> factors,
                                           BigDecimal riskScore, String riskTier) {
        StringBuilder sb = new StringBuilder();
        sb.append("Risk Assessment:\n");
        sb.append("- Risk Score: ").append(riskScore).append("/100\n");
        sb.append("- Risk Tier: ").append(riskTier).append("\n");
        sb.append("- Driver Age: ").append(request.getDriverAge()).append("\n");
        sb.append("- Years Licensed: ").append(request.getLicenseYears()).append("\n");
        sb.append("- Vehicle: ").append(request.getVehicleMake()).append(" ")
                .append(request.getVehicleModel()).append(" (").append(request.getVehicleYear()).append(")\n");
        sb.append("- Coverage Type: ").append(request.getCoverageType()).append("\n");
        sb.append("\nApplied Risk Factors:\n");
        for (AppliedFactor f : factors) {
            sb.append("- ").append(f.getLabel())
              .append(" (score impact: ").append(f.getScoreImpact()).append(")\n");
        }
        sb.append("\nPlease explain these risk factors to the customer in 2-3 sentences.");
        return sb.toString();
    }

    private String buildFallbackExplanation(List<AppliedFactor> factors, String riskTier) {
        if (factors.isEmpty()) {
            return "Your profile shows a low risk level with no significant risk factors identified. " +
                   "Your premium reflects standard rates for your vehicle and coverage selection.";
        }
        String primaryFactor = factors.get(0).getLabel();
        return String.format(
                "Your premium reflects a %s risk assessment based on your profile. " +
                "The most significant factor is: %s. " +
                "These factors are used by insurers to estimate the likelihood of future claims.",
                riskTier.toLowerCase().replace("_", " "), primaryFactor);
    }
}
