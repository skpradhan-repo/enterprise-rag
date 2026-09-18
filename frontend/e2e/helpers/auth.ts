/**
 * Auth helpers for Playwright tests.
 *
 * The app requires Keycloak PKCE authentication. These helpers intercept
 * the keycloak-js module so tests run without a live Keycloak server.
 *
 * Strategy:
 *   1. Intercept the GET /api/v1/* requests via page.route() to return mock data.
 *   2. Override the `Keycloak` constructor via addInitScript so AuthProvider
 *      resolves immediately with `authenticated = true`.
 */
import { type Page } from '@playwright/test'

export interface MockUser {
  email?: string
  tenantId?: string
  roles?: string[]
}

/**
 * Injects a mock Keycloak instance before the page script runs.
 * Must be called before page.goto().
 */
export async function mockAuthenticatedUser(page: Page, user: MockUser = {}) {
  const email    = user.email    ?? 'testuser@example.com'
  const tenantId = user.tenantId ?? 'demo-tenant'
  const roles    = user.roles    ?? ['user']

  await page.addInitScript(({ email, tenantId, roles }: MockUser) => {
    const payload = btoa(JSON.stringify({
      email,
      tenant_id: tenantId,
      realm_access: { roles },
      exp: Math.floor(Date.now() / 1000) + 3600,
      sub: 'mock-user-id',
    }))
    const fakeToken = `eyJhbGciOiJSUzI1NiJ9.${payload}.mock-signature`

    // Override the default export of keycloak-js before any module uses it.
    // Vite bundles keycloak-js as an ES module; we patch window.__kc_mock__
    // and the AuthProvider's keycloak.ts picks it up via the init script.
    // We also replace the global constructor so the module-level `new Keycloak()`
    // call in keycloak.ts returns our stub.
    const stub = {
      token: fakeToken,
      tokenParsed: JSON.parse(atob(payload)),
      realmAccess: { roles },
      authenticated: true,
      init: () => Promise.resolve(true),
      updateToken: () => Promise.resolve(true),
      logout: () => {},
    }

    // Expose the stub globally so the Vite-bundled keycloak module can be
    // intercepted at the window level before the app module evaluates.
    ;(window as unknown as Record<string, unknown>)['__MOCK_KC__'] = stub
  }, { email, tenantId, roles })

  // Intercept the keycloak-js ES module request and replace it with a stub
  await page.route('**/keycloak-js/**', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/javascript',
      body: `
        const stub = window['__MOCK_KC__'] || {
          token: 'mock', tokenParsed: {}, realmAccess: { roles: [] },
          init: () => Promise.resolve(true),
          updateToken: () => Promise.resolve(true),
          logout: () => {},
        };
        export default function Keycloak() { return stub; }
      `,
    })
  })
}

/** Mock all backend API routes with realistic fixture data. */
export async function mockApiRoutes(page: Page) {
  // GET /api/v1/documents
  await page.route('**/api/v1/documents**', async route => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [
            {
              id: 'doc-001',
              title: 'Enterprise Architecture Guide',
              fileName: 'arch-guide.pdf',
              mimeType: 'application/pdf',
              fileSizeBytes: 204800,
              docType: 'PDF',
              status: 'INDEXED',
              pageCount: 42,
              tags: ['architecture', 'enterprise'],
              createdAt: '2024-01-15T10:00:00Z',
              updatedAt: '2024-01-15T10:30:00Z',
            },
            {
              id: 'doc-002',
              title: 'Security Policy v2',
              fileName: 'security-policy.docx',
              mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
              fileSizeBytes: 51200,
              docType: 'DOCX',
              status: 'PROCESSING',
              pageCount: null,
              tags: ['security', 'policy'],
              createdAt: '2024-01-16T08:00:00Z',
              updatedAt: '2024-01-16T08:05:00Z',
            },
          ],
          totalElements: 2,
          totalPages: 1,
          number: 0,
          size: 20,
        }),
      })
    } else if (route.request().method() === 'DELETE') {
      await route.fulfill({ status: 204 })
    } else {
      // POST (upload)
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'doc-003',
          title: 'New Document',
          fileName: 'new-doc.pdf',
          mimeType: 'application/pdf',
          fileSizeBytes: 1024,
          docType: 'PDF',
          status: 'UPLOADED',
          pageCount: null,
          tags: [],
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }),
      })
    }
  })

  // POST /api/v1/rag/query
  await page.route('**/api/v1/rag/query', async route => {
    const body = JSON.parse(route.request().postData() ?? '{}')
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        answer: `This is a mock RAG answer to: "${body.question ?? 'your question'}"`,
        conversationId: 'conv-mock-123',
        correlationId: 'corr-mock-456',
        model: 'llama3.2',
        sources: [
          {
            documentId: 'doc-001',
            documentName: 'Enterprise Architecture Guide',
            pageNumber: 7,
            chunkId: 'chunk-001',
            similarity: 0.92,
            excerpt: 'Relevant excerpt from the document...',
          },
        ],
        latencyMs: 420,
        respondedAt: new Date().toISOString(),
      }),
    })
  })

  // POST /api/v1/llm/demo
  await page.route('**/api/v1/llm/demo', async route => {
    const body = JSON.parse(route.request().postData() ?? '{}')
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        response: `Mock LLM response to prompt: "${body.prompt ?? 'your prompt'}". This demonstrates direct Ollama communication.`,
        model: 'llama3.2',
        provider: 'ollama',
        latencyMs: 312,
      }),
    })
  })
}
