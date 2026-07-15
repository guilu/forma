import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import type { ReactNode } from 'react';
import {
  DEFAULT_THEME_MODE,
  applyResolvedTheme,
  fromApiThemeMode,
  readStoredThemeMode,
  resolveTheme,
  storeThemeMode,
  toApiThemeMode,
  type ResolvedTheme,
  type ThemeMode,
} from './theme';
import { getProfile, updateThemeMode } from '../api/profile';

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
 * Theme provider (FOR-62, backend-persisted since FOR-120). Resolves the
 * active theme on mount — explicit stored preference > system
 * (`prefers-color-scheme`) > dark default — and keeps `data-theme` on
 * `<html>` (styles/theme.css) in sync as the user toggles or the OS
 * preference changes while in `system` mode.
 *
 * <p>Flash avoidance: `index.html` carries a small inline script that applies
 * the same precedence synchronously before this component (and React) ever
 * runs, so the very first paint already shows the resolved theme. This
 * provider re-applies the value on mount (redundant but harmless when it
 * already matches) so it is also correct in every test/SSR-less context that
 * never loads `index.html`, and takes over live updates afterwards.
 *
 * <p><b>Backend persistence (FOR-120):</b> once mounted, this provider fetches
 * the FOR-107-persisted preference ({@code GET /api/v1/profile}) and, once it
 * loads, treats it as the source of truth -- reconciling `mode`/`data-theme`
 * if it differs from the pre-paint guess, and updating `localStorage` to
 * match. The fetch is fire-and-forget (never awaited before the first
 * render), so first paint is never delayed by a network round-trip; a failed
 * fetch is swallowed and the provider simply keeps the local/pre-paint
 * resolution (degraded mode, spec FOR-120: theme is a cosmetic preference,
 * not worth surfacing an error for). `setMode` mirrors this: it still writes
 * to `localStorage` first (fast, always-on fallback) and additionally
 * persists to the backend; a failed persist never reverts the already-applied
 * local change.
 */
export function ThemeProvider({ children }: { readonly children: ReactNode }) {
  const [mode, setModeState] = useState<ThemeMode>(
    () => readStoredThemeMode() ?? DEFAULT_THEME_MODE,
  );
  const [resolvedTheme, setResolvedTheme] = useState<ResolvedTheme>(() => resolveTheme(mode));
  // Tracks whether the user has made an explicit choice since mount. Guards the
  // one-shot backend reconciliation below from clobbering a toggle the user
  // made *while* the initial GET was still in flight (a slow-network race: the
  // stale server value must not silently revert a fresh, deliberate action).
  const userToggledRef = useRef(false);

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

  // FOR-120: reconcile with the backend-persisted preference once it loads.
  // Deliberately not awaited/blocking (spec NFR: "the backend call is
  // non-blocking for first paint") -- runs after the pre-paint/local value is
  // already applied above. A `cancelled` guard avoids a state update after
  // unmount if the fetch is still in flight (e.g. a fast test teardown).
  useEffect(() => {
    let cancelled = false;
    getProfile()
      .then((profile) => {
        // Skip if unmounted, or if the user already made an explicit choice
        // while this fetch was in flight -- their fresh action wins over a
        // now-stale server value (see `userToggledRef`).
        if (cancelled || userToggledRef.current) {
          return;
        }
        const backendMode = fromApiThemeMode(profile.themeMode);
        if (backendMode) {
          // Backend wins once loaded; keep the local fallback in sync too
          // (spec edge case: "backend and localStorage disagree").
          storeThemeMode(backendMode);
          setModeState(backendMode);
        }
      })
      .catch(() => {
        // Backend unavailable / preference not yet created -- keep the
        // pre-paint/local fallback chain (spec edge case, degraded mode).
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const setMode = useCallback((next: ThemeMode) => {
    userToggledRef.current = true;
    storeThemeMode(next);
    setModeState(next);
    // Fire-and-forget: a failed persist must not revert the local change
    // that already applied above (spec: cosmetic-preference tolerance).
    updateThemeMode({ themeMode: toApiThemeMode(next) }).catch(() => {
      // Preference simply won't survive a reload from another device --
      // not worth surfacing an error for a cosmetic preference.
    });
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
