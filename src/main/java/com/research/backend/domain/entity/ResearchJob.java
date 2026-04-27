package com.research.backend.domain.entity;

import com.research.backend.domain.enums.JobStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "research_jobs",
    indexes = {
        @Index(name = "idx_research_jobs_user_id", columnList = "user_id"),
        @Index(name = "idx_research_jobs_status", columnList = "status"),
        @Index(name = "idx_research_jobs_created_at", columnList = "created_at"),
        @Index(name = "idx_research_jobs_query_hash", columnList = "query_hash")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResearchJob extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "query", nullable = false, columnDefinition = "TEXT")
    private String query;

    @Column(name = "query_hash", nullable = false, length = 64)
    private String queryHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private JobStatus status = JobStatus.CREATED;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "elapsed_ms")
    private Long elapsedMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "search_top_n")
    @Builder.Default
    private Integer searchTopN = 10;

    @Column(name = "reranker_top_k")
    @Builder.Default
    private Integer rerankerTopK = 5;

    @Column(name = "refinement_iterations")
    @Builder.Default
    private Integer refinementIterations = 2;

    @OneToOne(mappedBy = "job", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ResearchResult result;

    public void markInProgress() {
        this.status = JobStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
    }

    public void markCompleted(long elapsedMs) {
        this.status = JobStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.elapsedMs = elapsedMs;
    }

    public void markFailed(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.completedAt = Instant.now();
        this.errorMessage = errorMessage;
    }
}
