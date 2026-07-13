import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import {
  DEFAULT_THEME_MODE,
  applyResolvedTheme,
  readStoredThemeMode,
  resolveTheme,
  storeThemeMode,
  type ResolvedTheme,
  type ThemeMode,
} from './theme';

interface ThemeContextValue {
  /** The user's preference: an explicit theme, or `system`. */
  readonly mode: ThemeMode;
  /** The concrete theme currently applied to `data-theme`. */
  readonly resolvedTheme: ResolvedTheme;
  /** Sets an explicit preference (or switches back to following the OS). */
  readonly setMode: (mode: ThemeMode) => void;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);

/**
 * Theme provider (FOR-62). Resolves the active theme on mount — explicit
 * stored preference > system (`prefers-color-scheme`) > dark default — and
 * keeps `data-theme` on `<html>` (styles/theme.css) in sync as the user
 * toggles or the OS preference changes while in `system` mode.
 *
 * <p>Flash avoidance: `index.html` carries a small inline script that applies
 * the same precedence synchronously before this component (and React) ever
 * runs, so the very first paint already shows the resolved theme. This
 * provider re-applies the value on mount (redundant but harmless when it
 * already matches) so it is also correct in every test/SSR-less context that
 * never loads `index.html`, and takes over live updates afterwards.
 */
export function ThemeProvider({ children }: { readonly children: ReactNode }) {
  const [mode, setModeState] = useState<ThemeMode>(
    () => readStoredThemeMode() ?? DEFAULT_THEME_MODE,
  );
  const [resolvedTheme, setResolvedTheme] = useState<ResolvedTheme>(() => resolveTheme(mode));

  useEffect(() => {
    const resolved = resolveTheme(mode);
    setResolvedTheme(resolved);
    applyResolvedTheme(resolved);
  }, [mode]);

  // Follow the OS live, but only while the user hasn't picked an explicit
  // theme (spec FOR-62 edge case: "system preference changes at runtime →
  // follow it only when in 'system' mode").
  useEffect(() => {
    if (mode !== 'system' || typeof window.matchMedia !== 'function') {
      return undefined;
    }
    const media = window.matchMedia('(prefers-color-scheme: light)');
    const handleChange = () => {
      const resolved = resolveTheme('system');
      setResolvedTheme(resolved);
      applyResolvedTheme(resolved);
    };
    media.addEventListener('change', handleChange);
    return () => media.removeEventListener('change', handleChange);
  }, [mode]);

  const setMode = useCallback((next: ThemeMode) => {
    storeThemeMode(next);
    setModeState(next);
  }, []);

  const value = useMemo<ThemeContextValue>(
    () => ({ mode, resolvedTheme, setMode }),
    [mode, resolvedTheme, setMode],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

// Hook lives alongside its provider so consumers get a single import; fast
// refresh still works, it just also reloads this hook when the file changes.
// eslint-disable-next-line react-refresh/only-export-components
export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return ctx;
}
