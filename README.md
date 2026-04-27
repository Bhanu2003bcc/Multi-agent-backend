# Multi-Agent Research System — Spring Boot Backend

Production-grade Spring Boot 3.3 backend that acts as the intelligent orchestration
layer between the Angular frontend and the Python FastAPI multi-agent pipeline.

---

## Architecture

```
Angular (SPA)
     │  HTTP/REST + JWT
     ▼
┌──────────────────────────────────────────────────────┐
│              Spring Boot Backend  :8080               │
│                                                      │
│  REST Controllers  →  Security Filter Chain          │
│       ↓                                              │
│  ResearchService  ←→  Redis Cache (30 min TTL)       │
│       ↓                                              │
│  AgentGatewayClient  (WebClient + Resilience4j)      │
│       ↓           ↑                                  │
│    POST /research  │  JSON response                  │
│       ↓           │                                  │
│  Async Job Manager (CompletableFuture + SSE)         │
│       ↓                                              │
│  JPA Persistence  →  PostgreSQL                      │
│  Micrometer       →  Prometheus / Grafana            │
└──────────────────────────────────────────────────────┘
     │  HTTP POST /research
     ▼
Python FastAPI :8000  (Multi-Agent Pipeline)
```

---

## Project Structure

```
src/main/java/com/research/backend/
├── ResearchBackendApplication.java   Main entry point
│
├── controller/
│   ├── ResearchController.java       POST /v1/research, GET status, SSE stream
│   ├── AuthController.java           POST /v1/auth/login, /refresh
│   └── AdminController.java          GET /v1/admin/stats  [ADMIN only]
│
├── service/
│   ├── ResearchService.java          Core orchestration logic
│   ├── CacheService.java             Redis cache wrapper
│   └── ResearchMapper.java           MapStruct DTO mapper
│
├── client/
│   ├── AgentGatewayClient.java       WebClient bridge to Python
│   ├── AgentApiContract.java         Request/Response DTOs (snake_case)
│   ├── AgentClientException.java
│   └── AgentServiceUnavailableException.java
│
├── async/
│   └── JobEventPublisher.java        SSE emitter registry + broadcaster
│
├── domain/
│   ├── entity/
│   │   ├── BaseEntity.java           UUID PK + audit timestamps
│   │   ├── ResearchJob.java          Job lifecycle entity
│   │   ├── ResearchResult.java       Stored pipeline output
│   │   └── AuditLog.java             Per-request audit trail
│   └── enums/
│       └── JobStatus.java            CREATED → IN_PROGRESS → COMPLETED/FAILED
│
├── repository/
│   ├── ResearchJobRepository.java
│   ├── ResearchResultRepository.java
│   └── AuditLogRepository.java
│
├── security/
│   ├── JwtService.java               Token generation + validation (jjwt)
│   ├── JwtAuthenticationFilter.java  Per-request JWT check + MDC injection
│   └── AppUserDetailsService.java    UserDetailsService (replace with DB in prod)
│
├── config/
│   ├── SecurityConfig.java           Spring Security 6 filter chain
│   ├── WebClientConfig.java          Netty pool + timeout config for WebClient
│   ├── AsyncConfig.java              research-pipeline- thread pool
│   ├── RedisConfig.java              Jackson JSON Redis serializer
│   ├── AppConfig.java                JPA auditing + OpenAPI bean
│   └── WebMvcConfig.java             Interceptor registration
│
├── observability/
│   ├── RateLimitFilter.java          Bucket4j per-IP rate limiting
│   ├── RequestLoggingInterceptor.java Latency + user logging on every request
│   ├── PipelineMetrics.java          Micrometer gauges
│   └── StaleJobCleanupScheduler.java Marks stuck IN_PROGRESS jobs as FAILED
│
├── exception/
│   ├── GlobalExceptionHandler.java   RFC 9457 ProblemDetail responses
│   ├── JobNotFoundException.java
│   └── ResourceAccessDeniedException.java
│
└── dto/
    ├── request/
    │   ├── ResearchRequest.java
    │   └── LoginRequest.java
    └── response/
        ├── JobSubmittedResponse.java
        ├── JobStatusResponse.java
        ├── ResearchResultResponse.java
        └── AuthResponse.java
```

---

## REST API Reference

### Authentication

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/auth/login` | Login → receive JWT access + refresh tokens |
| POST | `/api/v1/auth/refresh` | Refresh access token |

### Research

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/research` | ✅ | Submit research job → returns `jobId` (202) |
| GET | `/api/v1/research/{jobId}` | ✅ | Poll job status + result |
| GET | `/api/v1/research/{jobId}/stream` | ✅ | SSE stream of live events |
| GET | `/api/v1/research/history` | ✅ | Paginated job history |
| DELETE | `/api/v1/research/{jobId}` | ✅ | Cancel a job |

### Admin

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/admin/stats` | ADMIN | Active jobs, SSE connections, totals |

### Observability

| Path | Description |
|------|-------------|
| `/api/actuator/health` | Liveness + readiness probes |
| `/api/actuator/prometheus` | Prometheus metrics scrape endpoint |
| `/api/swagger-ui.html` | Interactive API documentation |

---

## Request / Response Examples

### Submit a Research Job

```http
POST /api/v1/research
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "query": "What are the latest breakthroughs in quantum computing in 2025?",
  "searchTopN": 10,
  "rerankerTopK": 5,
  "retrieverTopK": 8,
  "refinementIterations": 2
}
```

**Response 202 Accepted:**
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "CREATED",
  "createdAt": "2025-04-27T10:30:00Z",
  "streamUrl": "/api/v1/research/550e8400.../stream",
  "statusUrl": "/api/v1/research/550e8400..."
}
```

### Poll Job Status (completed)

```http
GET /api/v1/research/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer <jwt>
```

**Response 200:**
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "query": "What are the latest breakthroughs...",
  "createdAt": "2025-04-27T10:30:00Z",
  "completedAt": "2025-04-27T10:30:28Z",
  "elapsedMs": 27843,
  "result": {
    "answer": "## Quantum Computing in 2025\n\nRecent research has shown...",
    "sources": ["https://nature.com/quantum-2025", "https://ibm.com/blog/..."],
    "confidence": 0.891,
    "criticFeedback": {
      "factualCorrectnessScore": 0.93,
      "completenessScore": 0.87,
      "hallucinationRisk": 0.09,
      "overallQuality": 0.90
    },
    "refinementIterationsRun": 1,
    "elapsedSeconds": 27.2
  }
}
```

### SSE Stream (Angular EventSource)

```typescript
// Angular: subscribe to live pipeline events
const source = new EventSource(
  `/api/v1/research/${jobId}/stream`,
  { withCredentials: true }
);

source.addEventListener('in_progress', (e) => console.log('Running...', e.data));
source.addEventListener('completed', (e) => {
  const result = JSON.parse(e.data);
  console.log('Done!', result);
  source.close();
});
source.addEventListener('failed', (e) => {
  console.error('Failed:', e.data);
  source.close();
});
```

---

## Quick Start

### Prerequisites
- Java 21+
- Docker + Docker Compose
- Python FastAPI service running (from previous project)

### 1. Configure environment

```bash
cp .env.example .env
# Edit .env — add OPENAI_API_KEY and change JWT_SECRET
```

### 2. Run full stack

```bash
# Start everything: Postgres + Redis + Python Agent + Spring Boot
docker-compose up --build

# With monitoring (Prometheus + Grafana)
docker-compose --profile monitoring up --build
```

### 3. Local development (without Docker for Spring Boot)

```bash
# Start only infrastructure
docker-compose up postgres redis python-agent

# Run Spring Boot locally
./mvnw spring-boot:run
```

### 4. Access

| Service | URL |
|---|---|
| Spring Boot API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/api/swagger-ui.html |
| Python Agent | http://localhost:8000/docs |
| Grafana | http://localhost:3001 (monitoring profile) |
| Prometheus | http://localhost:9090 (monitoring profile) |

### 5. Default credentials (dev only)

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | ADMIN, USER |
| `researcher` | `research123` | USER |

> **Production:** Replace `AppUserDetailsService` with a JPA-backed user store.

---

## Configuration Reference

| Property | Default | Description |
|---|---|---|
| `app.agent.base-url` | `http://localhost:8000` | Python FastAPI URL |
| `app.agent.read-timeout-ms` | `120000` | 2 min pipeline timeout |
| `app.agent.max-retries` | `2` | Retry attempts on failure |
| `app.jwt.expiration-ms` | `86400000` | Access token lifetime (24h) |
| `app.rate-limit.research-requests-per-minute` | `10` | Per-IP research rate limit |
| `app.cors.allowed-origins` | `http://localhost:4200` | Angular origin |

---

## Resilience Behaviour

| Scenario | Behaviour |
|---|---|
| Python service down | Circuit Breaker opens → 503 with retry-after header |
| Python slow > 120s | TimeLimiter triggers → job marked FAILED |
| Same query repeated | Redis cache hit → instant response, no Python call |
| High traffic | Rate limiter (10 req/min/IP) + async queue (500 jobs) |
| Retry on transient error | 2 retries with 5s/10s exponential backoff |
| Stuck job (crash recovery) | Scheduler marks IN_PROGRESS > 10min jobs as FAILED |

---

## Running Tests

```bash
# All tests
./mvnw test

# Single test class
./mvnw test -Dtest=ResearchServiceTest

# With coverage report
./mvnw test jacoco:report
# Report at: target/site/jacoco/index.html
```
