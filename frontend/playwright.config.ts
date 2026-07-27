import { defineConfig, devices } from '@playwright/test';

/*
 * Declared here rather than pulled in with `@types/node`: that package has no
 * `types` fence around it, so installing it would make Node's globals visible
 * to the application sources too, and `process.env` compiling inside a browser
 * bundle is a trap worth not setting.
 */
declare const process: { readonly env: Record<string, string | undefined> };

/**
 * Layout checks (FOR-186). These exist because the unit suite runs in jsdom,
 * which performs no layout at all: it cannot tell whether an element overflows
 * its container, whether two scrollbars appeared, or whether a fill is
 * actually translucent. Every one of those has shipped as a visual bug that
 * only a browser could have caught.
 *
 * Deliberately narrow: this is not a second functional test suite. It asserts
 * geometry and computed style on a handful of routes, and the API is stubbed
 * (see `e2e/stubApi.ts`) so no backend is needed and the pages are the same on
 * every run.
 */
export default defineConfig({
  testDir: './e2e',
  // Layout is measured, not raced: a retry that "fixes" a geometry assertion
  // would be hiding a real flake in the page.
  retries: 0,
  fullyParallel: true,
  reporter: process.env.CI ? 'github' : 'list',
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
    stdout: 'ignore',
  },
});
