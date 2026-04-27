package com.research.backend.controller;

import com.research.backend.async.JobEventPublisher;
import com.research.backend.dto.request.ResearchRequest;
import com.research.backend.dto.response.JobStatusResponse;
import com.research.backend.dto.response.JobSubmittedResponse;
import com.research.backend.observability.RateLimitFilter;
import com.research.backend.service.ResearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/v1/research")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Research", description = "Multi-agent research pipeline endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ResearchController {

    private final ResearchService researchService;
    private final JobEventPublisher eventPublisher;

    /**
     * Submit a new research job.
     * Returns 202 Accepted with jobId for polling / streaming.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
        summary = "Submit a research query",
        description = "Starts the multi-agent research pipeline asynchronously. " +
                      "Use the returned jobId to poll for status or subscribe to the SSE stream."
    )
    public ResponseEntity<JobSubmittedResponse> submitResearch(
            @Valid @RequestBody ResearchRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Research request from user={}, query=[{}]",
                userDetails.getUsername(), truncate(request.getQuery(), 80));

        JobSubmittedResponse response = researchService.submitJob(request, userDetails.getUsername());
        return ResponseEntity.accepted().body(response);
    }

    /**
     * Get the current status (and result if complete) of a research job.
     */
    @GetMapping("/{jobId}")
    @Operation(
        summary = "Get job status and result",
        description = "Returns current status. When status=COMPLETED the full research result is included."
    )
    public ResponseEntity<JobStatusResponse> getJobStatus(
            @Parameter(description = "Job ID returned by POST /research")
            @PathVariable UUID jobId,
            @AuthenticationPrincipal UserDetails userDetails) {

        JobStatusResponse status = researchService.getJobStatus(jobId, userDetails.getUsername());
        return ResponseEntity.ok(status);
    }

    /**
     * SSE stream endpoint — subscribe for live job progress.
     * Angular can use EventSource API against this URL.
     */
    @GetMapping(value = "/{jobId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
        summary = "Subscribe to job progress via SSE",
        description = "Streams status events: in_progress → completed (with result) or failed."
    )
    public SseEmitter streamJobStatus(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Validate access first
        researchService.getJobStatus(jobId, userDetails.getUsername());
        log.debug("SSE subscription for jobId={}", jobId);
        return eventPublisher.register(jobId);
    }

    /**
     * Paginated history of all research jobs for the authenticated user.
     */
    @GetMapping("/history")
    @Operation(summary = "Get research job history")
    public ResponseEntity<Page<JobStatusResponse>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<JobStatusResponse> history = researchService.getJobHistory(
                userDetails.getUsername(), pageable);
        return ResponseEntity.ok(history);
    }

    /**
     * Cancel an in-progress research job.
     */
    @DeleteMapping("/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel a research job")
    public ResponseEntity<Void> cancelJob(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal UserDetails userDetails) {

        researchService.cancelJob(jobId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
