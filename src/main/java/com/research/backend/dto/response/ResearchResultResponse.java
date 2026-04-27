package com.research.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResearchResultResponse {

    String answer;
    List<String> sources;
    Double confidence;
    CriticFeedbackResponse criticFeedback;
    Integer refinementIterationsRun;
    Double elapsedSeconds;
    List<String> pipelineErrors;

    @Value
    @Builder
    @Jacksonized
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CriticFeedbackResponse {
        Double factualCorrectnessScore;
        Double completenessScore;
        Double hallucinationRisk;
        List<String> missingInformation;
        List<String> improvementSuggestions;
        Double overallQuality;
    }
}
