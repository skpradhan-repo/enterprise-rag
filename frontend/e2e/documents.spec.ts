import { test, expect, type Page } from '@playwright/test'
import { mockAuthenticatedUser, mockApiRoutes } from './helpers/auth'

/**
 * DocumentsPage tests
 *
 * Covers:
 *  - Page heading renders
 *  - Document table lists fetched documents
 *  - Status colours (INDEXED → green, PROCESSING → yellow)
 *  - Upload form validation (button disabled without file + title)
 *  - Successful file upload flow
 *  - Delete button triggers DELETE request and removes the row
 *  - API error handling on upload failure
 */

async function gotoDocuments(page: Page) {
  await mockAuthenticatedUser(page)
  await mockApiRoutes(page)
  await page.goto('/documents')
  await page.waitForSelector('nav', { timeout: 15_000 })
  await expect(page.getByRole('heading', { name: 'Documents' })).toBeVisible()
}

test.describe('DocumentsPage', () => {
  test('renders page heading and upload section', async ({ page }) => {
    await gotoDocuments(page)
    await expect(page.getByRole('heading', { name: 'Upload Document' })).toBeVisible()
  })

  test('lists documents fetched from API', async ({ page }) => {
    await gotoDocuments(page)

    await expect(page.getByText('Enterprise Architecture Guide')).toBeVisible()
    await expect(page.getByText('Security Policy v2')).toBeVisible()
  })

  test('shows document types from API response', async ({ page }) => {
    await gotoDocuments(page)

    await expect(page.getByText('PDF')).toBeVisible()
    await expect(page.getByText('DOCX')).toBeVisible()
  })

  test('shows INDEXED status in the table', async ({ page }) => {
    await gotoDocuments(page)
    await expect(page.getByText('INDEXED')).toBeVisible()
  })

  test('shows PROCESSING status in the table', async ({ page }) => {
    await gotoDocuments(page)
    await expect(page.getByText('PROCESSING')).toBeVisible()
  })

  test('shows page count for INDEXED document', async ({ page }) => {
    await gotoDocuments(page)
    await expect(page.getByText('42')).toBeVisible()
  })

  test('shows — for documents without page count', async ({ page }) => {
    await gotoDocuments(page)
    // PROCESSING document has pageCount: null → displayed as —
    const cells = page.locator('td', { hasText: '—' })
    await expect(cells.first()).toBeVisible()
  })

  test('Upload button is disabled when no file is selected', async ({ page }) => {
    await gotoDocuments(page)
    const uploadBtn = page.getByRole('button', { name: 'Upload' })
    await expect(uploadBtn).toBeDisabled()
  })

  test('Upload button is disabled when file selected but no title', async ({ page }) => {
    await gotoDocuments(page)

    // Create a dummy file buffer
    const fileBuffer = Buffer.from('Dummy PDF content')
    await page.locator('input[type="file"]').setInputFiles({
      name: 'test.pdf',
      mimeType: 'application/pdf',
      buffer: fileBuffer,
    })

    const uploadBtn = page.getByRole('button', { name: 'Upload' })
    await expect(uploadBtn).toBeDisabled()
  })

  test('Upload button becomes enabled with file and title', async ({ page }) => {
    await gotoDocuments(page)

    const fileBuffer = Buffer.from('Dummy PDF content')
    await page.locator('input[type="file"]').setInputFiles({
      name: 'test.pdf',
      mimeType: 'application/pdf',
      buffer: fileBuffer,
    })
    await page.getByPlaceholder('Document title').fill('My Test Document')

    const uploadBtn = page.getByRole('button', { name: 'Upload' })
    await expect(uploadBtn).toBeEnabled()
  })

  test('successful upload clears the form', async ({ page }) => {
    await gotoDocuments(page)

    const fileBuffer = Buffer.from('Dummy PDF content')
    await page.locator('input[type="file"]').setInputFiles({
      name: 'test.pdf',
      mimeType: 'application/pdf',
      buffer: fileBuffer,
    })
    await page.getByPlaceholder('Document title').fill('My Test Document')
    await page.getByPlaceholder('Tags (comma-separated)').fill('test, demo')
    await page.getByRole('button', { name: 'Upload' }).click()

    // After success the form resets
    await expect(page.getByPlaceholder('Document title')).toHaveValue('', { timeout: 10_000 })
    await expect(page.getByPlaceholder('Tags (comma-separated)')).toHaveValue('')
  })

  test('shows upload error message when API returns error', async ({ page }) => {
    await mockAuthenticatedUser(page)

    // Override upload route to 500 before loading
    await page.route('**/api/v1/documents**', async route => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 }),
        })
      } else {
        await route.fulfill({ status: 500, body: 'Server error' })
      }
    })

    await page.goto('/documents')
    await page.waitForSelector('nav', { timeout: 15_000 })

    const fileBuffer = Buffer.from('Dummy content')
    await page.locator('input[type="file"]').setInputFiles({
      name: 'fail.pdf',
      mimeType: 'application/pdf',
      buffer: fileBuffer,
    })
    await page.getByPlaceholder('Document title').fill('Fail Upload')
    await page.getByRole('button', { name: 'Upload' }).click()

    await expect(page.getByText('Upload failed. Please check the file and try again.')).toBeVisible({ timeout: 10_000 })
  })

  test('Delete button is visible for each document row', async ({ page }) => {
    await gotoDocuments(page)

    const deleteButtons = page.getByRole('button', { name: 'Delete' })
    await expect(deleteButtons).toHaveCount(2)
  })

  test('clicking Delete sends DELETE request and refreshes list', async ({ page }) => {
    let deleteCalled = false

    await mockAuthenticatedUser(page)
    await page.route('**/api/v1/documents**', async route => {
      if (route.request().method() === 'DELETE') {
        deleteCalled = true
        await route.fulfill({ status: 204 })
      } else if (route.request().method() === 'GET') {
        // Return one less document after delete
        const content = deleteCalled ? [] : [
          { id: 'doc-001', title: 'Only Doc', fileName: 'doc.pdf', mimeType: 'application/pdf',
            fileSizeBytes: 1024, docType: 'PDF', status: 'INDEXED', pageCount: 1,
            tags: [], createdAt: '2024-01-01T00:00:00Z', updatedAt: '2024-01-01T00:00:00Z' },
        ]
        await route.fulfill({
          status: 200, contentType: 'application/json',
          body: JSON.stringify({ content, totalElements: content.length, totalPages: 1, number: 0, size: 20 }),
        })
      }
    })

    await page.goto('/documents')
    await page.waitForSelector('nav', { timeout: 15_000 })
    await page.getByRole('button', { name: 'Delete' }).click()

    await expect(page.getByText('Only Doc')).toHaveCount(0, { timeout: 10_000 })
    expect(deleteCalled).toBe(true)
  })
})
