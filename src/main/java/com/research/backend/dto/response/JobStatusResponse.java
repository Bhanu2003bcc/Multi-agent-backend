package com.research.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.research.backend.domain.enums.JobStatus;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobStatusResponse {

    UUID jobId;
    JobStatus status;
    String query;
    Instant createdAt;
    Instant startedAt;
    Instant completedAt;
    Long elapsedMs;
    String errorMessage;

    // Populated only when status == COMPLETED
    ResearchResultResponse result;
}
