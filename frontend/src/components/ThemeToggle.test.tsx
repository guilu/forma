import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider } from '../theme/ThemeContext';
import { ThemeToggle } from './ThemeToggle';

// FOR-120: ThemeProvider reads/persists the theme preference through this
// module on mount and on toggle. Mocked so this FOR-62 test stays network-free
// and deterministic; 'SYSTEM' matches the default local mode so the mount-time
// reconciliation is a no-op here.
vi.mock('../api/profile', () => ({
  getProfile: vi.fn().mockResolvedValue({
    unitPreferences: { weightUnit: 'KG', heightUnit: 'CM', distanceUnit: 'KM', energyUnit: 'KCAL' },
    themeMode: 'SYSTEM',
  }),
  updateThemeMode: vi.fn().mockResolvedValue(undefined),
}));

function renderToggle() {
  return render(
    <ThemeProvider>
      <ThemeToggle />
    </ThemeProvider>,
  );
}

describe('ThemeToggle (FOR-62)', () => {
  afterEach(() => {
    window.localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
  });

  it('renders a labelled group with light/dark/system options', () => {
    renderToggle();

    // Radios, not toggle buttons (FOR-189): three mutually exclusive choices of
    // one setting is exactly what a radio group is, and it gets arrow-key
    // navigation from the platform instead of three separate tab stops.
    expect(screen.getByRole('radiogroup', { name: 'Tema' })).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: 'Claro' })).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: 'Oscuro' })).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: 'Sistema' })).toBeInTheDocument();
  });

  it('marks the active option as checked, and clicking switches data-theme', async () => {
    const user = userEvent.setup();
    renderToggle();

    // Default mode ("system", no stored preference) starts on "Sistema".
    expect(screen.getByRole('radio', { name: 'Sistema' })).toBeChecked();
    expect(screen.getByRole('radio', { name: 'Claro' })).not.toBeChecked();

    await user.click(screen.getByRole('radio', { name: 'Oscuro' }));
    expect(screen.getByRole('radio', { name: 'Oscuro' })).toBeChecked();
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');

    await user.click(screen.getByRole('radio', { name: 'Claro' }));
    expect(screen.getByRole('radio', { name: 'Claro' })).toBeChecked();
    expect(screen.getByRole('radio', { name: 'Oscuro' })).not.toBeChecked();
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });
});
