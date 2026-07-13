/**
 * Theme resolution + persistence logic (FOR-62). Pure functions only — no
 * React, no JSX — so they can be unit tested directly and reused by both the
 * runtime (`ThemeProvider`) and the pre-paint inline script in `index.html`
 * (which is plain JS and cannot import this module, but mirrors the same
 * precedence rules; keep the two in sync if this logic changes).
 *
 * `styles/theme.css` (FOR-81) already defines the dark (default) and
 * `[data-theme='light']` token overrides. This module only decides *which*
 * value to put in `data-theme` and persists the user's choice — it never
 * introduces a new palette.
 */

/** The user's stored preference. `system` means "follow the OS setting". */
export type ThemeMode = 'light' | 'dark' | 'system';

/** The theme actually applied to the document — always concrete. */
export type ResolvedTheme = 'light' | 'dark';

export const THEME_STORAGE_KEY = 'forma.theme';

const VALID_MODES: readonly ThemeMode[] = ['light', 'dark', 'system'];

function isThemeMode(value: string | null): value is ThemeMode {
  return value !== null && (VALID_MODES as readonly string[]).includes(value);
}

/**
 * Reads the explicit stored preference. Returns `null` when nothing was
 * stored yet, or when storage is unavailable (private browsing, quota) — the
 * caller then falls through to system/dark resolution (spec FOR-62: "explicit
 * stored preference > system > dark default").
 */
export function readStoredThemeMode(): ThemeMode | null {
  try {
    const stored = window.localStorage.getItem(THEME_STORAGE_KEY);
    return isThemeMode(stored) ? stored : null;
  } catch {
    return null;
  }
}

/** Persists the user's explicit choice. Silently no-ops if storage fails. */
export function storeThemeMode(mode: ThemeMode): void {
  try {
    window.localStorage.setItem(THEME_STORAGE_KEY, mode);
  } catch {
    // Preference simply won't survive a reload — not worth surfacing an error
    // for a cosmetic preference.
  }
}

/**
 * Whether the OS currently signals a light-mode preference. `false` (i.e.
 * "resolve to dark") when `matchMedia` is unavailable, matching the edge case
 * "no stored preference + no system signal → dark default".
 */
export function systemPrefersLight(): boolean {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return false;
  }
  return window.matchMedia('(prefers-color-scheme: light)').matches;
}

/** Resolves a mode to the concrete theme that should be applied. */
export function resolveTheme(mode: ThemeMode): ResolvedTheme {
  if (mode === 'system') {
    return systemPrefersLight() ? 'light' : 'dark';
  }
  return mode;
}

/** Sets the `data-theme` switch (styles/theme.css) on the document root. */
export function applyResolvedTheme(theme: ResolvedTheme): void {
  document.documentElement.setAttribute('data-theme', theme);
}

/** The mode to start from when nothing has been explicitly chosen yet. */
export const DEFAULT_THEME_MODE: ThemeMode = 'system';
