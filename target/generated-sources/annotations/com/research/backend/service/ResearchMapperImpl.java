package com.research.backend.service;

import com.research.backend.client.AgentApiContract;
import com.research.backend.domain.entity.ResearchJob;
import com.research.backend.domain.entity.ResearchResult;
import com.research.backend.dto.response.JobStatusResponse;
import com.research.backend.dto.response.ResearchResultResponse;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-29T09:37:42+0530",
    comments = "version: 1.6.2, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class ResearchMapperImpl implements ResearchMapper {

    @Override
    public ResearchResultResponse toResultResponse(AgentApiContract.AgentResponse agentResponse) {
        if ( agentResponse == null ) {
            return null;
        }

        ResearchResultResponse.ResearchResultResponseBuilder researchResultResponse = ResearchResultResponse.builder();

        researchResultResponse.criticFeedback( toCriticFeedbackResponse( agentResponse.getCriticFeedback() ) );
        researchResultResponse.answer( agentResponse.getAnswer() );
        researchResultResponse.confidence( agentResponse.getConfidence() );
        researchResultResponse.elapsedSeconds( agentResponse.getElapsedSeconds() );
        List<String> list = agentResponse.getPipelineErrors();
        if ( list != null ) {
            researchResultResponse.pipelineErrors( new ArrayList<String>( list ) );
        }
        researchResultResponse.refinementIterationsRun( agentResponse.getRefinementIterationsRun() );
        List<String> list1 = agentResponse.getSources();
        if ( list1 != null ) {
            researchResultResponse.sources( new ArrayList<String>( list1 ) );
        }

        return researchResultResponse.build();
    }

    @Override
    public ResearchResultResponse.CriticFeedbackResponse toCriticFeedbackResponse(AgentApiContract.CriticFeedback criticFeedback) {
        if ( criticFeedback == null ) {
            return null;
        }

        ResearchResultResponse.CriticFeedbackResponse.CriticFeedbackResponseBuilder criticFeedbackResponse = ResearchResultResponse.CriticFeedbackResponse.builder();

        criticFeedbackResponse.completenessScore( criticFeedback.getCompletenessScore() );
        criticFeedbackResponse.factualCorrectnessScore( criticFeedback.getFactualCorrectnessScore() );
        criticFeedbackResponse.hallucinationRisk( criticFeedback.getHallucinationRisk() );
        List<String> list = criticFeedback.getImprovementSuggestions();
        if ( list != null ) {
            criticFeedbackResponse.improvementSuggestions( new ArrayList<String>( list ) );
        }
        List<String> list1 = criticFeedback.getMissingInformation();
        if ( list1 != null ) {
            criticFeedbackResponse.missingInformation( new ArrayList<String>( list1 ) );
        }
        criticFeedbackResponse.overallQuality( criticFeedback.getOverallQuality() );

        return criticFeedbackResponse.build();
    }

    @Override
    public JobStatusResponse toJobStatusResponse(ResearchJob job) {
        if ( job == null ) {
            return null;
        }

        JobStatusResponse.JobStatusResponseBuilder jobStatusResponse = JobStatusResponse.builder();

        jobStatusResponse.jobId( job.getId() );
        jobStatusResponse.result( toResultResponse( job.getResult() ) );
        jobStatusResponse.completedAt( job.getCompletedAt() );
        jobStatusResponse.createdAt( job.getCreatedAt() );
        jobStatusResponse.elapsedMs( job.getElapsedMs() );
        jobStatusResponse.errorMessage( job.getErrorMessage() );
        jobStatusResponse.query( job.getQuery() );
        jobStatusResponse.startedAt( job.getStartedAt() );
        jobStatusResponse.status( job.getStatus() );

        return jobStatusResponse.build();
    }

    @Override
    public ResearchResultResponse toResultResponse(ResearchResult result) {
        if ( result == null ) {
            return null;
        }

        ResearchResultResponse.ResearchResultResponseBuilder researchResultResponse = ResearchResultResponse.builder();

        researchResultResponse.criticFeedback( criticFeedbackSnapshotToCriticFeedbackResponse( result.getCriticFeedback() ) );
        researchResultResponse.answer( result.getAnswer() );
        researchResultResponse.confidence( result.getConfidence() );
        researchResultResponse.elapsedSeconds( result.getElapsedSeconds() );
        List<String> list = result.getPipelineErrors();
        if ( list != null ) {
            researchResultResponse.pipelineErrors( new ArrayList<String>( list ) );
        }
        researchResultResponse.refinementIterationsRun( result.getRefinementIterationsRun() );
        List<String> list1 = result.getSources();
        if ( list1 != null ) {
            researchResultResponse.sources( new ArrayList<String>( list1 ) );
        }

        return researchResultResponse.build();
    }

    protected ResearchResultResponse.CriticFeedbackResponse criticFeedbackSnapshotToCriticFeedbackResponse(ResearchResult.CriticFeedbackSnapshot criticFeedbackSnapshot) {
        if ( criticFeedbackSnapshot == null ) {
            return null;
        }

        ResearchResultResponse.CriticFeedbackResponse.CriticFeedbackResponseBuilder criticFeedbackResponse = ResearchResultResponse.CriticFeedbackResponse.builder();

        criticFeedbackResponse.factualCorrectnessScore( criticFeedbackSnapshot.getFactualCorrectnessScore() );
        criticFeedbackResponse.completenessScore( criticFeedbackSnapshot.getCompletenessScore() );
        criticFeedbackResponse.hallucinationRisk( criticFeedbackSnapshot.getHallucinationRisk() );
        List<String> list = criticFeedbackSnapshot.getMissingInformation();
        if ( list != null ) {
            criticFeedbackResponse.missingInformation( new ArrayList<String>( list ) );
        }
        List<String> list1 = criticFeedbackSnapshot.getImprovementSuggestions();
        if ( list1 != null ) {
            criticFeedbackResponse.improvementSuggestions( new ArrayList<String>( list1 ) );
        }
        criticFeedbackResponse.overallQuality( criticFeedbackSnapshot.getOverallQuality() );

        return criticFeedbackResponse.build();
    }
}
