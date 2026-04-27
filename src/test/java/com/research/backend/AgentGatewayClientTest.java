package com.research.backend.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AgentGatewayClientTest {

    private static MockWebServer mockWebServer;
    private AgentGatewayClient client;
    private ObjectMapper objectMapper;

    @BeforeAll
    static void startServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void stopServer() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();
        client = new AgentGatewayClient(webClient);
    }

    @Test
    @DisplayName("Should successfully call agent API and deserialize response")
    void shouldCallAgentAndDeserializeResponse() throws Exception {
        AgentApiContract.AgentResponse mockResponse = AgentApiContract.AgentResponse.builder()
                .answer("Quantum computing achieved 1000-qubit milestone in 2025.")
                .sources(List.of("https://nature.com/quantum", "https://ibm.com/quantum"))
                .confidence(0.91)
                .refinementIterationsRun(1)
                .elapsedSeconds(24.5)
                .pipelineErrors(List.of())
                .build();

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(mockResponse)));

        AgentApiContract.AgentRequest request = AgentApiContract.AgentRequest.builder()
                .query("Latest quantum computing breakthroughs 2025")
                .searchTopN(10)
                .rerankerTopK(5)
                .retrieverTopK(8)
                .refinementIterations(2)
                .build();

        // Note: In unit tests, we call the underlying WebClient directly
        // without Resilience4j annotations (those require Spring context)
        AgentApiContract.AgentResponse response = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build()
                .post()
                .uri("/research")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AgentApiContract.AgentResponse.class)
                .block();

        assertThat(response).isNotNull();
        assertThat(response.getAnswer()).contains("1000-qubit");
        assertThat(response.getSources()).hasSize(2);
        assertThat(response.getConfidence()).isEqualTo(0.91);

        RecordedRequest recorded = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getPath()).isEqualTo("/research");
        assertThat(recorded.getMethod()).isEqualTo("POST");
    }

    @Test
    @DisplayName("Should correctly serialize snake_case JSON fields to Python")
    void shouldSerializeRequestWithSnakeCaseFields() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"answer\":\"test\",\"sources\":[],\"confidence\":0.5," +
                         "\"refinement_iterations_run\":0,\"elapsed_seconds\":1.0," +
                         "\"pipeline_errors\":[]}"));

        AgentApiContract.AgentRequest request = AgentApiContract.AgentRequest.builder()
                .query("test query")
                .searchTopN(5)
                .rerankerTopK(3)
                .retrieverTopK(4)
                .refinementIterations(1)
                .build();

        WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build()
                .post()
                .uri("/research")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AgentApiContract.AgentResponse.class)
                .block();

        RecordedRequest recorded = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        String body = recorded.getBody().readUtf8();

        // Verify snake_case serialization matches Python contract
        assertThat(body).contains("\"search_top_n\":5");
        assertThat(body).contains("\"reranker_top_k\":3");
        assertThat(body).contains("\"retriever_top_k\":4");
        assertThat(body).contains("\"refinement_iterations\":1");
    }
}
