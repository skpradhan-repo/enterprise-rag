import { test, expect } from '@playwright/test'
import { mockAuthenticatedUser, mockApiRoutes } from './helpers/auth'

/**
 * LlmDemoPage tests
 *
 * Covers:
 *  - Page heading and description render
 *  - Architecture diagram text is visible
 *  - Textarea is present and accepts input
 *  - "Send to Ollama" button is disabled when prompt is empty
 *  - Button becomes enabled when user types a prompt
 *  - API is called with the correct prompt text
 *  - LLM response is rendered with model, provider, latency metadata
 *  - Button shows "Calling LLM…" while request is in flight
 *  - Error message shown when API call fails
 */

async function gotoLlmDemo(page: Page) {
  await mockAuthenticatedUser(page)
  await mockApiRoutes(page)
  await page.goto('/llm-demo')
  await page.waitForSelector('nav', { timeout: 15_000 })
  await expect(page.getByRole('heading', { name: 'LLM Client / Server Demo' })).toBeVisible()
}

test.describe('LlmDemoPage — LLM Demo', () => {
  test('renders page heading', async ({ page }) => {
    await gotoLlmDemo(page)
    await expect(page.getByRole('heading', { name: 'LLM Client / Server Demo' })).toBeVisible()
  })

  test('renders description text about architecture', async ({ page }) => {
    await gotoLlmDemo(page)
    await expect(page.getByText('Spring AI ChatClient')).toBeVisible()
    await expect(page.getByText('Ollama LLM Server')).toBeVisible()
  })

  test('renders architecture diagram text', async ({ page }) => {
    await gotoLlmDemo(page)
    await expect(page.getByText(/React → Spring Boot/)).toBeVisible()
  })

  test('renders prompt textarea', async ({ page }) => {
    await gotoLlmDemo(page)
    await expect(page.getByPlaceholder("Enter any prompt (e.g. 'Explain RAG in 3 sentences')")).toBeVisible()
  })

  test('"Send to Ollama" button is disabled when prompt is empty', async ({ page }) => {
    await gotoLlmDemo(page)
    await expect(page.getByRole('button', { name: 'Send to Ollama' })).toBeDisabled()
  })

  test('"Send to Ollama" button becomes enabled when user types', async ({ page }) => {
    await gotoLlmDemo(page)
    await page.getByPlaceholder("Enter any prompt (e.g. 'Explain RAG in 3 sentences')").fill('Explain RAG')
    await expect(page.getByRole('button', { name: 'Send to Ollama' })).toBeEnabled()
  })

  test('clicking Send calls the LLM API and displays the response', async ({ page }) => {
    await gotoLlmDemo(page)

    await page.getByPlaceholder("Enter any prompt (e.g. 'Explain RAG in 3 sentences')").fill('Explain RAG in 3 sentences')
    await page.getByRole('button', { name: 'Send to Ollama' }).click()

    // Mock returns "Mock LLM response to prompt: ..."
    await expect(page.getByText(/Mock LLM response to prompt/)).toBeVisible({ timeout: 10_000 })
  })

  test('displays model name in the response metadata', async ({ page }) => {
    await gotoLlmDemo(page)

    await page.getByPlaceholder("Enter any prompt (e.g. 'Explain RAG in 3 sentences')").fill('Hello LLM')
    await page.getByRole('button', { name: 'Send to Ollama' }).click()

    await expect(page.getByText('llama3.2')).toBeVisible({ timeout: 10_000 })
  })

  test('displays provider in the response metadata', async ({ page }) => {
    await gotoLlmDemo(page)

    await page.getByPlaceholder("Enter any prompt (e.g. 'Explain RAG in 3 sentences')").fill('Hello')
    await page.getByRole('button', { name: 'Send to Ollama' }).click()

    await expect(page.getByText('ollama')).toBeVisible({ timeout: 10_000 })
  })

  test('displays latency in the response metadata', async ({ page }) => {
    await gotoLlmDemo(page)

    await page.getByPlaceholder("Enter any prompt (e.g. 'Explain RAG in 3 sentences')").fill('Hello')
    await page.getByRole('button', { name: 'Send to Ollama' }).click()

    // Mock returns latencyMs: 312
    await expect(page.getByText(/312ms/)).toBeVisible({ timeout: 10_000 })
  })

  test('sends the exact prompt text to the API', async ({ page }) => {
    await mockAuthenticatedUser(page)

    let capturedBody: Record<string, unknown> = {}
    await page.route('**/api/v1/llm/demo', async route => {
      capturedBody = JSON.parse(route.request().postData() ?? '{}')
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({ response: 'ok', model: 'llama3.2', provider: 'ollama', latencyMs: 100 }),
      })
    })

    await page.goto('/llm-demo')
    await page.waitForSelector('nav', { timeout: 15_000 })

    const promptText = 'Exactly this prompt please'
    await page.getByPlaceholder("Enter any prompt (e.g. 'Explain RAG in 3 sentences')").fill(promptText)
    await page.getByRole('button', { name: 'Send to Ollama' }).click()

    await expect(page.getByText('ok')).toBeVisible({ timeout: 10_000 })
    expect(capturedBody.prompt).toBe(promptText)
  })

  test('shows loading state "Calling LLM…" while request is in flight', async ({ page }) => {
    await mockAuthenticatedUser(page)

    // Delay the response so we can assert the loading state
    await page.route('**/api/v1/llm/demo', async route => {
      await new Promise(resolve => setTimeout(resolve, 800))
      await route.fulfill({
        status: 200, contentType: 'application/json',
        body: JSON.stringify({ response: 'Delayed response', model: 'llama3.2', provider: 'ollama', latencyMs: 800 }),
      })
    })

    await page.goto('/llm-demo')
    await page.waitForSelector('nav', { timeout: 15_000 })

    await page.getByPlaceholder("Enter any prompt (e.g. 'Explain RAG in 3 sentences')").fill('test slow')
    await page.getByRole('button', { name: 'Send to Ollama' }).click()

    await expect(page.getByRole('button', { name: 'Calling LLM…' })).toBeVisible()
    await expect(page.getByText('Delayed response')).toBeVisible({ timeout: 5_000 })
  })

  test('shows error message when API call fails', async ({ page }) => {
    await mockAuthenticatedUser(page)

    await page.route('**/api/v1/llm/demo', route =>
      route.fulfill({ status: 503, body: 'Service Unavailable' })
    )

    await page.goto('/llm-demo')
    await page.waitForSelector('nav', { timeout: 15_000 })

    await page.getByPlaceholder("Enter any prompt (e.g. 'Explain RAG in 3 sentences')").fill('Trigger error')
    await page.getByRole('button', { name: 'Send to Ollama' }).click()

    await expect(page.getByText('LLM call failed. Is Ollama running?')).toBeVisible({ timeout: 10_000 })
  })

  test('previous response is cleared when a new prompt is sent', async ({ page }) => {
    await gotoLlmDemo(page)
    const textarea = page.getByPlaceholder("Enter any prompt (e.g. 'Explain RAG in 3 sentences')")

    await textarea.fill('First prompt')
    await page.getByRole('button', { name: 'Send to Ollama' }).click()
    await expect(page.getByText(/Mock LLM response to prompt/)).toBeVisible({ timeout: 10_000 })

    await textarea.fill('Second prompt')
    await page.getByRole('button', { name: 'Send to Ollama' }).click()

    // Only one response block visible at a time
    const responses = page.locator('text=/Mock LLM response to prompt/')
    await expect(responses).toHaveCount(1, { timeout: 10_000 })
  })
})
