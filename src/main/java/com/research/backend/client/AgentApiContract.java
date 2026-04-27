package com.research.backend.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * DTOs that mirror the Python FastAPI /research endpoint contract exactly.
 */
public final class AgentApiContract {

    private AgentApiContract() {}

    // ── Outbound request to Python ─────────────────────────────────────────
    @Value
    @Builder
    @Jacksonized
    public static class AgentRequest {
        String query;

        @JsonProperty("search_top_n")
        Integer searchTopN;

        @JsonProperty("reranker_top_k")
        Integer rerankerTopK;

        @JsonProperty("retriever_top_k")
        Integer retrieverTopK;

        @JsonProperty("refinement_iterations")
        Integer refinementIterations;
    }

    // ── Inbound response from Python ───────────────────────────────────────
    @Value
    @Builder
    @Jacksonized
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AgentResponse {
        String answer;
        List<String> sources;
        Double confidence;

        @JsonProperty("critic_feedback")
        CriticFeedback criticFeedback;

        @JsonProperty("refinement_iterations_run")
        Integer refinementIterationsRun;

        @JsonProperty("elapsed_seconds")
        Double elapsedSeconds;

        @JsonProperty("pipeline_errors")
        List<String> pipelineErrors;
    }

    @Value
    @Builder
    @Jacksonized
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CriticFeedback {

        @JsonProperty("factual_correctness_score")
        Double factualCorrectnessScore;

        @JsonProperty("completeness_score")
        Double completenessScore;

        @JsonProperty("hallucination_risk")
        Double hallucinationRisk;

        @JsonProperty("missing_information")
        List<String> missingInformation;

        @JsonProperty("improvement_suggestions")
        List<String> improvementSuggestions;

        @JsonProperty("overall_quality")
        Double overallQuality;
    }
}
