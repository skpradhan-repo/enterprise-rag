# Observability

## Stack

| Tool | Purpose |
|------|---------|
| Spring Boot Actuator | Health, info, liveness/readiness probes |
| Micrometer | Metrics abstraction |
| Prometheus registry | Metrics scraping endpoint |
| OpenTelemetry | Distributed tracing |
| Prometheus | Time-series metrics store |
| Grafana | Metrics dashboards |

## Endpoints

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Overall health (public) |
| `/actuator/health/liveness` | Liveness probe |
| `/actuator/health/readiness` | Readiness probe |
| `/actuator/prometheus` | Prometheus metrics (requires ADMIN) |
| `/actuator/info` | Application info |

## Custom Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `rag.chat.requests.total` | Counter | Successful RAG queries |
| `rag.chat.failures.total` | Counter | Failed RAG queries |
| `rag.chat.latency` | Timer | End-to-end RAG latency |
| `rag.ingestion.total` | Counter | Documents ingested |
| `rag.ingestion.failures.total` | Counter | Ingestion failures |

## Access

- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001 (admin / from `.env`)
- Metrics endpoint: http://localhost:8090/actuator/prometheus (requires ADMIN JWT)
