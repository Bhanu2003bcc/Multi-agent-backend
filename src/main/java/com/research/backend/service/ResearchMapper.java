package com.research.backend.service;

import com.research.backend.client.AgentApiContract;
import com.research.backend.domain.entity.ResearchJob;
import com.research.backend.domain.entity.ResearchResult;
import com.research.backend.dto.response.JobStatusResponse;
import com.research.backend.dto.response.ResearchResultResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ResearchMapper {

    // AgentResponse → ResearchResultResponse
    @Mapping(source = "criticFeedback", target = "criticFeedback")
    ResearchResultResponse toResultResponse(AgentApiContract.AgentResponse agentResponse);

    ResearchResultResponse.CriticFeedbackResponse toCriticFeedbackResponse(
            AgentApiContract.CriticFeedback criticFeedback);

    // ResearchJob → JobStatusResponse
    @Mapping(source = "id", target = "jobId")
    @Mapping(source = "result", target = "result")
    JobStatusResponse toJobStatusResponse(ResearchJob job);

    // ResearchResult → ResearchResultResponse
    @Mapping(source = "criticFeedback.factualCorrectnessScore",
             target = "criticFeedback.factualCorrectnessScore")
    @Mapping(source = "criticFeedback.completenessScore",
             target = "criticFeedback.completenessScore")
    @Mapping(source = "criticFeedback.hallucinationRisk",
             target = "criticFeedback.hallucinationRisk")
    @Mapping(source = "criticFeedback.missingInformation",
             target = "criticFeedback.missingInformation")
    @Mapping(source = "criticFeedback.improvementSuggestions",
             target = "criticFeedback.improvementSuggestions")
    @Mapping(source = "criticFeedback.overallQuality",
             target = "criticFeedback.overallQuality")
    ResearchResultResponse toResultResponse(ResearchResult result);
}
