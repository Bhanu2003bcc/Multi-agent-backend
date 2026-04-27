package com.research.backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "research_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResearchResult extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private ResearchJob job;

    @Column(name = "answer", nullable = false, columnDefinition = "TEXT")
    private String answer;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sources", columnDefinition = "jsonb")
    private List<String> sources;

    @Column(name = "confidence")
    private Double confidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "critic_feedback", columnDefinition = "jsonb")
    private CriticFeedbackSnapshot criticFeedback;

    @Column(name = "refinement_iterations_run")
    private Integer refinementIterationsRun;

    @Column(name = "elapsed_seconds")
    private Double elapsedSeconds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pipeline_errors", columnDefinition = "jsonb")
    private List<String> pipelineErrors;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CriticFeedbackSnapshot {
        private Double factualCorrectnessScore;
        private Double completenessScore;
        private Double hallucinationRisk;
        private List<String> missingInformation;
        private List<String> improvementSuggestions;
        private Double overallQuality;
    }
}
