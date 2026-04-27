package com.research.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@Schema(description = "Request body for submitting a research query")
public class ResearchRequest {

    @NotBlank(message = "Query must not be blank")
    @Size(min = 3, max = 500, message = "Query must be between 3 and 500 characters")
    @Schema(description = "The research question or topic", example = "Latest breakthroughs in quantum computing 2025")
    String query;

    @Min(value = 1, message = "searchTopN must be at least 1")
    @Max(value = 20, message = "searchTopN must be at most 20")
    @Builder.Default
    @Schema(description = "Number of Exa search results to fetch", defaultValue = "10")
    Integer searchTopN = 10;

    @Min(value = 1, message = "rerankerTopK must be at least 1")
    @Max(value = 10, message = "rerankerTopK must be at most 10")
    @Builder.Default
    @Schema(description = "Top-K results to keep after re-ranking", defaultValue = "5")
    Integer rerankerTopK = 5;

    @Min(value = 1, message = "retrieverTopK must be at least 1")
    @Max(value = 20, message = "retrieverTopK must be at most 20")
    @Builder.Default
    @Schema(description = "Top-K chunks to retrieve from FAISS", defaultValue = "8")
    Integer retrieverTopK = 8;

    @Min(value = 0, message = "refinementIterations must be at least 0")
    @Max(value = 3, message = "refinementIterations must be at most 3")
    @Builder.Default
    @Schema(description = "Maximum writer-critic refinement iterations", defaultValue = "2")
    Integer refinementIterations = 2;
}
