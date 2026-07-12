// Vitest global setup (FOR-81): extends expect with jest-dom matchers and
// cleans up the DOM between tests.
import '@testing-library/jest-dom/vitest';
import { afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';

/**
 * Guard against Node's built-in `localStorage` global (stable since Node 22,
 * file-backed) leaking into jsdom's `window` and shadowing jsdom's own
 * working implementation (FOR-59, first story to persist to localStorage).
 * Observed on Node 25 without a configured `--localstorage-file`: the global
 * resolves to a non-functional stub (`typeof window.localStorage.clear`
 * is `undefined`) instead of jsdom's `Storage`. Falling back to a minimal
 * in-memory polyfill here keeps `localStorage`-dependent tests stable
 * regardless of which Node version runs them, without touching product code
 * or requiring a `NODE_OPTIONS` flag in every environment.
 */
function ensureWorkingLocalStorage() {
  const current = window.localStorage;
  if (current && typeof current.clear === 'function' && typeof current.setItem === 'function') {
    return;
  }
  const store = new Map<string, string>();
  const polyfill: Storage = {
    get length() {
      return store.size;
    },
    clear: () => store.clear(),
    getItem: (key) => (store.has(key) ? (store.get(key) ?? null) : null),
    key: (index) => Array.from(store.keys())[index] ?? null,
    removeItem: (key) => {
      store.delete(key);
    },
    setItem: (key, value) => {
      store.set(key, String(value));
    },
  };
  Object.defineProperty(window, 'localStorage', {
    value: polyfill,
    configurable: true,
    writable: true,
  });
}

ensureWorkingLocalStorage();

afterEach(() => {
  cleanup();
});
