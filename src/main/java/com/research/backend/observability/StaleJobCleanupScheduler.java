package com.research.backend.observability;

import com.research.backend.domain.enums.JobStatus;
import com.research.backend.repository.ResearchJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class StaleJobCleanupScheduler {

    private final ResearchJobRepository jobRepository;

    /**
     * Mark jobs that have been IN_PROGRESS for more than 10 minutes as FAILED.
     * Runs every 5 minutes. Guards against stuck jobs if the app crashes mid-execution.
     */
    @Scheduled(fixedDelay = 300_000) // every 5 min
    @Transactional
    public void cleanupStaleJobs() {
        Instant cutoff = Instant.now().minus(10, ChronoUnit.MINUTES);
        var stale = jobRepository.findStaleActiveJobs(cutoff);

        if (stale.isEmpty()) return;

        log.warn("Found {} stale jobs — marking FAILED", stale.size());
        stale.forEach(job -> {
            job.markFailed("Job timed out — pipeline did not complete within 10 minutes");
            log.warn("Stale job marked FAILED: jobId={}", job.getId());
        });
        jobRepository.saveAll(stale);
    }
}
