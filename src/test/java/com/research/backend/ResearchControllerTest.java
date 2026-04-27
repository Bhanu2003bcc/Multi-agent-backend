package com.research.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.backend.async.JobEventPublisher;
import com.research.backend.controller.ResearchController;
import com.research.backend.domain.enums.JobStatus;
import com.research.backend.dto.request.ResearchRequest;
import com.research.backend.dto.response.JobSubmittedResponse;
import com.research.backend.security.JwtAuthenticationFilter;
import com.research.backend.service.ResearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = ResearchController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthenticationFilter.class
    )
)
class ResearchControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ResearchService researchService;
    @MockBean JobEventPublisher eventPublisher;

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("POST /v1/research should return 202 Accepted with jobId")
    void submitResearch_validRequest_returns202() throws Exception {
        UUID jobId = UUID.randomUUID();
        JobSubmittedResponse mockResponse = JobSubmittedResponse.builder()
                .jobId(jobId)
                .status(JobStatus.CREATED)
                .createdAt(Instant.now())
                .streamUrl("/api/v1/research/" + jobId + "/stream")
                .statusUrl("/api/v1/research/" + jobId)
                .build();

        when(researchService.submitJob(any(ResearchRequest.class), eq("testuser")))
                .thenReturn(mockResponse);

        ResearchRequest request = ResearchRequest.builder()
                .query("What are the latest quantum computing breakthroughs?")
                .searchTopN(10)
                .rerankerTopK(5)
                .retrieverTopK(8)
                .refinementIterations(2)
                .build();

        mockMvc.perform(post("/v1/research")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.streamUrl").exists())
                .andExpect(jsonPath("$.statusUrl").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("POST /v1/research should return 400 for blank query")
    void submitResearch_blankQuery_returns400() throws Exception {
        ResearchRequest badRequest = ResearchRequest.builder()
                .query("  ")
                .build();

        mockMvc.perform(post("/v1/research")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.violations.query").exists());
    }

    @Test
    @DisplayName("POST /v1/research should return 401 for unauthenticated request")
    void submitResearch_unauthenticated_returns401() throws Exception {
        ResearchRequest request = ResearchRequest.builder()
                .query("test query")
                .build();

        mockMvc.perform(post("/v1/research")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
