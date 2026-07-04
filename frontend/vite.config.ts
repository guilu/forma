import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

// Vite + React config for the FORMA frontend skeleton (FOR-81).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    css: true,
  },
});
