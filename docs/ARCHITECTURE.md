# Architecture

## System Overview

```mermaid
graph TD
    UI[React UI<br/>TypeScript · Vite] -->|HTTPS/REST| API[Spring Boot API]
    API -->|JWT validation| KC[Keycloak 26<br/>Identity Provider]
    API -->|Spring AI ChatClient| OL[Ollama<br/>LLM Server]
    OL --> LLM[llama3.2<br/>Local Model]
    API -->|VectorStore.similaritySearch| PG[(PostgreSQL 16<br/>+ pgvector)]
    API -->|MCP SSE| MCP[MCP Server<br/>Enterprise Tools]
    MCP -->|authorized reads| PG
    API -->|metrics/traces| PROM[Prometheus]
    PROM --> GR[Grafana]

    subgraph "Spring Boot Modular Monolith"
        direction TB
        SEC[Security Layer<br/>Keycloak JWT · Tenant]
        RAG[RAG Orchestrator<br/>Retrieval · Prompt · Sources]
        ING[Ingestion Pipeline<br/>Tika · Chunking · Embedding]
        AUDIT[Audit Service]
    end
```

## Module Structure

```
com.enterprise.rag
├── api                  REST controllers, DTOs, error handling
├── security             JWT converter, tenant context, authorization
├── rag
│   ├── ingestion        Tika extraction, text normalization
│   ├── chunking         Sliding-window token chunker
│   ├── retrieval        Authorization-aware vector search
│   ├── orchestration    Full RAG pipeline coordinator
│   └── prompt           Prompt templates, injection sanitizer
├── ai
│   ├── chat             ChatClient configuration
│   └── embedding        EmbeddingModel wrapper
├── mcp
│   └── tools            4 MCP tools + Spring AI tool registration
├── document             Document entity, repo, service, controller
├── conversation         Conversation + Message entities
├── audit                AuditEvent entity + async audit service
├── observability        Micrometer custom metrics
└── configuration        AppProperties, WebConfig, AsyncConfig
```

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Modular monolith | Start simple; each module has clear boundaries for future extraction |
| Spring AI abstractions | `ChatClient`, `VectorStore`, `EmbeddingModel` — never Ollama-specific APIs in business logic |
| Flyway migrations | Production-safe schema management; no DDL auto |
| Async ingestion | `@Async` with dedicated thread pool keeps uploads non-blocking |
| Separate audit transaction | `REQUIRES_NEW` ensures audit record is never rolled back with business transaction |
| Localhost binding | All ports on `127.0.0.1` — no unintended external exposure |
