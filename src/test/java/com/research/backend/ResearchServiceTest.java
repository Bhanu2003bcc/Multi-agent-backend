package com.research.backend;

import com.research.backend.async.JobEventPublisher;
import com.research.backend.client.AgentApiContract;
import com.research.backend.client.AgentGatewayClient;
import com.research.backend.domain.entity.ResearchJob;
import com.research.backend.domain.enums.JobStatus;
import com.research.backend.dto.request.ResearchRequest;
import com.research.backend.dto.response.JobSubmittedResponse;
import com.research.backend.repository.ResearchJobRepository;
import com.research.backend.repository.ResearchResultRepository;
import com.research.backend.service.CacheService;
import com.research.backend.service.ResearchMapper;
import com.research.backend.service.ResearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResearchServiceTest {

        @Mock
        ResearchJobRepository jobRepository;
        @Mock
        ResearchResultRepository resultRepository;
        @Mock
        AgentGatewayClient agentClient;
        @Mock
        CacheService cacheService;
        @Mock
        JobEventPublisher eventPublisher;
        @Mock
        ResearchMapper mapper;

        @InjectMocks
        ResearchService researchService;

        private ResearchRequest validRequest;
        private ResearchJob savedJob;

        @BeforeEach
        void setUp() {
                validRequest = ResearchRequest.builder()
                                .query("What are the latest AI breakthroughs in 2025?")
                                .searchTopN(10)
                                .rerankerTopK(5)
                                .retrieverTopK(8)
                                .refinementIterations(2)
                                .build();

                savedJob = ResearchJob.builder()
                                .userId("testuser")
                                .query(validRequest.getQuery())
                                .queryHash("abc123")
                                .status(JobStatus.CREATED)
                                .build();
                savedJob.setId(UUID.randomUUID()); // ID is in BaseEntity, set it manually
        }

        @Test
        @DisplayName("submitJob should return ACCEPTED response with jobId when cache misses")
        void submitJob_cacheMiss_returnsJobSubmittedResponse() {
                when(cacheService.getCachedResult(anyString())).thenReturn(null);
                when(jobRepository.save(any(ResearchJob.class))).thenReturn(savedJob);
                when(jobRepository.findById(savedJob.getId())).thenReturn(Optional.of(savedJob));
                when(agentClient.research(any())).thenReturn(CompletableFuture.completedFuture(
                                AgentApiContract.AgentResponse.builder().build()));

                // We need to stub executeJobAsync indirectly since it's @Async
                // In a real unit test (MockitoExtension), @Async is ignored and it runs
                // synchronously
                JobSubmittedResponse response = researchService.submitJob(validRequest, "testuser");

                assertThat(response).isNotNull();
                verify(jobRepository, atLeastOnce()).save(any(ResearchJob.class));
                verify(cacheService, times(1)).getCachedResult(anyString());
        }

        @Test
        @DisplayName("submitJob should use cached result when cache hits")
        void submitJob_cacheHit_doesNotCallAgentClient() {
                com.research.backend.dto.response.ResearchResultResponse cachedResult = com.research.backend.dto.response.ResearchResultResponse
                                .builder()
                                .answer("Cached answer about AI breakthroughs")
                                .sources(List.of("https://example.com"))
                                .confidence(0.88)
                                .build();

                when(cacheService.getCachedResult(anyString())).thenReturn(cachedResult);
                when(jobRepository.save(any(ResearchJob.class))).thenAnswer(i -> i.getArgument(0));

                researchService.submitJob(validRequest, "testuser");

                // Agent client should NEVER be called on a cache hit
                verifyNoInteractions(agentClient);
                verify(cacheService, times(1)).getCachedResult(anyString());
        }

        @Test
        @DisplayName("getJobStatus should throw JobNotFoundException for unknown jobId")
        void getJobStatus_unknownJobId_throwsException() {
                UUID unknownId = UUID.randomUUID();
                when(jobRepository.findById(unknownId)).thenReturn(Optional.empty());

                org.junit.jupiter.api.Assertions.assertThrows(
                                com.research.backend.exception.JobNotFoundException.class,
                                () -> researchService.getJobStatus(unknownId, "testuser"));
        }

        @Test
        @DisplayName("executeJobAsync should mark job COMPLETED on successful agent call")
        void executeJobAsync_success_marksJobCompleted() throws Exception {
                AgentApiContract.AgentResponse agentResponse = AgentApiContract.AgentResponse.builder()
                                .answer("AI made tremendous progress in 2025.")
                                .sources(List.of("https://science.org/ai2025"))
                                .confidence(0.93)
                                .refinementIterationsRun(1)
                                .elapsedSeconds(22.3)
                                .pipelineErrors(List.of())
                                .build();

                when(jobRepository.findById(any())).thenReturn(Optional.of(savedJob));
                when(agentClient.research(any())).thenReturn(CompletableFuture.completedFuture(agentResponse));
                when(resultRepository.save(any())).thenAnswer(i -> i.getArgument(0));
                when(jobRepository.save(any())).thenAnswer(i -> i.getArgument(0));
                when(mapper.toResultResponse(any(AgentApiContract.AgentResponse.class)))
                                .thenReturn(com.research.backend.dto.response.ResearchResultResponse.builder()
                                                .answer(agentResponse.getAnswer())
                                                .sources(agentResponse.getSources())
                                                .confidence(agentResponse.getConfidence())
                                                .build());

                researchService.executeJobAsync(UUID.randomUUID(), validRequest).get();

                verify(jobRepository, atLeast(2)).save(any(ResearchJob.class));
                verify(cacheService, times(1)).cacheResult(anyString(), any());
                verify(eventPublisher, times(1))
                                .publishStatusChange(any(), eq(JobStatus.COMPLETED), any());
        }
}
