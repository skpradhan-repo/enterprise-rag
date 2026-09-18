# Enterprise RAG Platform

A **production-grade, fully offline enterprise AI reference application** demonstrating:

- Enterprise RAG with Spring AI + PGVector + Ollama
- LLM Client/Server separation (Spring AI → Ollama)
- MCP Client/Server pattern with authorized enterprise tools
- Multi-tenant security with Keycloak + JWT
- Full observability with Prometheus + Grafana

> **New to this project?** Read the **[Developer Runbook](RUNBOOK.md)** — it covers every setup step, every issue we encountered running it for the first time, and exactly how each one was resolved.

---

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Java | 17 LTS |
| Framework | Spring Boot | 3.4.5 |
| AI Layer | Spring AI | 1.0.0 |
| LLM Server | Ollama | latest |
| Chat Model | llama3.2 | — |
| Embedding Model | nomic-embed-text | — |
| Vector DB | PostgreSQL + pgvector | 16 |
| Identity | Keycloak | 26.2.5 |
| Doc Extraction | Apache Tika | 3.0.0 |
| Migrations | Flyway | 10.x |
| Testing | Testcontainers | 1.20.6 |
| Frontend | React + TypeScript + Vite | 18 / 5 / 5 |
| Browser Tests | Playwright | latest |

---

## Prerequisites

- Java 17+
- Maven 3.9+
- Docker **or** Podman + compose plugin
- Node.js 20+ (only needed for frontend dev or browser tests)

> **Windows users:** Podman Desktop (with WSL2) works as a full Docker substitute. See [RUNBOOK.md — Issue #1](RUNBOOK.md#issue-1-docker-not-installed--podman-used-instead).

---

## Quick Start

### 1. Clone and configure

```bash
git clone https://github.com/<your-org>/enterprise-rag.git
cd enterprise-rag
cp .env.example .env          # passwords — never commit .env
```

### 2. Build the backend JAR

```bash
mvn clean package -DskipTests
```

### 3. Start all services

```bash
docker compose up -d
# or: podman compose up -d
```

First run downloads Ollama models (~2.3 GB). Wait ~3–5 minutes, then check:

```bash
docker compose ps          # all services should show "healthy" or "Up"
# or: podman ps
```

### 4. Open the app

| Service | URL |
|---------|-----|
| **Frontend** | **http://localhost:5173** |
| Swagger / API docs | http://localhost:8090/swagger-ui.html |
| Keycloak admin | http://localhost:8180 |
| Grafana dashboards | http://localhost:3001 |

### 5. Login

| Username | Password | Roles |
|----------|----------|-------|
| `admin@acme.com` | `Admin@1234` | ADMIN · UPLOADER · USER |
| `user@acme.com` | `User@1234` | USER |
| `user@beta.com` | `User@1234` | USER (different tenant) |

### 6. Explore the full flow

1. **Documents page** — upload a PDF, DOCX, TXT, or MD file; wait for `INDEXED` status
2. **RAG Chat page** — ask questions grounded in your uploaded documents
3. **LLM Demo page** — call Ollama directly (no RAG) to compare responses

---

## Running Browser Tests

The Playwright test suite (46 tests, 4 files) mocks auth and API calls — no running stack needed:

```bash
cd frontend
npm install
npx playwright install chromium   # one-time setup
npm run test:e2e                   # headless
npm run test:e2e:headed            # watch the browser
npm run test:e2e:ui                # interactive debugger
```

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/documents` | Upload document |
| GET | `/api/v1/documents` | List documents |
| GET | `/api/v1/documents/{id}` | Get document |
| DELETE | `/api/v1/documents/{id}` | Soft-delete |
| POST | `/api/v1/rag/query` | RAG query |
| POST | `/api/v1/llm/demo` | Direct LLM call |
| GET | `/actuator/health` | Health check |
| GET | `/actuator/prometheus` | Metrics |
| GET | `/swagger-ui.html` | API docs |

---

## Security Notes

- All ports bound to `127.0.0.1` only — nothing exposed to the network
- No secrets in source code — environment variables only
- JWT validation via Keycloak JWKS
- Tenant isolation enforced at vector retrieval layer
- Prompt injection defence in system prompt + input sanitiser
- Audit trail for all AI operations

---

## Documentation

| Document | What's in it |
|----------|-------------|
| **[RUNBOOK.md](RUNBOOK.md)** | Full setup guide, service URLs, credentials, 7 documented issues + solutions, useful commands |
| **[docs/TESTING.md](docs/TESTING.md)** | Playwright e2e guide, mock strategy, writing new tests, backend Testcontainers setup, CI integration |
| **[docs/CONTRIBUTING.md](docs/CONTRIBUTING.md)** | Fork workflow, branch naming, commit conventions, PR checklist, first-time GitHub publish guide |
| **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** | System architecture, module structure, data flow diagrams |
| **[docs/SECURITY.md](docs/SECURITY.md)** | Security model, JWT flow, tenant isolation, prompt injection defence |
| **[docs/RAG-ARCHITECTURE.md](docs/RAG-ARCHITECTURE.md)** | RAG pipeline deep-dive: ingestion, chunking, embedding, retrieval, prompting |
| **[docs/MCP-ARCHITECTURE.md](docs/MCP-ARCHITECTURE.md)** | MCP Client/Server pattern, available tools |
| **[docs/OBSERVABILITY.md](docs/OBSERVABILITY.md)** | Prometheus metrics, Grafana dashboards, tracing |
| **[docs/LLM-CLIENT-SERVER.md](docs/LLM-CLIENT-SERVER.md)** | Spring AI ChatClient → Ollama integration |

## Troubleshooting

See **[RUNBOOK.md — Issues Faced & How We Solved Them](RUNBOOK.md#8-issues-faced--how-we-solved-them)** for documented solutions to:

- Docker not installed (Podman substitute)
- Keycloak `direct_access_grants` error during API testing
- RAG returning "I cannot find relevant information"
- Nginx 502 Bad Gateway in Podman networks
- `rag-ollama-init` showing `Exited (0)` (expected behaviour)
- Spring Boot failing to start due to missing `DB_PASSWORD`
- Playwright tests blocked by Keycloak redirect
