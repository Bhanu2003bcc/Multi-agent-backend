package com.research.backend.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.backend.domain.enums.JobStatus;
import com.research.backend.dto.response.ResearchResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j

public class JobEventPublisher {

    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    /**
     * Register an SSE emitter for a specific job.
     * The emitter is automatically cleaned up on complete/timeout/error.
     */
    public SseEmitter register(UUID jobId) {
        SseEmitter emitter = new SseEmitter(120_000L); // 2 min timeout

        emitter.onCompletion(() -> {
            emitters.remove(jobId);
            log.debug("SSE emitter completed for jobId={}", jobId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(jobId);
            log.debug("SSE emitter timed out for jobId={}", jobId);
        });
        emitter.onError(e -> {
            emitters.remove(jobId);
            log.debug("SSE emitter error for jobId={}: {}", jobId, e.getMessage());
        });

        emitters.put(jobId, emitter);
        log.debug("SSE emitter registered for jobId={}", jobId);
        return emitter;
    }

    /**
     * Publishes a job lifecycle event to the registered SSE stream.
     */
    public void publishStatusChange(UUID jobId, JobStatus status, ResearchResultResponse result) {
        SseEmitter emitter = emitters.get(jobId);
        if (emitter == null) {
            log.debug("No SSE emitter registered for jobId={}", jobId);
            return;
        }

        try {
            JobEvent event = new JobEvent(jobId, status, Instant.now(), result);
            String json = objectMapper.writeValueAsString(event);

            emitter.send(
                    SseEmitter.event()
                            .name(status.name().toLowerCase())
                            .data(json)
                            .id(UUID.randomUUID().toString())
            );

            if (status.isTerminal()) {
                emitter.complete();
                emitters.remove(jobId);
            }

        } catch (IOException e) {
            log.warn("Failed to send SSE event for jobId={}: {}", jobId, e.getMessage());
            emitters.remove(jobId);
        }
    }

    public int activeStreamCount() {
        return emitters.size();
    }

    // ─── Event payload ────────────────────────────────────────────────────

    public record JobEvent(
            UUID jobId,
            JobStatus status,
            Instant timestamp,
            ResearchResultResponse result
    ) {}
}
