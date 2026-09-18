import { test, expect } from '@playwright/test'
import { mockAuthenticatedUser, mockApiRoutes } from './helpers/auth'

/**
 * Navigation tests — verifies the nav bar renders correctly
 * and all page routes are reachable.
 */
test.describe('Navigation', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthenticatedUser(page)
    await mockApiRoutes(page)
    await page.goto('/')
    // Wait until "Authenticating…" spinner disappears
    await page.waitForSelector('nav', { timeout: 15_000 })
  })

  test('renders nav bar with app title', async ({ page }) => {
    await expect(page.getByText('Enterprise RAG')).toBeVisible()
  })

  test('shows nav links for all three sections', async ({ page }) => {
    await expect(page.getByRole('link', { name: 'RAG Chat' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Documents' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'LLM Demo' })).toBeVisible()
  })

  test('shows authenticated user email and tenant', async ({ page }) => {
    await expect(page.getByText('testuser@example.com')).toBeVisible()
    await expect(page.getByText('demo-tenant')).toBeVisible()
  })

  test('shows Logout button', async ({ page }) => {
    await expect(page.getByRole('button', { name: 'Logout' })).toBeVisible()
  })

  test('navigates to Documents page via nav link', async ({ page }) => {
    await page.getByRole('link', { name: 'Documents' }).click()
    await expect(page).toHaveURL(/\/documents/)
    await expect(page.getByRole('heading', { name: 'Documents' })).toBeVisible()
  })

  test('navigates to LLM Demo page via nav link', async ({ page }) => {
    await page.getByRole('link', { name: 'LLM Demo' }).click()
    await expect(page).toHaveURL(/\/llm-demo/)
    await expect(page.getByRole('heading', { name: 'LLM Client / Server Demo' })).toBeVisible()
  })

  test('navigates back to Chat page via RAG Chat link', async ({ page }) => {
    await page.getByRole('link', { name: 'Documents' }).click()
    await page.getByRole('link', { name: 'RAG Chat' }).click()
    await expect(page).toHaveURL('/')
    await expect(page.getByRole('heading', { name: 'RAG Chat' })).toBeVisible()
  })
})
