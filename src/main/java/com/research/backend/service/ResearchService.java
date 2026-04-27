package com.research.backend.service;

import com.research.backend.async.JobEventPublisher;
import com.research.backend.client.AgentApiContract;
import com.research.backend.client.AgentGatewayClient;
import com.research.backend.client.AgentServiceUnavailableException;
import com.research.backend.domain.entity.ResearchJob;
import com.research.backend.domain.entity.ResearchResult;
import com.research.backend.domain.enums.JobStatus;
import com.research.backend.dto.request.ResearchRequest;
import com.research.backend.dto.response.JobStatusResponse;
import com.research.backend.dto.response.JobSubmittedResponse;
import com.research.backend.dto.response.ResearchResultResponse;
import com.research.backend.exception.JobNotFoundException;
import com.research.backend.exception.ResourceAccessDeniedException;
import com.research.backend.repository.ResearchJobRepository;
import com.research.backend.repository.ResearchResultRepository;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResearchService {

    private final ResearchJobRepository jobRepository;
    private final ResearchResultRepository resultRepository;
    private final AgentGatewayClient agentClient;
    private final CacheService cacheService;
    private final JobEventPublisher eventPublisher;
    private final ResearchMapper mapper;

    /**
     * Submits a new research job.
     * Returns immediately with a jobId — processing is fully asynchronous.
     */
    @Transactional
    @Timed(value = "research.submit", description = "Time to submit a research job")
    @Counted(value = "research.submit.count")
    public JobSubmittedResponse submitJob(ResearchRequest request, String userId) {
        String queryHash = sha256(request.getQuery().trim().toLowerCase());

        // Check Redis cache first — same query answered before? Return instantly.
        ResearchResultResponse cached = cacheService.getCachedResult(queryHash);
        if (cached != null) {
            log.info("Cache HIT for query hash={}", queryHash);
            ResearchJob cachedJob = createCompletedJobRecord(request, userId, queryHash);
            ResearchResult cachedResult = buildResultEntity(cachedJob, cached);
            cachedJob.setResult(cachedResult);
            jobRepository.save(cachedJob);
            return buildSubmittedResponse(cachedJob);
        }

        // No cache — create a new job record and dispatch async processing
        ResearchJob job = ResearchJob.builder()
                .userId(userId)
                .query(request.getQuery())
                .queryHash(queryHash)
                .status(JobStatus.CREATED)
                .searchTopN(request.getSearchTopN())
                .rerankerTopK(request.getRerankerTopK())
                .refinementIterations(request.getRefinementIterations())
                .build();

        job = jobRepository.save(job);
        log.info("Research job created: jobId={}, userId={}", job.getId(), userId);

        // Fire-and-forget async execution
        executeJobAsync(job.getId(), request);

        return buildSubmittedResponse(job);
    }

    /**
     * Async pipeline execution — runs in the research-async thread pool.
     * Never throws — all exceptions are caught and recorded on the job entity.
     */
    @Async("researchTaskExecutor")
    public CompletableFuture<Void> executeJobAsync(UUID jobId, ResearchRequest request) {
        long startMs = System.currentTimeMillis();

        ResearchJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        try {
            // Transition to IN_PROGRESS
            job.markInProgress();
            jobRepository.save(job);
            eventPublisher.publishStatusChange(jobId, JobStatus.IN_PROGRESS, null);

            log.info("Pipeline started: jobId={}", jobId);

            // Build the request for Python agent
            AgentApiContract.AgentRequest agentRequest = AgentApiContract.AgentRequest.builder()
                    .query(request.getQuery())
                    .searchTopN(request.getSearchTopN())
                    .rerankerTopK(request.getRerankerTopK())
                    .retrieverTopK(request.getRetrieverTopK())
                    .refinementIterations(request.getRefinementIterations())
                    .build();

            // Call Python — this blocks the async thread (by design)
            AgentApiContract.AgentResponse agentResponse =
                    agentClient.research(agentRequest).get();

            long elapsedMs = System.currentTimeMillis() - startMs;

            // Persist result
            ResearchResultResponse resultDto = mapper.toResultResponse(agentResponse);
            ResearchResult result = buildResultEntity(job, resultDto);
            result = resultRepository.save(result);

            job.setResult(result);
            job.markCompleted(elapsedMs);
            jobRepository.save(job);

            // Cache the result under the query hash
            cacheService.cacheResult(job.getQueryHash(), resultDto);

            log.info("Pipeline completed: jobId={}, elapsedMs={}, confidence={}",
                    jobId, elapsedMs, agentResponse.getConfidence());

            eventPublisher.publishStatusChange(jobId, JobStatus.COMPLETED, resultDto);

        } catch (AgentServiceUnavailableException e) {
            handleJobFailure(job, "Agent service unavailable: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handleJobFailure(job, "Job interrupted");
        } catch (Exception e) {
            log.error("Unexpected error executing job={}: {}", jobId, e.getMessage(), e);
            handleJobFailure(job, "Internal error: " + e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    @Transactional(readOnly = true)
    public JobStatusResponse getJobStatus(UUID jobId, String userId) {
        ResearchJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        if (!job.getUserId().equals(userId)) {
            throw new ResourceAccessDeniedException("Access denied to job " + jobId);
        }

        return mapper.toJobStatusResponse(job);
    }

    @Transactional(readOnly = true)
    public Page<JobStatusResponse> getJobHistory(String userId, Pageable pageable) {
        return jobRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(mapper::toJobStatusResponse);
    }

    @Transactional
    public void cancelJob(UUID jobId, String userId) {
        ResearchJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        if (!job.getUserId().equals(userId)) {
            throw new ResourceAccessDeniedException("Access denied to job " + jobId);
        }

        if (job.getStatus().isTerminal()) {
            log.warn("Cannot cancel terminal job={}", jobId);
            return;
        }

        job.setStatus(JobStatus.CANCELLED);
        jobRepository.save(job);
        eventPublisher.publishStatusChange(jobId, JobStatus.CANCELLED, null);
        log.info("Job cancelled: jobId={}", jobId);
    }

    // ─── Private helpers ──────────────────────────────────────────────────

    private void handleJobFailure(ResearchJob job, String errorMessage) {
        log.error("Job failed: jobId={}, reason={}", job.getId(), errorMessage);
        job.markFailed(errorMessage);
        jobRepository.save(job);
        eventPublisher.publishStatusChange(job.getId(), JobStatus.FAILED, null);
    }

    private JobSubmittedResponse buildSubmittedResponse(ResearchJob job) {
        return JobSubmittedResponse.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .createdAt(job.getCreatedAt())
                .streamUrl("/api/v1/research/" + job.getId() + "/stream")
                .statusUrl("/api/v1/research/" + job.getId())
                .build();
    }

    private ResearchJob createCompletedJobRecord(
            ResearchRequest request, String userId, String queryHash) {
        ResearchJob job = ResearchJob.builder()
                .userId(userId)
                .query(request.getQuery())
                .queryHash(queryHash)
                .status(JobStatus.COMPLETED)
                .searchTopN(request.getSearchTopN())
                .rerankerTopK(request.getRerankerTopK())
                .refinementIterations(request.getRefinementIterations())
                .build();
        job.markCompleted(0L);
        return job;
    }

    private ResearchResult buildResultEntity(ResearchJob job, ResearchResultResponse dto) {
        ResearchResult.CriticFeedbackSnapshot snapshot = null;
        if (dto.getCriticFeedback() != null) {
            var cf = dto.getCriticFeedback();
            snapshot = ResearchResult.CriticFeedbackSnapshot.builder()
                    .factualCorrectnessScore(cf.getFactualCorrectnessScore())
                    .completenessScore(cf.getCompletenessScore())
                    .hallucinationRisk(cf.getHallucinationRisk())
                    .missingInformation(cf.getMissingInformation())
                    .improvementSuggestions(cf.getImprovementSuggestions())
                    .overallQuality(cf.getOverallQuality())
                    .build();
        }

        return ResearchResult.builder()
                .job(job)
                .answer(dto.getAnswer())
                .sources(dto.getSources())
                .confidence(dto.getConfidence())
                .criticFeedback(snapshot)
                .refinementIterationsRun(dto.getRefinementIterationsRun())
                .elapsedSeconds(dto.getElapsedSeconds())
                .pipelineErrors(dto.getPipelineErrors())
                .build();
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
