package com.research.backend.observability;

import com.research.backend.async.JobEventPublisher;
import com.research.backend.domain.enums.JobStatus;
import com.research.backend.repository.ResearchJobRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PipelineMetrics {

    private final MeterRegistry meterRegistry;
    private final ResearchJobRepository jobRepository;
    private final JobEventPublisher eventPublisher;

    @PostConstruct
    public void registerGauges() {

        Gauge.builder("research.jobs.active",
                        jobRepository,
                        repo -> repo.findByStatusIn(
                                List.of(JobStatus.CREATED, JobStatus.IN_PROGRESS)).size())
                .description("Number of currently active research jobs")
                .register(meterRegistry);

        Gauge.builder("research.jobs.total",
                        jobRepository,
                        repo -> (double) repo.count())
                .description("Total number of research jobs ever submitted")
                .register(meterRegistry);

        Gauge.builder("research.sse.active_streams",
                        eventPublisher,
                        JobEventPublisher::activeStreamCount)
                .description("Number of active SSE client connections")
                .register(meterRegistry);
    }
}
