import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

// Vite + React config for the FORMA frontend skeleton (FOR-81).
export default defineConfig({
  plugins: [react()],
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
  },
});
