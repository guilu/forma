import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, useTheme } from './ThemeContext';
import { THEME_STORAGE_KEY } from './theme';
import { getProfile, updateThemeMode, type UserProfile } from '../api/profile';

// FOR-120: ThemeProvider now reads/persists the theme preference through the
// FOR-107 profile backend. Mocked here so FOR-62's existing regression tests
// stay deterministic and network-free; the backend-specific behavior gets its
// own describe block below.
vi.mock('../api/profile', () => ({
  getProfile: vi.fn(),
  updateThemeMode: vi.fn(),
}));

const getProfileMock = vi.mocked(getProfile);
const updateThemeModeMock = vi.mocked(updateThemeMode);

const BASE_PROFILE: UserProfile = {
  unitPreferences: { weightUnit: 'KG', heightUnit: 'CM', distanceUnit: 'KM', energyUnit: 'KCAL' },
  themeMode: 'SYSTEM',
  // FOR-121 fields: irrelevant to this file's theme tests.
  onboardingAnswers: {
    profile: { name: '', birthDate: '', sex: '', heightCm: '' },
    metrics: { measurementSaved: false },
    goal: {},
    training: { days: [] },
    equipment: { items: [] },
    nutrition: { preference: '', restrictions: '' },
  },
  firstRunCompleted: false,
};

/**
 * A controllable `matchMedia('(prefers-color-scheme: light)')` fake: lets a
 * test set the initial system preference and later fire a `change` event to
 * simulate the OS switching live (FOR-62 edge case: "system preference
 * changes at runtime → follow it only in 'system' mode").
 */
function stubControllableMatchMedia(initialMatches: boolean) {
  let matches = initialMatches;
  let listener: (() => void) | null = null;
  const original = window.matchMedia;

  window.matchMedia = (query: string) =>
    ({
      get matches() {
        return matches;
      },
      media: query,
      onchange: null,
      addEventListener: (_event: string, cb: () => void) => {
        listener = cb;
      },
      removeEventListener: () => {
        listener = null;
      },
      addListener: () => {},
      removeListener: () => {},
      dispatchEvent: () => false,
    }) as unknown as MediaQueryList;

  return {
    triggerChange(newMatches: boolean) {
      matches = newMatches;
      listener?.();
    },
    restore() {
      window.matchMedia = original;
    },
  };
}

function ThemeConsumer() {
  const { mode, resolvedTheme, setMode } = useTheme();
  return (
    <div>
      <span data-testid="mode">{mode}</span>
      <span data-testid="resolved">{resolvedTheme}</span>
      <button type="button" onClick={() => setMode('light')}>
        light
      </button>
      <button type="button" onClick={() => setMode('dark')}>
        dark
      </button>
      <button type="button" onClick={() => setMode('system')}>
        system
      </button>
    </div>
  );
}

describe('ThemeProvider / useTheme (FOR-62)', () => {
  beforeEach(() => {
    // Default: backend unreachable, so these FOR-62 regression tests exercise
    // the pure local-fallback path exactly as before FOR-120 existed.
    getProfileMock.mockRejectedValue(new Error('network unavailable'));
    updateThemeModeMock.mockResolvedValue(BASE_PROFILE);
  });

  afterEach(() => {
    window.localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    vi.clearAllMocks();
  });

  it('resolves to dark by default with no stored preference and no system signal', () => {
    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    expect(screen.getByTestId('mode')).toHaveTextContent('system');
    expect(screen.getByTestId('resolved')).toHaveTextContent('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  it('the toggle switches data-theme between light and dark', async () => {
    const user = userEvent.setup();
    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    await user.click(screen.getByRole('button', { name: 'light' }));
    expect(screen.getByTestId('resolved')).toHaveTextContent('light');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');

    await user.click(screen.getByRole('button', { name: 'dark' }));
    expect(screen.getByTestId('resolved')).toHaveTextContent('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  it('persists the preference and restores it on the next mount ("reload")', async () => {
    const user = userEvent.setup();
    const { unmount } = render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    await user.click(screen.getByRole('button', { name: 'light' }));
    expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBe('light');
    unmount();

    // Simulates a reload: a brand-new provider reading the same storage.
    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    expect(screen.getByTestId('mode')).toHaveTextContent('light');
    expect(screen.getByTestId('resolved')).toHaveTextContent('light');
  });

  it('follows the system preference when there is no explicit choice', () => {
    const media = stubControllableMatchMedia(true);

    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    expect(screen.getByTestId('mode')).toHaveTextContent('system');
    expect(screen.getByTestId('resolved')).toHaveTextContent('light');

    media.restore();
  });

  it('follows a system change at runtime only while in "system" mode', async () => {
    const media = stubControllableMatchMedia(false);

    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );
    expect(screen.getByTestId('resolved')).toHaveTextContent('dark');

    // The listener fires outside of any React event, so the resulting state
    // update must be flushed explicitly (matches how a real `MediaQueryList`
    // "change" event would need `act` in a test).
    act(() => {
      media.triggerChange(true);
    });
    expect(screen.getByTestId('resolved')).toHaveTextContent('light');

    media.restore();
  });

  it('an explicit choice is not overridden by a later system change', async () => {
    const user = userEvent.setup();
    const media = stubControllableMatchMedia(false);

    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    await user.click(screen.getByRole('button', { name: 'dark' }));
    expect(screen.getByTestId('mode')).toHaveTextContent('dark');

    // The OS now says "light", but the user picked dark explicitly.
    act(() => {
      media.triggerChange(true);
    });
    expect(screen.getByTestId('resolved')).toHaveTextContent('dark');

    media.restore();
  });

  it('useTheme throws when used outside a ThemeProvider', () => {
    function BareConsumer() {
      useTheme();
      return null;
    }

    expect(() => render(<BareConsumer />)).toThrow('useTheme must be used within a ThemeProvider');
  });
});

describe('ThemeProvider backend persistence (FOR-120)', () => {
  beforeEach(() => {
    updateThemeModeMock.mockResolvedValue(BASE_PROFILE);
  });

  afterEach(() => {
    window.localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    vi.clearAllMocks();
  });

  it('reconciles data-theme and mode once the backend preference loads and differs from the pre-paint guess', async () => {
    getProfileMock.mockResolvedValue({ ...BASE_PROFILE, themeMode: 'LIGHT' });

    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    // Pre-mount/pre-paint guess (no stored preference, no system signal): dark.
    expect(screen.getByTestId('resolved')).toHaveTextContent('dark');

    await waitFor(() => {
      expect(screen.getByTestId('mode')).toHaveTextContent('light');
      expect(screen.getByTestId('resolved')).toHaveTextContent('light');
    });
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    // Backend wins, and localStorage is updated to match (spec edge case).
    expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBe('light');
  });

  it('keeps the local/pre-paint theme when the backend fetch fails (degraded mode, no crash)', async () => {
    getProfileMock.mockRejectedValue(new Error('backend unavailable'));

    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    await waitFor(() => expect(getProfileMock).toHaveBeenCalled());
    // Still resolves via the local fallback chain -- no crash, no blocked render.
    expect(screen.getByTestId('mode')).toHaveTextContent('system');
    expect(screen.getByTestId('resolved')).toHaveTextContent('dark');
  });

  it('applies the backend first-run default (dark) with no error surfaced', async () => {
    getProfileMock.mockResolvedValue({ ...BASE_PROFILE, themeMode: 'DARK' });

    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('mode')).toHaveTextContent('dark');
    });
    expect(screen.getByTestId('resolved')).toHaveTextContent('dark');
  });

  it('a "system" preference loaded from the backend still follows live OS preference changes (FOR-62 regression guard)', async () => {
    const media = stubControllableMatchMedia(false);
    getProfileMock.mockResolvedValue({ ...BASE_PROFILE, themeMode: 'SYSTEM' });

    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId('mode')).toHaveTextContent('system');
    });

    act(() => {
      media.triggerChange(true);
    });
    expect(screen.getByTestId('resolved')).toHaveTextContent('light');

    media.restore();
  });

  it('persists a toggled mode to the backend via PATCH /api/v1/profile/theme', async () => {
    getProfileMock.mockRejectedValue(new Error('backend unavailable'));
    const user = userEvent.setup();

    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    await user.click(screen.getByRole('button', { name: 'light' }));

    await waitFor(() => {
      expect(updateThemeModeMock).toHaveBeenCalledWith({ themeMode: 'LIGHT' });
    });
    // The localStorage fallback write is preserved alongside the backend write.
    expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBe('light');
  });

  it('does not revert the local toggle when backend persistence fails', async () => {
    getProfileMock.mockRejectedValue(new Error('backend unavailable'));
    updateThemeModeMock.mockRejectedValue(new Error('persist failed'));
    const user = userEvent.setup();

    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    await user.click(screen.getByRole('button', { name: 'light' }));

    expect(screen.getByTestId('resolved')).toHaveTextContent('light');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    await waitFor(() => expect(updateThemeModeMock).toHaveBeenCalled());
    // Still light after the persistence rejection settles -- no revert.
    expect(screen.getByTestId('resolved')).toHaveTextContent('light');
  });

  it('a user toggle during the in-flight mount fetch is not clobbered by the (now stale) backend value', async () => {
    // A controllable GET: it stays pending until we resolve it, so we can
    // simulate the user toggling *before* the backend reply lands.
    let resolveProfile!: (profile: UserProfile) => void;
    getProfileMock.mockReturnValue(
      new Promise<UserProfile>((resolve) => {
        resolveProfile = resolve;
      }),
    );
    const user = userEvent.setup();

    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>,
    );

    // User explicitly picks light while the mount fetch is still pending.
    await user.click(screen.getByRole('button', { name: 'light' }));
    expect(screen.getByTestId('mode')).toHaveTextContent('light');

    // The backend reply (a now-stale 'DARK') finally arrives.
    await act(async () => {
      resolveProfile({ ...BASE_PROFILE, themeMode: 'DARK' });
    });

    // The user's fresh choice wins -- the stale server value must not revert it.
    expect(screen.getByTestId('mode')).toHaveTextContent('light');
    expect(screen.getByTestId('resolved')).toHaveTextContent('light');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });
});
