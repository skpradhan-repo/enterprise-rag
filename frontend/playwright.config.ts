import { defineConfig, devices } from '@playwright/test'

/**
 * Playwright configuration for Enterprise RAG frontend.
 *
 * Tests run against the local Vite dev server (http://localhost:5173).
 * The dev server is started automatically before the tests if not already running.
 *
 * Run all tests:     npm run test:e2e
 * Run with UI:       npm run test:e2e:ui
 * Run headed:        npm run test:e2e:headed
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,

  reporter: [
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['list'],
  ],

  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  /**
   * Start the Vite dev server automatically when running tests.
   * Remove this block if you want to start the server manually first.
   */
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: true,
    timeout: 30_000,
  },
})
