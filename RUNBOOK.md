# Enterprise RAG Platform — Developer Runbook

> **Goal:** Clone → run → explore in under 15 minutes.
> This document captures every issue encountered during our first local run and exactly how each one was resolved.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Prerequisites](#2-prerequisites)
3. [Clone & Configure](#3-clone--configure)
4. [Running with Docker / Podman Compose](#4-running-with-docker--podman-compose)
5. [Service URLs & Credentials](#5-service-urls--credentials)
6. [Exploring the Application](#6-exploring-the-application)
7. [Running the Browser Test Suite](#7-running-the-browser-test-suite)
8. [Issues Faced & How We Solved Them](#8-issues-faced--how-we-solved-them)
9. [RAG — What to Ask & What to Expect](#9-rag--what-to-ask--what-to-expect)
10. [Architecture Deep-Dive](#10-architecture-deep-dive)
11. [Useful Commands](#11-useful-commands)

---

## 1. Architecture Overview

```
Browser
  │
  ▼
┌─────────────────────────────────┐  port 5173
│  React + TypeScript + Vite      │  (Nginx inside container)
│  - RAG Chat page                │
│  - Documents page               │
│  - LLM Demo page                │
└──────────────┬──────────────────┘
               │  /api/*  (proxied by Nginx)
               ▼
┌─────────────────────────────────┐  port 8090
│  Spring Boot 3.4.5              │
│  Spring AI 1.0.0                │
│  - Document ingestion pipeline  │
│  - RAG orchestration            │
│  - MCP Client + Server          │
│  - Keycloak JWT auth (RBAC)     │
│  - Multi-tenant isolation       │
│  - Flyway migrations            │
└───┬──────────────────┬──────────┘
    │                  │
    ▼                  ▼
┌──────────┐    ┌──────────────────┐  port 11434
│PostgreSQL│    │  Ollama LLM      │
│+ pgvector│    │  llama3.2 (chat) │
│  port    │    │  nomic-embed-text│
│  5432    │    │  (embedding)     │
└──────────┘    └──────────────────┘

┌──────────────┐  port 8180   ┌──────────┐  port 9090
│  Keycloak 26 │              │Prometheus│
│  enterprise- │              └────┬─────┘
│  rag realm   │                   │
└──────────────┘              ┌────▼─────┐  port 3001
                              │ Grafana  │
                              └──────────┘
```

**Key design decisions:**
- All ports are bound to `127.0.0.1` — nothing is exposed to the network
- Nginx inside the frontend container reverse-proxies `/api/*` to the backend — no CORS issues
- Keycloak PKCE flow (no client secret needed in the browser)
- Tenant isolation is enforced at the vector retrieval layer — each user only sees their own documents
- The backend JAR must be built **before** `docker compose up` (see step 4.2)

---

## 2. Prerequisites

| Tool | Minimum Version | Check command |
|------|----------------|---------------|
| Java JDK | 17 (pom uses 17; 21 also works) | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Docker **or** Podman | Docker 24+ / Podman 5+ | `docker --version` or `podman --version` |
| Docker Compose **or** Podman Compose | v2+ | `docker compose version` or `podman compose version` |
| Node.js | 20+ (only needed for frontend dev or tests) | `node --version` |
| npm | 9+ | `npm --version` |
| Git | Any | `git --version` |

> **Windows users:** Docker Desktop is the easiest path. Podman Desktop with WSL2 also works — see [Issue #1](#issue-1-docker-not-installed--podman-used-instead).

> **macOS / Linux:** Docker Desktop or the native Docker Engine both work.

### Disk & RAM requirements

| Resource | Minimum | Recommended |
|----------|---------|-------------|
| RAM | 8 GB | 16 GB |
| Disk (models) | 10 GB free | 20 GB free |
| Disk (containers) | 5 GB free | 10 GB free |

> Ollama downloads `llama3.2` (~2 GB) and `nomic-embed-text` (~270 MB) on first start. This happens once and is cached in the `ollama-data` Docker volume.

---

## 3. Clone & Configure

```bash
git clone https://github.com/<your-org>/enterprise-rag.git
cd enterprise-rag

# Copy the example env file — never commit .env
cp .env.example .env
```

The `.env` file controls passwords. The defaults work for local development:

```dotenv
POSTGRES_PASSWORD=ragpass
KEYCLOAK_ADMIN_USER=admin
KEYCLOAK_ADMIN_PASSWORD=admin
LLM_MODEL=llama3.2
EMBEDDING_MODEL=nomic-embed-text
GRAFANA_ADMIN_PASSWORD=admin
```

> **Security note:** These defaults are intentionally simple for local dev. Use strong random passwords for any environment accessible over a network.

---

## 4. Running with Docker / Podman Compose

### 4.1 Build the backend JAR first

The `app` container copies a pre-built JAR from `target/`. You must build it before composing:

```bash
# From the project root
mvn clean package -DskipTests
```

Expected output (last lines):
```
[INFO] Building jar: .../target/enterprise-rag-1.0.0-SNAPSHOT.jar
[INFO] BUILD SUCCESS
```

> **Why `-DskipTests`?** The integration tests use Testcontainers (they spin up their own PostgreSQL). Running them requires Docker to be available and adds ~3 minutes. Skip for a first run; run them separately with `mvn test`.

### 4.2 Start all services

```bash
# Docker
docker compose up -d

# Podman (Windows/WSL2)
podman compose up -d
```

This starts 7 services. Wait times on first run:

| Service | First-run wait | Subsequent starts |
|---------|---------------|-------------------|
| PostgreSQL | ~10 s | ~5 s |
| Keycloak | ~90 s | ~30 s |
| Ollama (service) | ~30 s | ~10 s |
| Ollama model pull | **2–5 min** (downloads ~2.3 GB) | instant |
| Spring Boot app | ~90 s (waits for all above) | ~30 s |
| React frontend | ~60 s (npm build inside container) | ~10 s |

**Check all services are healthy:**

```bash
# Docker
docker compose ps

# Podman
podman ps

# Expected: all show "healthy" or "Up"
```

```
CONTAINER         STATUS
rag-postgres      Up (healthy)
rag-keycloak      Up (healthy)
rag-ollama        Up (healthy)
rag-ollama-init   Exited (0)       ← normal, this one-shot container exits after pulling models
rag-app           Up (healthy)
rag-frontend      Up
rag-prometheus    Up
rag-grafana       Up
```

### 4.3 Verify all endpoints respond

```bash
# Quick health check (Linux/macOS)
curl http://localhost:8090/actuator/health
# Expected: {"status":"UP","groups":["liveness","readiness"]}

curl -s http://localhost:5173 | head -5
# Expected: <!DOCTYPE html> ...

curl -s http://localhost:8180/realms/enterprise-rag | python3 -m json.tool | grep realm
# Expected:   "realm": "enterprise-rag",
```

```powershell
# PowerShell (Windows)
(Invoke-RestMethod http://localhost:8090/actuator/health).status
# Expected: UP

(Invoke-WebRequest http://localhost:5173 -UseBasicParsing).StatusCode
# Expected: 200
```

### 4.4 Stopping and restarting

```bash
# Stop all containers (data volumes preserved)
docker compose down

# Stop and wipe ALL data (start fresh)
docker compose down -v

# Restart a single service after a code change
mvn clean package -DskipTests
docker compose up -d --build app
```

---

## 5. Service URLs & Credentials

### Application URLs

| Service | URL | Notes |
|---------|-----|-------|
| **Frontend (React)** | **http://localhost:5173** | Start here — redirects to Keycloak login |
| Backend API | http://localhost:8090 | Not accessed directly from browser |
| Swagger / OpenAPI | http://localhost:8090/swagger-ui.html | Interactive API explorer |
| Actuator health | http://localhost:8090/actuator/health | Used by Docker healthcheck |
| Keycloak admin | http://localhost:8180 | Identity provider console |
| Grafana dashboards | http://localhost:3001 | Metrics & tracing |
| Prometheus | http://localhost:9090 | Raw metrics scrape |
| Ollama API | http://localhost:11434 | LLM server (API only) |

### Login credentials

**Application login** (at http://localhost:5173):

| Username | Password | Tenant | Roles |
|----------|----------|--------|-------|
| `admin@acme.com` | `Admin@1234` | acme | ADMIN · UPLOADER · USER |
| `user@acme.com` | `User@1234` | acme | USER |
| `user@beta.com` | `User@1234` | beta | USER |

> **Multi-tenancy in action:** `admin@acme.com` and `user@beta.com` share the same database but **cannot see each other's documents** — tenant isolation is enforced at the vector retrieval layer.

**Keycloak admin console** (at http://localhost:8180):
- Username: `admin`
- Password: `admin` (from `.env` / `KEYCLOAK_ADMIN_PASSWORD`)

**Grafana** (at http://localhost:3001):
- Username: `admin`
- Password: `admin` (from `.env` / `GRAFANA_ADMIN_PASSWORD`)

---

## 6. Exploring the Application

### Step 1 — Open the app and log in

Navigate to **http://localhost:5173**.  
You will be redirected to Keycloak. Log in with `admin@acme.com` / `Admin@1234`.

After login you land on the **RAG Chat** page. The nav bar shows:
- Your email (`admin@acme.com`)
- Your tenant (`acme`)
- Three pages: **RAG Chat** · **Documents** · **LLM Demo**

### Step 2 — Upload a document

1. Click **Documents** in the nav bar → http://localhost:5173/documents
2. Click **Choose File** → select any `.pdf`, `.docx`, `.txt`, or `.md` file
3. Enter a **title** (required)
4. Optionally enter comma-separated **tags**
5. Click **Upload**

The document goes through this pipeline:
```
UPLOADED → PROCESSING → INDEXED
```

- `UPLOADED` — file received, stored on disk
- `PROCESSING` — Apache Tika extracts text, chunked into 512-token segments with 64-token overlap, each chunk embedded via `nomic-embed-text` and stored in pgvector
- `INDEXED` — all chunks are in the vector store; RAG queries will find them

> Wait until the status shows **INDEXED** before asking questions. Refresh the page to update the status.

### Step 3 — Ask questions in RAG Chat

1. Click **RAG Chat** in the nav bar → http://localhost:5173
2. Type a question and press **Enter** or click **Send**
3. The answer appears with:
   - Source document name and page number
   - Similarity score (how relevant the chunk was)
   - Response latency in milliseconds

**Good first questions to try** (after uploading a document):
- *"Summarize the key points of [your document title]"*
- *"What does [your document] say about [topic]?"*
- *"List the main sections covered in this document"*

The system only answers from documents you have uploaded — it does **not** use general internet knowledge.

### Step 4 — Try the LLM Demo page

Click **LLM Demo** → http://localhost:5173/llm-demo

This page calls Ollama **directly** — no document retrieval, no RAG, no vector search. It's a raw `Spring AI ChatClient → Ollama` call. Use it to:

- Verify Ollama is working (`"Say hello in one sentence"`)
- Compare RAG vs non-RAG answers to the same question
- Test the LLM's general knowledge vs grounded document answers

### Step 5 — View metrics in Grafana

Navigate to http://localhost:3001 (login: `admin` / `admin`).  
The pre-provisioned dashboard shows:
- HTTP request rates and p95 latency
- RAG query latency and throughput
- JVM heap and GC metrics

---

## 7. Running the Browser Test Suite

The project includes a **Playwright** end-to-end test suite that covers all three pages. Tests mock both Keycloak auth and all backend API calls — **no running services are required**.

### Setup (first time only)

```bash
cd frontend
npm install
npx playwright install chromium
```

### Run the tests

```bash
cd frontend

# Headless — fastest, good for CI
npm run test:e2e

# Watch the browser run tests live
npm run test:e2e:headed

# Interactive Playwright UI with time-travel debugging
npm run test:e2e:ui

# Open the HTML report after a run
npm run test:e2e:report
```

### What's tested

| File | Tests | Covers |
|------|-------|--------|
| `e2e/navigation.spec.ts` | 7 | Nav bar, links, user identity display |
| `e2e/chat.spec.ts` | 11 | Send message, Enter key, error states, conversation history |
| `e2e/documents.spec.ts` | 14 | List docs, upload validation, delete, error handling |
| `e2e/llm-demo.spec.ts` | 14 | Prompt submit, loading state, metadata display, errors |
| **Total** | **46** | All three pages + navigation |

### How the mock works

The tests use two helpers from [`e2e/helpers/auth.ts`](frontend/e2e/helpers/auth.ts):

- **`mockAuthenticatedUser(page)`** — intercepts the `keycloak-js` ES module via `page.route()` and replaces it with a stub that resolves immediately as authenticated. No Keycloak server needed.
- **`mockApiRoutes(page)`** — intercepts all `/api/v1/*` XHR calls with realistic fixture responses (documents list, RAG answers, LLM responses).

---

## 8. Issues Faced & How We Solved Them

### Issue #1: Docker not installed — Podman used instead

**Symptom:**
```
docker : The term 'docker' is not recognized as the name of a cmdlet...
```

**Root cause:** Docker Desktop was not installed on the Windows 11 machine. Only Podman (via WSL2) was available.

**Solution:** Podman is a drop-in replacement for Docker. All `docker` commands work identically with `podman`:

```bash
# Instead of:
docker compose up -d

# Use:
podman compose up -d

# Check running containers:
podman ps
```

**Podman on Windows — setup steps:**
1. Install [Podman Desktop](https://podman-desktop.io/) — it bundles Podman + WSL2
2. During first launch, click **Initialize and start** to create the Podman machine
3. Podman exposes a Docker-compatible socket, so `docker-compose` CLI also works via Podman

> `podman compose version` showed `Docker Compose version v5.1.1` — this is `docker-compose` CLI being called through Podman's compatibility layer. Both work.

---

### Issue #2: Keycloak `direct_access_grants` disabled for `rag-frontend` client

**Symptom:** When trying to get a token via password grant for API testing:
```json
{"error":"unauthorized_client","error_description":"Client not allowed for direct access grants"}
```

**Root cause:** The `rag-frontend` Keycloak client has `directAccessGrantsEnabled: false` by design — it is a public browser client that uses PKCE flow (the proper secure approach). Direct access (password grant) is intentionally disabled to prevent credential exposure.

**Solution:** Use the `rag-app` client for programmatic/API testing — it has `directAccessGrantsEnabled: true`:

```bash
# WRONG — rag-frontend does not allow password grant
curl -d "grant_type=password&client_id=rag-frontend&username=admin@acme.com&password=Admin@1234" \
  http://localhost:8180/realms/enterprise-rag/protocol/openid-connect/token

# CORRECT — use rag-app client for API testing
curl -d "grant_type=password&client_id=rag-app&username=admin@acme.com&password=Admin@1234" \
  http://localhost:8180/realms/enterprise-rag/protocol/openid-connect/token
```

```powershell
# PowerShell equivalent
$token = (Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8180/realms/enterprise-rag/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" `
  -Body "grant_type=password&client_id=rag-app&username=admin@acme.com&password=Admin@1234"
).access_token
```

**The browser app (`rag-frontend`) is not affected** — it uses the PKCE redirect flow automatically when you open http://localhost:5173.

---

### Issue #3: RAG returns "I cannot find relevant information" for some questions

**Symptom:** The chat returns the fallback message even though a document is uploaded and shows `INDEXED` status.

**Root cause:** The RAG system uses a **similarity threshold of 0.65** (cosine distance). If the query embedding is not similar enough to any stored chunk, no sources are retrieved and the fallback prompt fires. This is correct behaviour — the system refuses to hallucinate.

Three sub-causes were identified:

**a) The document has only one chunk of meaningful text**  
A test document (`test.txt`) containing a single sentence produced only one chunk. Questions that didn't closely match that sentence returned no results.

**b) PDF chunking didn't surface all pages above threshold**  
The 3-page `Personal Accident Coverage 2026.pdf` only had 2 of 3 pages exceed the 0.65 similarity threshold on typical queries. Page 1 (the benefits schedule table) had lower similarity scores for natural-language queries.

**c) Query phrasing matters**  
Keyword-style queries (`"premium amount"`) scored lower than natural questions (`"What is covered under accident insurance?"`).

**Solutions:**

1. **Upload richer documents** — multi-page PDFs with paragraphs of prose retrieve better than single-sentence files or image-heavy PDFs.

2. **Phrase queries naturally** — ask full questions rather than keywords:
   - ❌ `"accident coverage"` 
   - ✅ `"What is covered under this accident insurance policy?"`

3. **Tune retrieval settings** in `.env` (optional):
   ```dotenv
   RAG_SIMILARITY_THRESHOLD=0.55   # lower = more results (but more noise)
   RAG_TOP_K=8                      # retrieve more chunks per query
   CHUNK_SIZE=256                   # smaller chunks = finer granularity
   CHUNK_OVERLAP=32
   ```

4. **Re-upload failed documents** — if a document stuck at `PROCESSING`, delete it and re-upload. Check backend logs with:
   ```bash
   docker logs rag-app --tail 50
   # or
   podman logs rag-app --tail 50
   ```

---

### Issue #4: Nginx DNS resolution fails in Podman — `resolver` directive required

**Symptom:** The frontend container starts but all `/api/*` requests return `502 Bad Gateway`.

**Root cause:** Docker's embedded DNS resolver lives at `127.0.0.11`. Podman's DNS resolver lives at `10.89.2.1`. Nginx caches DNS at startup by default. If the `app` container restarts after `frontend` started, Nginx holds a stale IP.

**Solution (already applied in [`frontend/nginx.conf`](frontend/nginx.conf)):**

```nginx
# Use Podman's DNS so nginx re-resolves 'app' on every request
resolver 10.89.2.1 valid=5s ipv6=off;
set $backend http://app:8090;

location /api/ {
    proxy_pass $backend;
    ...
}
```

Using a variable (`$backend`) instead of a literal in `proxy_pass` forces Nginx to re-resolve the hostname on each request rather than caching it at startup. The `resolver` line points to Podman's gateway.

> **Docker users:** If you see 502s under Docker, change `10.89.2.1` to `127.0.0.11` in `nginx.conf`, rebuild the frontend image, and restart the container.

---

### Issue #5: `rag-ollama-init` container shows `Exited (0)` — is this a problem?

**Symptom:** `podman ps` shows `rag-ollama-init` with status `Exited (0)`.

**Answer:** This is **expected and correct**. The `ollama-init` service is a one-shot container that:
1. Waits for `rag-ollama` to become healthy
2. Pulls `llama3.2` and `nomic-embed-text` models
3. Exits with code 0 (success)

After the models are downloaded into the `ollama-data` volume they persist across restarts. The init container never runs again (it has `restart: "no"`).

---

### Issue #6: Spring Boot app fails to start — `DB_PASSWORD` not set

**Symptom:**
```
Caused by: org.springframework.boot.context.properties.ConfigurationPropertiesBindException:
Error creating bean with name 'spring.datasource-...': Could not bind properties
...
IllegalArgumentException: Could not resolve placeholder 'DB_PASSWORD' in value "${DB_PASSWORD}"
```

**Root cause:** `DB_PASSWORD` has no default value in `application.yml` — it is intentionally required to be set explicitly (avoids silent use of a weak default in any environment).

**Solution:** Ensure `.env` exists with `POSTGRES_PASSWORD` set. When running via compose, the env file is picked up automatically. When running the JAR directly:

```bash
# Linux/macOS
export DB_PASSWORD=ragpass
java -jar target/enterprise-rag-1.0.0-SNAPSHOT.jar

# Windows PowerShell
$env:DB_PASSWORD = "ragpass"
java -jar target/enterprise-rag-1.0.0-SNAPSHOT.jar

# Or pass inline
DB_PASSWORD=ragpass java -jar target/enterprise-rag-1.0.0-SNAPSHOT.jar
```

---

### Issue #7: Frontend Playwright tests — Keycloak blocks tests

**Symptom:** Playwright tests hang on `"Authenticating…"` spinner or redirect to Keycloak login page.

**Root cause:** The app's `AuthProvider` calls `keycloak.init({ onLoad: 'login-required' })` — if Keycloak is not reachable or the mock is not in place, the browser gets redirected.

**Solution:** The test suite in `e2e/helpers/auth.ts` intercepts the `keycloak-js` ES module via `page.route()` before the page loads and replaces it with a stub that resolves immediately:

```typescript
await page.route('**/keycloak-js/**', async route => {
  await route.fulfill({
    status: 200,
    contentType: 'application/javascript',
    body: `export default function Keycloak() { return window['__MOCK_KC__']; }`
  })
})
```

**Always call `mockAuthenticatedUser(page)` before `page.goto()`** — the route intercept must be registered before navigation.

---

## 9. RAG — What to Ask & What to Expect

### How RAG works in this system

```
User question
     │
     ▼
Embed question via nomic-embed-text
     │
     ▼
Vector similarity search in pgvector
(top-K=5 chunks, similarity ≥ 0.65)
     │
     ├── No chunks found → fallback response ("I cannot find...")
     │
     ▼
Assemble context from chunks
     │
     ▼
Build prompt: system instructions + context + question
     │
     ▼
llama3.2 generates answer grounded in retrieved context
     │
     ▼
Return answer + source citations + latency
```

### Tips for good questions

| Do | Don't |
|----|-------|
| Ask complete natural-language questions | Use short keyword queries |
| Reference the topic clearly in the question | Assume the LLM knows context from previous messages |
| Ask about content that is actually in your documents | Expect answers about general knowledge |
| Upload documents with substantial prose | Upload image-only PDFs (text extraction requires actual text) |

### Understanding the response format

```
[Source 1] My Document.pdf (page 3):
The policy covers accidental death and permanent disability...

Based on the provided context, here is the answer: ...
```

- **[Source N]** — the N-th most relevant chunk
- **page X** — the page in the original document where this chunk came from
- The LLM synthesizes across all retrieved sources

### What happens if no relevant document is uploaded

The system returns:
```
I cannot find relevant information in the available documents to answer your question.

Please try:
- Rephrasing your question with different keywords
- Checking whether the relevant documents have been uploaded and indexed
- Contacting your administrator if you believe this information should be available
```

This is intentional — the system does **not** fall back to general LLM knowledge. Every answer must be grounded in your uploaded documents.

---

## 10. Architecture Deep-Dive

### Document ingestion pipeline

```
POST /api/v1/documents (multipart form)
  │
  ├── DocumentController validates request
  ├── Saves file to ./uploads/
  ├── Creates Document record (status=UPLOADED)
  │
  └── Async: IngestionPipeline
        ├── DocumentExtractor (Apache Tika) → raw text
        ├── TextNormalizer → clean whitespace, fix encoding
        ├── ChunkingService → 512-token chunks, 64-token overlap
        ├── EmbeddingService → nomic-embed-text via Ollama
        ├── VectorStoreService → insert into pgvector
        └── Update Document status → INDEXED
```

### RAG query pipeline

```
POST /api/v1/rag/query
  │
  ├── Extract tenantId from JWT
  ├── Sanitize input (PromptSanitizer — blocks injection)
  │
  └── RagOrchestrator
        ├── EmbeddingService → embed question
        ├── RetrievalService → pgvector search (filtered by tenantId)
        ├── ContextAssembler → format chunks into context string
        ├── PromptTemplates → system prompt + context + question
        ├── ChatClient (Spring AI) → Ollama llama3.2
        └── SourceAttributor → map answer back to source chunks
```

### Multi-tenancy

Every document chunk stored in pgvector includes a `tenant_id` metadata field derived from the Keycloak JWT claim `tenant_id`. All vector similarity searches include a `WHERE metadata->>'tenant_id' = ?` filter — users from `acme` can never retrieve documents uploaded by users from `beta`.

### MCP (Model Context Protocol)

The backend exposes four MCP tools that an AI agent can call:
- `SearchKnowledgeTool` — semantic search across the knowledge base
- `GetDocumentByIdTool` — fetch a specific document by ID
- `GetDocumentMetadataTool` — get metadata for a document
- `GetPolicyInformationTool` — specialized policy document lookup

---

## 11. Useful Commands

### Check container logs

```bash
# All services
docker compose logs -f

# Specific service
docker logs rag-app -f --tail 100
docker logs rag-keycloak -f --tail 50
docker logs rag-ollama -f --tail 50

# Podman equivalent
podman logs rag-app -f --tail 100
```

### Test the API directly (without a browser)

```bash
# Get a token (use rag-app client — see Issue #2)
TOKEN=$(curl -s -d "grant_type=password&client_id=rag-app&username=admin@acme.com&password=Admin@1234" \
  http://localhost:8180/realms/enterprise-rag/protocol/openid-connect/token \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

# List documents
curl -H "Authorization: Bearer $TOKEN" http://localhost:8090/api/v1/documents | python3 -m json.tool

# RAG query
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"question":"What is RAG?"}' \
  http://localhost:8090/api/v1/rag/query | python3 -m json.tool

# LLM demo (no RAG)
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"prompt":"Explain vector embeddings in one paragraph"}' \
  http://localhost:8090/api/v1/llm/demo | python3 -m json.tool

# Upload a document
curl -X POST -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/your/document.pdf" \
  -F "title=My Document" \
  -F "tags=finance,policy" \
  http://localhost:8090/api/v1/documents | python3 -m json.tool
```

### Restart after a code change

```bash
# Backend change
mvn clean package -DskipTests
docker compose up -d --build app
# or
podman compose up -d --build app

# Frontend change
docker compose up -d --build frontend
# or
podman compose up -d --build frontend

# Run frontend in dev mode (hot reload, outside Docker)
cd frontend && npm run dev
# Open http://localhost:5173 — Vite proxies /api/* to http://localhost:8090
```

### Check Ollama models

```bash
# List downloaded models
curl http://localhost:11434/api/tags | python3 -m json.tool

# Check Ollama version
curl http://localhost:11434/api/version
```

### Run backend tests

```bash
# Unit tests only (fast, no Docker needed)
mvn test -Dtest='*Unit*,*Service*'

# Integration tests (requires Docker/Podman for Testcontainers)
mvn test

# Single test class
mvn test -Dtest=RagOrchestratorTest
```

### Reset everything and start fresh

```bash
# Stop and remove all containers AND volumes (wipes DB, model cache, etc.)
docker compose down -v
# or
podman compose down -v

# Then start again from step 4.1
mvn clean package -DskipTests
docker compose up -d
```

---

*Generated from a live developer session on Windows 11 with Podman 5.8.1, Java 17.0.12, Maven 3.9.10, Node 22.*
