package com.research.backend.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgentGatewayClient {

    private final WebClient agentWebClient;

    /**
     * Calls the Python FastAPI /research endpoint asynchronously.
     * Wrapped with Circuit Breaker + Retry + TimeLimiter via Resilience4j.
     *
     * @param request the research request payload
     * @return CompletableFuture of the agent's response
     */
    @CircuitBreaker(name = "agentService", fallbackMethod = "agentServiceFallback")
    @Retry(name = "agentService")
    @TimeLimiter(name = "agentService")
    @Timed(value = "agent.gateway.call", description = "Time taken to call the Python agent service")
    public CompletableFuture<AgentApiContract.AgentResponse> research(
            AgentApiContract.AgentRequest request) {

        log.info("Calling Python agent service for query: [{}]",
                truncate(request.getQuery(), 80));

        return agentWebClient
                .post()
                .uri("/research")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new AgentClientException(
                                                "Agent service returned 4xx: " + body,
                                                clientResponse.statusCode().value()
                                        )
                                ))
                )
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new AgentClientException(
                                                "Agent service returned 5xx: " + body,
                                                clientResponse.statusCode().value()
                                        )
                                ))
                )
                .bodyToMono(AgentApiContract.AgentResponse.class)
                .doOnSuccess(response ->
                        log.info("Agent service responded in {}s, confidence={}",
                                response.getElapsedSeconds(), response.getConfidence()))
                .doOnError(error ->
                        log.error("Agent service call failed: {}", error.getMessage()))
                .toFuture();
    }

    /**
     * Resilience4j Circuit Breaker fallback.
     * Called when the circuit is open or all retries exhausted.
     */
    @SuppressWarnings("unused")
    public CompletableFuture<AgentApiContract.AgentResponse> agentServiceFallback(
            AgentApiContract.AgentRequest request, Throwable throwable) {

        log.error("Agent service fallback triggered for query=[{}], cause={}",
                truncate(request.getQuery(), 80), throwable.getMessage());

        return CompletableFuture.failedFuture(
                new AgentServiceUnavailableException(
                        "Research pipeline is temporarily unavailable. Please try again later.",
                        throwable
                )
        );
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
