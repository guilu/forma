import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Topbar } from './Topbar';
import { MobileNav } from './MobileNav';
import { ThemeProvider } from '../theme/ThemeContext';
import styles from './Sidebar.module.css';

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
 * Shell hardening tests (FOR-49): the sidebar integration status, the topbar
 * account area, and the mobile "Más" overflow that makes every section reachable
 * from navigation on small screens.
 */
describe('application shell', () => {
  it('renders the Withings integration status as a card in the sidebar footer', () => {
    render(
      <MemoryRouter>
        <Sidebar />
      </MemoryRouter>,
    );

    // FOR-164: the footer moved from a plain icon+text row to a bordered card
    // with an uppercase "WITHINGS" label; the status copy stays honest
    // ("Conectado") since there is no real sync-timestamp backend yet.
    expect(screen.getByText('WITHINGS')).toBeInTheDocument();
    expect(screen.getByText('Conectado')).toBeInTheDocument();
  });

  // FOR-164: nav items move from a solid active fill to a subtle tint + right
  // border. The tint/border/radius are CSS-only and not meaningfully
  // assertable in jsdom, but the CSS Module class wiring that drives them is —
  // compare against the real compiled classnames instead of guessing hashes.
  it('applies the active CSS module class only to the link matching the current route', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <Sidebar />
      </MemoryRouter>,
    );

    const activeLink = screen.getByRole('link', { name: 'Dashboard' });
    const inactiveLink = screen.getByRole('link', { name: 'Mediciones' });

    expect(activeLink.className.split(' ')).toContain(styles.active);
    expect(inactiveLink.className.split(' ')).not.toContain(styles.active);
  });

  it('renders the account area and notifications in the topbar', () => {
    render(
      <ThemeProvider>
        <Topbar />
      </ThemeProvider>,
    );

    expect(screen.getByText('Diego')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Notificaciones' })).toBeInTheDocument();
  });

  it('toggles the theme from the topbar next to the notifications bell', async () => {
    const user = userEvent.setup();
    document.documentElement.removeAttribute('data-theme');
    render(
      <ThemeProvider>
        <Topbar />
      </ThemeProvider>,
    );

    // Default resolves to dark → the button offers switching to light (sun).
    const toggle = screen.getByRole('button', { name: 'Cambiar a tema claro' });
    await user.click(toggle);

    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    // Now it offers switching back to dark (moon).
    expect(screen.getByRole('button', { name: 'Cambiar a tema oscuro' })).toBeInTheDocument();
  });

  // FOR-164: "Ajustes" moves out of the primary nav flow and into its own
  // settings group, pinned to the bottom of the sidebar's flex column (just
  // above the Withings card) via `margin-top: auto` — matching the template's
  // gap between the primary section list and the lower "Ajustes" entry. The
  // pixel-level pinning itself is CSS-only and not assertable in jsdom, but
  // the DOM grouping that drives it is.
  it('separates "Ajustes" into its own settings group at the end of the nav', () => {
    render(
      <MemoryRouter>
        <Sidebar />
      </MemoryRouter>,
    );

    const nav = screen.getByRole('navigation', { name: 'Navegación principal' });
    const ajustesLink = screen.getByRole('link', { name: 'Ajustes' });
    const dashboardLink = screen.getByRole('link', { name: 'Dashboard' });

    // The primary group's links are direct children of <nav>...
    expect(dashboardLink.parentElement).toBe(nav);
    // ...but Ajustes lives inside a dedicated settings wrapper, not as a bare
    // sibling of the primary links.
    expect(ajustesLink.parentElement).not.toBe(nav);
    expect(ajustesLink.parentElement?.parentElement).toBe(nav);
    expect(ajustesLink.parentElement).toHaveClass(styles.settingsGroup);
    // That wrapper is the last element in the nav, so it anchors to the bottom.
    expect(nav.lastElementChild).toBe(ajustesLink.parentElement);
  });

  // The mobile bar is CSS-hidden at the jsdom desktop viewport (shown only
  // <=768px), so these query with `hidden: true` to exercise the component logic.
  it('exposes secondary sections behind the mobile "Más" overflow', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <MobileNav />
      </MemoryRouter>,
    );

    // Secondary sections are not rendered until "Más" is opened.
    expect(
      screen.queryByRole('menuitem', { name: 'Objetivos', hidden: true }),
    ).not.toBeInTheDocument();

    // FOR-164: the primary mobile bar is limited to Dashboard, Mediciones,
    // Entrenamiento and Nutrición. Progreso moved behind "Más", so it is not a
    // primary bar link.
    expect(screen.queryByRole('link', { name: 'Progreso', hidden: true })).not.toBeInTheDocument();

    const more = screen.getByRole('button', { name: 'Más', hidden: true });
    expect(more).toHaveAttribute('aria-expanded', 'false');
    await user.click(more);

    expect(more).toHaveAttribute('aria-expanded', 'true');
    const menu = screen.getByRole('menu', { name: 'Más secciones', hidden: true });
    expect(
      within(menu).getByRole('menuitem', { name: 'Lista de compra', hidden: true }),
    ).toBeInTheDocument();
    expect(
      within(menu).getByRole('menuitem', { name: 'Progreso', hidden: true }),
    ).toBeInTheDocument();
    expect(
      within(menu).getByRole('menuitem', { name: 'Objetivos', hidden: true }),
    ).toBeInTheDocument();
    expect(
      within(menu).getByRole('menuitem', { name: 'Ajustes', hidden: true }),
    ).toBeInTheDocument();
  });

  it('collapses the "Más" overflow after choosing a section', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <MobileNav />
      </MemoryRouter>,
    );

    await user.click(screen.getByRole('button', { name: 'Más', hidden: true }));
    await user.click(screen.getByRole('menuitem', { name: 'Ajustes', hidden: true }));

    expect(
      screen.queryByRole('menu', { name: 'Más secciones', hidden: true }),
    ).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Más', hidden: true })).toHaveAttribute(
      'aria-expanded',
      'false',
    );
  });
});
