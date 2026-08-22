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
  build: {
    /*
     * Never inline the anatomy muscle masks.
     *
     * `MuscleSilhouette` reaches its pack with an eager `import.meta.glob`, so
     * whichever chunk imports the component carries a reference to all 42 mask
     * files. Each one is a couple of kB — comfortably under the default 4 kB
     * inline threshold — so Vite turned every one of them into a data URI baked
     * into that chunk. Harmless while the component only ran on a lazily loaded
     * training route; not harmless once the public landing put the overlay in
     * its hero, where it added ~63 kB (~16 kB gzipped) to the entry chunk that
     * blocks first paint.
     *
     * Emitted as files instead, the chunk carries 42 short URLs and the browser
     * fetches only the handful of masks a given view actually draws. Returning
     * `undefined` for everything else keeps Vite's default behaviour, so this is
     * a rule about one directory rather than a change of policy.
     */
    assetsInlineLimit: (filePath: string) =>
      /\/assets\/anatomy\/.+\.svg$/.test(filePath) ? false : undefined,
  },
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
