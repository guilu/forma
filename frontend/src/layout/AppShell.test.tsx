import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Link, MemoryRouter, Route, Routes } from 'react-router-dom';
import { AppShell } from './AppShell';
import { ThemeProvider } from '../theme/ThemeContext';

// FOR-120: ThemeProvider reads/persists the theme preference through this
// module on mount. Mocked so these shell tests stay network-free; 'SYSTEM'
// matches the default local mode so the mount-time reconciliation is a no-op.
vi.mock('../api/profile', () => ({
  getProfile: vi.fn().mockResolvedValue({
    unitPreferences: { weightUnit: 'KG', heightUnit: 'CM', distanceUnit: 'KM', energyUnit: 'KCAL' },
    themeMode: 'SYSTEM',
  }),
  updateThemeMode: vi.fn().mockResolvedValue(undefined),
}));

/**
 * Application shell accessibility tests (FOR-61): the skip link, the
 * header/nav/main landmarks every page relies on, and focus management on
 * client-side route changes (React Router does not move focus itself, so the
 * shell has to).
 */
function RoutedPage({ label, to }: { readonly label: string; readonly to?: string }) {
  return (
    <>
      <h1>{label}</h1>
      {to && <Link to={to}>Ir a la siguiente página</Link>}
    </>
  );
}

function renderShell(initialEntry: string) {
  return render(
    <ThemeProvider>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/" element={<AppShell />}>
            <Route path="a" element={<RoutedPage label="Página A" to="/b" />} />
            <Route path="b" element={<RoutedPage label="Página B" />} />
          </Route>
        </Routes>
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe('AppShell accessibility', () => {
  it('exposes header, navigation and main landmarks', () => {
    renderShell('/a');

    expect(screen.getByRole('banner')).toBeInTheDocument();
    expect(screen.getByRole('navigation')).toBeInTheDocument();
    expect(screen.getByRole('main')).toBeInTheDocument();
  });

  it('renders a skip link targeting the main landmark', () => {
    renderShell('/a');

    const skipLink = screen.getByRole('link', { name: 'Saltar al contenido principal' });
    expect(skipLink).toHaveAttribute('href', '#main-content');
    expect(screen.getByRole('main')).toHaveAttribute('id', 'main-content');
  });

  it('does not steal focus into main on first mount', () => {
    renderShell('/a');

    expect(screen.getByRole('main')).not.toHaveFocus();
  });

  it('moves focus to the main landmark after a client-side route change', async () => {
    const user = userEvent.setup();
    renderShell('/a');

    await user.click(screen.getByRole('link', { name: 'Ir a la siguiente página' }));

    expect(screen.getByRole('heading', { name: 'Página B' })).toBeInTheDocument();
    expect(screen.getByRole('main')).toHaveFocus();
  });
});
