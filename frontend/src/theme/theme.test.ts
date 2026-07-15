import { afterEach, describe, expect, it } from 'vitest';
import {
  DEFAULT_THEME_MODE,
  applyResolvedTheme,
  fromApiThemeMode,
  readStoredThemeMode,
  resolveTheme,
  storeThemeMode,
  systemPrefersLight,
  toApiThemeMode,
  THEME_STORAGE_KEY,
} from './theme';

/** Replaces `window.matchMedia` for a single test; always restored after. */
function stubMatchMedia(matches: boolean) {
  const original = window.matchMedia;
  window.matchMedia = (query: string) =>
    ({
      matches,
      media: query,
      onchange: null,
      addEventListener: () => {},
      removeEventListener: () => {},
      addListener: () => {},
      removeListener: () => {},
      dispatchEvent: () => false,
    }) as MediaQueryList;
  return () => {
    window.matchMedia = original;
  };
}

describe('theme resolution + persistence (FOR-62)', () => {
  afterEach(() => {
    window.localStorage.clear();
  });

  it('defaults to "system" as the starting mode', () => {
    expect(DEFAULT_THEME_MODE).toBe('system');
  });

  it('resolves "system" to dark when there is no system signal (jsdom default)', () => {
    expect(systemPrefersLight()).toBe(false);
    expect(resolveTheme('system')).toBe('dark');
  });

  it('resolves "system" to light when the OS prefers light', () => {
    const restore = stubMatchMedia(true);
    expect(resolveTheme('system')).toBe('light');
    restore();
  });

  it('resolves "system" to dark when the OS prefers dark', () => {
    const restore = stubMatchMedia(false);
    expect(resolveTheme('system')).toBe('dark');
    restore();
  });

  it('an explicit mode is returned as-is, regardless of system preference', () => {
    const restore = stubMatchMedia(true);
    expect(resolveTheme('dark')).toBe('dark');
    expect(resolveTheme('light')).toBe('light');
    restore();
  });

  it('reads null when nothing was ever stored', () => {
    expect(readStoredThemeMode()).toBeNull();
  });

  it('round-trips a stored preference', () => {
    storeThemeMode('light');
    expect(readStoredThemeMode()).toBe('light');
    expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBe('light');
  });

  it('ignores an invalid/corrupted stored value', () => {
    window.localStorage.setItem(THEME_STORAGE_KEY, 'sepia');
    expect(readStoredThemeMode()).toBeNull();
  });

  it('applies the resolved theme to the document root via data-theme', () => {
    applyResolvedTheme('light');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');

    applyResolvedTheme('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });
});

describe('backend theme-mode mapping (FOR-120)', () => {
  it('maps a frontend mode to the backend uppercase vocabulary', () => {
    expect(toApiThemeMode('light')).toBe('LIGHT');
    expect(toApiThemeMode('dark')).toBe('DARK');
    expect(toApiThemeMode('system')).toBe('SYSTEM');
  });

  it('maps a backend value back to the matching frontend mode', () => {
    expect(fromApiThemeMode('LIGHT')).toBe('light');
    expect(fromApiThemeMode('DARK')).toBe('dark');
    expect(fromApiThemeMode('SYSTEM')).toBe('system');
  });

  it('returns null for an unrecognized backend value instead of throwing', () => {
    expect(fromApiThemeMode('SEPIA')).toBeNull();
  });

  it('returns null when the backend value is missing', () => {
    expect(fromApiThemeMode(null)).toBeNull();
    expect(fromApiThemeMode(undefined)).toBeNull();
  });
});
