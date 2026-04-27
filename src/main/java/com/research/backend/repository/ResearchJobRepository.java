package com.research.backend.repository;

import com.research.backend.domain.entity.ResearchJob;
import com.research.backend.domain.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ResearchJobRepository extends JpaRepository<ResearchJob, UUID> {

    Page<ResearchJob> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    List<ResearchJob> findByStatusIn(List<JobStatus> statuses);

    @Query("SELECT j FROM ResearchJob j WHERE j.status IN ('CREATED', 'IN_PROGRESS') " +
           "AND j.createdAt < :cutoff")
    List<ResearchJob> findStaleActiveJobs(Instant cutoff);

    long countByUserIdAndCreatedAtAfter(String userId, Instant since);
}
