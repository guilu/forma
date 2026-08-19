import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import { devApiFixtures } from './devApiFixtures';

/*
 * Declared here rather than pulled in with `@types/node`, for the reason
 * playwright.config.ts gives: that package has no `types` fence, so installing
 * it would make Node's globals visible to the application sources too, and
 * `process.env` compiling inside a browser bundle is a trap worth not setting.
 */
declare const process: { readonly env: Record<string, string | undefined> };

// Vite + React config for the FORMA frontend skeleton (FOR-81).
export default defineConfig({
  /*
   * The fixture server is opt-in and dev-only (`npm run dev:fixtures`): a plain
   * `npm run dev` keeps proxying to a real backend, and a build never sees it.
   */
  plugins: [react(), ...(process.env.FIXTURES === '1' ? [devApiFixtures()] : [])],
  server: {
    port: 5173,
    // The app calls relative `/api/...` (same-origin). In dev, proxy those to the
    // local backend so no absolute backend host is baked into the bundle. Change
    // the target here if the backend runs on a different port/host.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    css: true,
    /*
     * Only the jsdom suite. `e2e/` holds Playwright specs (see
     * playwright.config.ts): they import from `@playwright/test`, so Vitest
     * collecting them by its default glob fails the run outright.
     */
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
  },
});
