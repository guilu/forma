import { beforeEach, describe, expect, it, vi } from 'vitest';
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
const logoutMock = vi.fn();
let authStatus: 'authenticated' | 'anonymous' = 'authenticated';
vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({
    status: authStatus,
    user: authStatus === 'authenticated' ? { id: 'user-1', email: 'persona@example.com' } : null,
    logout: logoutMock,
  }),
}));

/**
 * Shell hardening tests (FOR-49): the sidebar integration status, the topbar
 * account area, and the mobile "Más" overflow that makes every section reachable
 * from navigation on small screens.
 */
// FOR-185: the topbar is now the global navigation bar and links to the app /
// landing root, so it needs a router context of its own.
function renderTopbar() {
  return render(
    <ThemeProvider>
      <MemoryRouter>
        <Topbar />
      </MemoryRouter>
    </ThemeProvider>,
  );
}

describe('application shell', () => {
  beforeEach(() => {
    logoutMock.mockReset();
    authStatus = 'authenticated';
  });

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
      <MemoryRouter initialEntries={['/app']}>
        <Sidebar />
      </MemoryRouter>,
    );

    const activeLink = screen.getByRole('link', { name: 'Dashboard' });
    const inactiveLink = screen.getByRole('link', { name: 'Mediciones' });

    expect(activeLink.className.split(' ')).toContain(styles.active);
    expect(inactiveLink.className.split(' ')).not.toContain(styles.active);
  });

  // FOR-185: the brand is the route back out to the public site, so it points
  // at the landing in both states — including while signed in, where it is the
  // only way to reach `/` at all.
  it.each(['authenticated', 'anonymous'] as const)(
    'links the brand to the landing for a %s visitor',
    (status) => {
      authStatus = status;
      renderTopbar();

      expect(screen.getByRole('link', { name: 'FORMA, inicio' })).toHaveAttribute('href', '/');
    },
  );

  // FOR-185: the account collapsed into an avatar trigger plus a menu, so the
  // profile and logout actions live one interaction deeper than they used to.
  it('exposes profile and logout behind the account menu', async () => {
    const user = userEvent.setup();
    renderTopbar();

    expect(screen.getByRole('button', { name: 'Notificaciones' })).toBeInTheDocument();
    const trigger = screen.getByRole('button', { name: 'Cuenta: persona@example.com' });
    expect(trigger).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByRole('menu', { name: 'Cuenta' })).not.toBeInTheDocument();

    await user.click(trigger);

    expect(trigger).toHaveAttribute('aria-expanded', 'true');
    const menu = screen.getByRole('menu', { name: 'Cuenta' });
    expect(within(menu).getByRole('menuitem', { name: 'Perfil' })).toHaveAttribute(
      'href',
      '/app/ajustes',
    );
    await user.click(within(menu).getByRole('menuitem', { name: 'Cerrar sesión' }));
    expect(logoutMock).toHaveBeenCalled();
  });

  it('closes the account menu when focus leaves it', async () => {
    const user = userEvent.setup();
    renderTopbar();

    await user.click(screen.getByRole('button', { name: 'Cuenta: persona@example.com' }));
    expect(screen.getByRole('menu', { name: 'Cuenta' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Notificaciones' }));

    expect(screen.queryByRole('menu', { name: 'Cuenta' })).not.toBeInTheDocument();
  });

  it('keeps logout failure handled and offers retry', async () => {
    logoutMock.mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce(undefined);
    const user = userEvent.setup();
    renderTopbar();
    await user.click(screen.getByRole('button', { name: 'Cuenta: persona@example.com' }));
    await user.click(screen.getByRole('menuitem', { name: 'Cerrar sesión' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cerrar la sesión');
    // The menu stays open through the failure, so the retry is right there.
    await user.click(screen.getByRole('menuitem', { name: 'Reintentar cierre de sesión' }));
    expect(logoutMock).toHaveBeenCalledTimes(2);
  });

  it('toggles the theme from the topbar next to the notifications bell', async () => {
    const user = userEvent.setup();
    document.documentElement.removeAttribute('data-theme');
    renderTopbar();

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

    // FOR-164: the primary mobile bar is limited to Dashboard, Mediciones and
    // Entrenamiento. Progreso moved behind "Más", so it is not a primary bar
    // link — and FOR-185 moved Nutrición there too.
    expect(screen.queryByRole('link', { name: 'Progreso', hidden: true })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Nutrición', hidden: true })).not.toBeInTheDocument();

    const more = screen.getByRole('button', { name: 'Más', hidden: true });
    expect(more).toHaveAttribute('aria-expanded', 'false');
    await user.click(more);

    expect(more).toHaveAttribute('aria-expanded', 'true');
    const menu = screen.getByRole('menu', { name: 'Más secciones', hidden: true });
    // FOR-185: Nutrición leads the overflow, directly above Lista de compra —
    // the order comes from NAV_ITEMS, so this pins it there.
    const items = within(menu).getAllByRole('menuitem', { hidden: true });
    expect(items.map((item) => item.textContent)).toEqual([
      'Nutrición',
      'Lista de compra',
      'Progreso',
      'Objetivos',
      'Ajustes',
    ]);
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

  // FOR-185: the same bar now renders above the public pages too, so an
  // anonymous visitor gets the landing's section anchors and a login action
  // instead of the account controls.
  it('shows the public navigation and a login action to anonymous visitors', () => {
    authStatus = 'anonymous';
    renderTopbar();

    const nav = screen.getByRole('navigation', { name: 'Navegación pública' });
    expect(within(nav).getByRole('link', { name: 'Entrenamiento' })).toHaveAttribute(
      'href',
      '/#entrenamiento',
    );
    expect(within(nav).getByRole('link', { name: 'Nutrición' })).toHaveAttribute(
      'href',
      '/#nutricion',
    );
    expect(within(nav).getByRole('link', { name: 'Planes' })).toHaveAttribute('href', '/#planes');
    // The login action sits outside the <nav>: it is an account action, not a
    // section link, and staying out of the collapsing group keeps it in the
    // bar on small screens instead of behind the disclosure.
    expect(within(nav).queryByRole('link', { name: 'Iniciar Sesión' })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Iniciar Sesión' })).toHaveAttribute('href', '/login');
    expect(screen.queryByRole('button', { name: 'Cerrar sesión' })).not.toBeInTheDocument();
  });

  // FOR-185: the theme toggle is ordered before the login action in the bar.
  it('places the theme toggle before the login action', () => {
    authStatus = 'anonymous';
    renderTopbar();

    const toggle = screen.getByRole('button', { name: /Cambiar a tema/ });
    const login = screen.getByRole('link', { name: 'Iniciar Sesión' });

    expect(toggle.compareDocumentPosition(login) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('keeps the theme toggle available to anonymous visitors', async () => {
    authStatus = 'anonymous';
    document.documentElement.removeAttribute('data-theme');
    const user = userEvent.setup();
    renderTopbar();

    await user.click(screen.getByRole('button', { name: 'Cambiar a tema claro' }));

    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });

  // The disclosure only matters below `md`, where the anchors are CSS-hidden;
  // at the jsdom desktop viewport the button itself is `display: none`, which
  // makes its accessible name compute to "" — hence `getByLabelText`, which
  // matches the attribute rather than the computed name. This exercises the
  // component logic only; the breakpoint behaviour is CSS.
  it('toggles the public navigation from the mobile menu button', async () => {
    authStatus = 'anonymous';
    const user = userEvent.setup();
    renderTopbar();

    const menu = screen.getByLabelText('Abrir menú');
    expect(menu).toHaveAttribute('aria-expanded', 'false');

    await user.click(menu);

    expect(menu).toHaveAttribute('aria-expanded', 'true');
    // The hamburger morphs into an X purely in CSS, so the accessible name is
    // the only signal of that state change a screen reader gets — it has to
    // travel with it. Asserted as the attribute rather than via
    // `toHaveAccessibleName`, for the same reason this test uses
    // `getByLabelText`: the button is `display: none` at the jsdom desktop
    // viewport, and a hidden element's computed accessible name is "".
    expect(menu).toHaveAttribute('aria-label', 'Cerrar menú');
  });
});
