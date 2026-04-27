package com.research.backend.repository;

import com.research.backend.domain.entity.ResearchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResearchResultRepository extends JpaRepository<ResearchResult, UUID> {
    Optional<ResearchResult> findByJobId(UUID jobId);
}
