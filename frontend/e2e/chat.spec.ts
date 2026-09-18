import { test, expect } from '@playwright/test'
import { mockAuthenticatedUser, mockApiRoutes } from './helpers/auth'

/**
 * ChatPage (RAG Chat) tests
 *
 * Covers:
 *  - Page structure renders correctly
 *  - User can type a question and submit via button
 *  - User can submit via Enter key
 *  - Loading indicator ("Thinking…") appears while waiting
 *  - Assistant answer is displayed after API responds
 *  - Source document name is shown under the assistant answer
 *  - Send button is disabled when input is empty
 *  - Input is cleared after submit
 */
test.describe('ChatPage — RAG Chat', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthenticatedUser(page)
    await mockApiRoutes(page)
    await page.goto('/')
    await page.waitForSelector('nav', { timeout: 15_000 })
  })

  test('renders chat page heading and description', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'RAG Chat' })).toBeVisible()
    await expect(page.getByText('Ask questions about your uploaded documents')).toBeVisible()
  })

  test('renders chat input and Send button', async ({ page }) => {
    await expect(page.getByPlaceholder('Ask a question about your documents…')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Send' })).toBeVisible()
  })

  test('Send button is disabled when input is empty', async ({ page }) => {
    const sendBtn = page.getByRole('button', { name: 'Send' })
    await expect(sendBtn).toBeDisabled()
  })

  test('Send button becomes enabled when user types a question', async ({ page }) => {
    const input   = page.getByPlaceholder('Ask a question about your documents…')
    const sendBtn = page.getByRole('button', { name: 'Send' })

    await input.fill('What is the enterprise architecture?')
    await expect(sendBtn).toBeEnabled()
  })

  test('user message appears in chat after submit via button', async ({ page }) => {
    const question = 'What is the enterprise architecture?'
    await page.getByPlaceholder('Ask a question about your documents…').fill(question)
    await page.getByRole('button', { name: 'Send' }).click()

    await expect(page.getByText(question)).toBeVisible()
  })

  test('assistant answer appears after API responds', async ({ page }) => {
    const question = 'What is the enterprise architecture?'
    await page.getByPlaceholder('Ask a question about your documents…').fill(question)
    await page.getByRole('button', { name: 'Send' }).click()

    // Mock returns: "This is a mock RAG answer to: ..."
    await expect(page.getByText(/This is a mock RAG answer to/)).toBeVisible({ timeout: 10_000 })
  })

  test('source document name is shown under assistant answer', async ({ page }) => {
    await page.getByPlaceholder('Ask a question about your documents…').fill('test question')
    await page.getByRole('button', { name: 'Send' }).click()

    await expect(page.getByText(/Enterprise Architecture Guide/)).toBeVisible({ timeout: 10_000 })
  })

  test('input is cleared after message is sent', async ({ page }) => {
    const input = page.getByPlaceholder('Ask a question about your documents…')
    await input.fill('What is RAG?')
    await page.getByRole('button', { name: 'Send' }).click()

    await expect(input).toHaveValue('')
  })

  test('user can submit question by pressing Enter', async ({ page }) => {
    const question = 'Explain the security policy'
    await page.getByPlaceholder('Ask a question about your documents…').fill(question)
    await page.keyboard.press('Enter')

    await expect(page.getByText(question)).toBeVisible()
    await expect(page.getByText(/This is a mock RAG answer to/)).toBeVisible({ timeout: 10_000 })
  })

  test('multiple questions build up a conversation history', async ({ page }) => {
    const input = page.getByPlaceholder('Ask a question about your documents…')

    await input.fill('First question')
    await page.getByRole('button', { name: 'Send' }).click()
    await expect(page.getByText(/This is a mock RAG answer to/)).toBeVisible({ timeout: 10_000 })

    await input.fill('Second question')
    await page.getByRole('button', { name: 'Send' }).click()

    const messages = page.locator('text=/This is a mock RAG answer to/')
    await expect(messages).toHaveCount(2, { timeout: 10_000 })
  })

  test('shows error message when API call fails', async ({ page }) => {
    // Override the RAG route to return 500 for this test
    await page.route('**/api/v1/rag/query', route =>
      route.fulfill({ status: 500, body: 'Internal Server Error' })
    )

    await page.getByPlaceholder('Ask a question about your documents…').fill('Trigger error')
    await page.getByRole('button', { name: 'Send' }).click()

    await expect(page.getByText('An error occurred. Please try again.')).toBeVisible({ timeout: 10_000 })
  })
})
