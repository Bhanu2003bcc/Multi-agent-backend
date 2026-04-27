package com.research.backend.controller;

import com.research.backend.async.JobEventPublisher;
import com.research.backend.domain.enums.JobStatus;
import com.research.backend.repository.ResearchJobRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin and operational endpoints")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ResearchJobRepository jobRepository;
    private final JobEventPublisher eventPublisher;

    @GetMapping("/stats")
    @Operation(summary = "Get pipeline runtime statistics")
    public ResponseEntity<Map<String, Object>> getStats() {
        long totalJobs = jobRepository.count();
        long activeJobs = jobRepository.findByStatusIn(
                List.of(JobStatus.CREATED, JobStatus.IN_PROGRESS)).size();
        int activeSseStreams = eventPublisher.activeStreamCount();

        return ResponseEntity.ok(Map.of(
                "totalJobs", totalJobs,
                "activeJobs", activeJobs,
                "activeSseStreams", activeSseStreams,
                "timestamp", Instant.now().toString()
        ));
    }
}
