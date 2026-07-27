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

/**
 * jsdom does not implement `window.matchMedia` (FOR-62, first story to read
 * `prefers-color-scheme`) — calling it throws "not implemented" instead of
 * returning a `MediaQueryList`. This default resolves to "no match" so any
 * incidental call (and every test that doesn't care about theme) stays safe
 * and consistent with the "no system signal → dark default" edge case.
 * Theme tests that need a specific system preference replace
 * `window.matchMedia` themselves and restore it afterwards.
 */
function ensureMatchMedia() {
  if (typeof window.matchMedia === 'function') {
    return;
  }
  window.matchMedia = (query: string) =>
    ({
      matches: false,
      media: query,
      onchange: null,
      addEventListener: () => {},
      removeEventListener: () => {},
      addListener: () => {},
      removeListener: () => {},
      dispatchEvent: () => false,
    }) as MediaQueryList;
}

ensureMatchMedia();

/**
 * jsdom does not implement `URL.createObjectURL`/`URL.revokeObjectURL` (FOR-144,
 * first story to render a photo via an authenticated-fetch → object URL). This
 * default returns a stable, distinguishable `blob:mock-N` string per call so
 * component tests can assert an `<img>`'s `src` without a real Blob storage
 * backend; `revokeObjectURL` is a no-op. Tests that need to assert revocation
 * itself spy on `URL.revokeObjectURL` directly.
 */
function ensureObjectUrl() {
  if (typeof URL.createObjectURL === 'function') {
    return;
  }
  let counter = 0;
  Object.defineProperty(URL, 'createObjectURL', {
    value: () => `blob:mock-${counter++}`,
    configurable: true,
    writable: true,
  });
  Object.defineProperty(URL, 'revokeObjectURL', {
    value: () => {},
    configurable: true,
    writable: true,
  });
}

ensureObjectUrl();

/**
 * jsdom performs no layout, so every element measures 0x0 and has no
 * `ResizeObserver` at all. Recharts' `ResponsiveContainer` (ADR-013) asks its
 * parent for a size and refuses to draw anything at zero, which would make
 * every chart invisible to component tests.
 *
 * These two shims give the tree a plausible, fixed size instead: a container
 * that reports 600x300 and an observer that reports that same box once, on
 * subscribe. They are deliberately global rather than per-test — no test
 * asserts on real geometry (jsdom cannot produce any), so a constant size is
 * strictly more truthful than the 0 it would otherwise see.
 */
const CHART_TEST_WIDTH = 600;
const CHART_TEST_HEIGHT = 300;

function ensureLayoutSize() {
  for (const [property, value] of [
    ['offsetWidth', CHART_TEST_WIDTH],
    ['offsetHeight', CHART_TEST_HEIGHT],
    ['clientWidth', CHART_TEST_WIDTH],
    ['clientHeight', CHART_TEST_HEIGHT],
  ] as const) {
    Object.defineProperty(HTMLElement.prototype, property, {
      configurable: true,
      value,
    });
  }
}

function ensureResizeObserver() {
  if (typeof window.ResizeObserver === 'function') {
    return;
  }
  window.ResizeObserver = class {
    constructor(private readonly callback: ResizeObserverCallback) {}

    observe(target: Element) {
      this.callback(
        [
          {
            target,
            contentRect: {
              width: CHART_TEST_WIDTH,
              height: CHART_TEST_HEIGHT,
              top: 0,
              left: 0,
              bottom: CHART_TEST_HEIGHT,
              right: CHART_TEST_WIDTH,
              x: 0,
              y: 0,
              toJSON: () => ({}),
            } as DOMRectReadOnly,
          } as ResizeObserverEntry,
        ],
        this as unknown as ResizeObserver,
      );
    }

    unobserve() {}

    disconnect() {}
  } as unknown as typeof ResizeObserver;
}

ensureLayoutSize();
ensureResizeObserver();

afterEach(() => {
  cleanup();
});
