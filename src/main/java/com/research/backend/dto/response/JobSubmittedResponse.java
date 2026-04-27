package com.research.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.research.backend.domain.enums.JobStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Response returned when a research job is submitted")
public class JobSubmittedResponse {

    @Schema(description = "Unique job identifier for polling")
    UUID jobId;

    @Schema(description = "Current job status")
    JobStatus status;

    @Schema(description = "ISO-8601 timestamp when job was created")
    Instant createdAt;

    @Schema(description = "SSE stream URL for live progress updates")
    String streamUrl;

    @Schema(description = "Poll URL for job status")
    String statusUrl;
}
