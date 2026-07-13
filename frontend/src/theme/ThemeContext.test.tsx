import { afterEach, describe, expect, it } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, useTheme } from './ThemeContext';
import { THEME_STORAGE_KEY } from './theme';

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
  afterEach(() => {
    window.localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
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
