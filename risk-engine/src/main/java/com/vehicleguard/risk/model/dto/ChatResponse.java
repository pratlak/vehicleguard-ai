package com.vehicleguard.risk.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String reply;
    private String sessionId;
    private List<String> suggestedActions;
    private String detectedAction;
    private AssessRequest quoteData;
}
