# Demo Script (10-15 minutes)

## Part 1 — Login (1 min)
1. Open http://localhost:5173
2. You are redirected to Keycloak login
3. Login as `admin@acme.com` / `Admin@1234`
4. Observe the nav bar shows email and tenant: **acme**

## Part 2 — Document Upload (2 min)
1. Navigate to **Documents** page
2. Upload a PDF or TXT file (e.g. a healthcare policy)
3. Observe status: `UPLOADED` → `PROCESSING` → `INDEXED`
4. This demonstrates: Tika extraction → chunking → nomic-embed-text → PGVector

## Part 3 — RAG Query (3 min)
1. Navigate to **RAG Chat**
2. Ask: *"What does the policy say about eligibility?"*
3. Observe:
   - Answer is grounded in document content
   - Sources panel shows document name, page, similarity score
   - Response includes `correlationId` and `latencyMs`
4. This demonstrates the full RAG pipeline

## Part 4 — LLM Client/Server Demo (2 min)
1. Navigate to **LLM Demo**
2. Enter: *"Explain RAG in 3 sentences"*
3. Observe:
   - Architecture box: `Spring AI → Ollama → llama3.2`
   - Model name and provider in response
   - No sources returned (pure LLM, no RAG)

## Part 5 — Multi-Tenant Security (2 min)
1. Logout
2. Login as `user@beta.com` / `User@1234` (Tenant: **beta**)
3. Navigate to Documents — observe **no documents** (tenant-isolated)
4. Ask the same question in RAG Chat — fallback response: no relevant documents found
5. This proves tenant isolation is enforced at the retrieval layer

## Part 6 — Observability (2 min)
1. Open Grafana: http://localhost:3001
2. Show custom RAG metrics: chat requests, latency, ingestion count
3. Open Prometheus: http://localhost:9090
4. Query: `rag_chat_requests_total`

---

## Key Talking Points

| Concept | Demonstration |
|---------|---------------|
| RAG | Upload → Index → Query → Grounded answer with sources |
| LLM Client | Spring AI ChatClient |
| LLM Server | Ollama at localhost:11434 |
| Local LLM | llama3.2 (no cloud, no paid API) |
| MCP | SSE tools: searchKnowledge, getDocumentMetadata |
| Security | Tenant isolation prevents cross-tenant access |
| Observability | Prometheus metrics + Grafana dashboards |
| No paid services | Everything runs locally, 100% free |
