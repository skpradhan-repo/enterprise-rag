# Testing Guide

This project has two levels of tests:

| Layer | Tool | Scope | Requires running stack? |
|-------|------|-------|------------------------|
| **Browser (E2E)** | Playwright | All 3 UI pages, navigation, auth flow | ❌ No — auth + API fully mocked |
| **Backend integration** | JUnit 5 + Testcontainers | REST controllers, RAG pipeline, security | ✅ Yes — Docker/Podman for Testcontainers |

---

## Browser Tests (Playwright)

### Setup — one time only

```bash
cd frontend
npm install
npx playwright install chromium
```

> Only Chromium is configured. Firefox and WebKit can be added in
> [`playwright.config.ts`](../frontend/playwright.config.ts) under `projects`.

### Run commands

```bash
cd frontend

# Headless — fastest, ideal for CI
npm run test:e2e

# Headed — watch Chromium run each test live
npm run test:e2e:headed

# Interactive UI — time-travel debugger, inspect DOM snapshots per step
npm run test:e2e:ui

# Open the HTML report after a run (auto-saved to frontend/playwright-report/)
npm run test:e2e:report
```

### What is covered

**46 tests across 4 spec files:**

| File | Tests | What it verifies |
|------|------:|-----------------|
| [`e2e/navigation.spec.ts`](../frontend/e2e/navigation.spec.ts) | 7 | Nav bar renders, all 3 links work, user email + tenant displayed, Logout button present |
| [`e2e/chat.spec.ts`](../frontend/e2e/chat.spec.ts) | 11 | Send via button, send via Enter, input cleared after send, assistant answer renders, source citation shown, multiple messages accumulate, API error state |
| [`e2e/documents.spec.ts`](../frontend/e2e/documents.spec.ts) | 14 | Document list renders, status badges (INDEXED/PROCESSING), Upload button disabled without file+title, successful upload clears form, upload error message, Delete triggers DELETE request |
| [`e2e/llm-demo.spec.ts`](../frontend/e2e/llm-demo.spec.ts) | 14 | Prompt textarea, Send disabled when empty, loading state "Calling LLM…", response metadata (model/provider/latency), exact prompt sent to API, delayed response, error state, previous response cleared |

### How mocking works

The app requires Keycloak PKCE authentication. Tests bypass this entirely with two helpers in [`e2e/helpers/auth.ts`](../frontend/e2e/helpers/auth.ts):

#### `mockAuthenticatedUser(page)`

Intercepts the `keycloak-js` ES module **before the page loads** and replaces it with a stub that resolves `init()` immediately as authenticated. A fake JWT payload with configurable `email`, `tenantId`, and `roles` is injected via `page.addInitScript()`.

```typescript
// Must be called BEFORE page.goto()
await mockAuthenticatedUser(page, {
  email: 'admin@acme.com',
  tenantId: 'acme',
  roles: ['ADMIN', 'USER'],
})
await page.goto('/')
```

#### `mockApiRoutes(page)`

Intercepts all `/api/v1/*` XHR calls via `page.route()` and returns realistic fixture data:

- `GET /api/v1/documents` → 2 documents (one INDEXED, one PROCESSING)
- `POST /api/v1/documents` → 201 Created (upload success)
- `DELETE /api/v1/documents/**` → 204 No Content
- `POST /api/v1/rag/query` → answer with one source citation
- `POST /api/v1/llm/demo` → response with model/provider/latency metadata

Individual tests can **override** specific routes to test error states:

```typescript
// Override just the RAG route to simulate a 500 error
await page.route('**/api/v1/rag/query', route =>
  route.fulfill({ status: 500, body: 'Internal Server Error' })
)
```

### Writing new tests

1. Create a new file in `frontend/e2e/` named `<feature>.spec.ts`
2. Import the helpers:
   ```typescript
   import { test, expect } from '@playwright/test'
   import { mockAuthenticatedUser, mockApiRoutes } from './helpers/auth'
   ```
3. Always call `mockAuthenticatedUser` before `page.goto()`:
   ```typescript
   test.beforeEach(async ({ page }) => {
     await mockAuthenticatedUser(page)
     await mockApiRoutes(page)
     await page.goto('/')
     await page.waitForSelector('nav', { timeout: 15_000 })
   })
   ```
4. Run `npm run test:e2e:headed` to see the test in the browser while developing

### CI integration

The test suite is designed for CI — just add this step after `npm install`:

```yaml
# GitHub Actions example
- name: Install Playwright browsers
  run: npx playwright install chromium --with-deps
  working-directory: frontend

- name: Run E2E tests
  run: npm run test:e2e
  working-directory: frontend

- name: Upload Playwright report
  uses: actions/upload-artifact@v4
  if: failure()
  with:
    name: playwright-report
    path: frontend/playwright-report/
```

---

## Backend Integration Tests

### Prerequisites

- Docker or Podman running (Testcontainers spins up its own PostgreSQL)
- No other services need to be running

### Run commands

```bash
# From project root

# All tests (unit + integration) — takes ~3 minutes
mvn test

# Skip integration tests (unit only — fast)
mvn test -Dtest='!*IntegrationTest,!*IT'

# Single test class
mvn test -Dtest=RagOrchestratorTest

# Single test method
mvn test -Dtest=DocumentServiceTest#uploadDocument_shouldIndexSuccessfully
```

### Test configuration

Integration tests use [`src/test/resources/application-test.yml`](../src/test/resources/application-test.yml) which overrides:

- `datasource.url` → Testcontainers PostgreSQL (spun up automatically)
- `flyway.clean-disabled: false` → allows schema reset between test runs
- Debug logging enabled for `com.enterprise.rag` and Spring Security

### Test profiles

Tests annotated with `@SpringBootTest` start the full application context with Testcontainers. The `@ActiveProfiles("test")` annotation picks up `application-test.yml` automatically.

---

## Testing the Live Stack Manually

When the full stack is running (`podman compose up -d`), you can test the API directly using the scripts in `infra/`:

```bash
# Run inside the rag-app container (has access to internal Keycloak hostname)
podman exec -it rag-app sh infra/test-api.sh    # list docs, create conversation, RAG query, LLM demo
podman exec -it rag-app sh infra/test-auth.sh   # verify JWT token flow through Nginx proxy
podman exec -it rag-app sh infra/test-upload.sh # upload a test document
```

Or from the host using PowerShell (see [RUNBOOK.md — Useful Commands](../RUNBOOK.md#11-useful-commands)):

```powershell
# Get token (rag-app client supports password grant for API testing)
$token = (Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8180/realms/enterprise-rag/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" `
  -Body "grant_type=password&client_id=rag-app&username=admin@acme.com&password=Admin@1234"
).access_token

# RAG query
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8090/api/v1/rag/query" `
  -Headers @{ Authorization = "Bearer $token"; "Content-Type" = "application/json" } `
  -Body '{"question":"What is covered under this policy?"}'
```

> **Why use `rag-app` and not `rag-frontend` for API testing?**  
> `rag-frontend` is a browser PKCE client — `directAccessGrantsEnabled` is intentionally `false`.  
> `rag-app` is the backend service client — it has `directAccessGrantsEnabled: true` for programmatic access.  
> The browser always uses the PKCE redirect flow via Keycloak at `http://localhost:8180`. See [RUNBOOK.md — Issue #2](../RUNBOOK.md#issue-2-keycloak-direct_access_grants-disabled-for-rag-frontend-client).
