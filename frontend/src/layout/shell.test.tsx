import { describe, expect, it } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Topbar } from './Topbar';
import { MobileNav } from './MobileNav';

/**
 * Shell hardening tests (FOR-49): the sidebar integration status, the topbar
 * account area, and the mobile "Más" overflow that makes every section reachable
 * from navigation on small screens.
 */
describe('application shell', () => {
  it('renders the Withings integration status in the sidebar footer', () => {
    render(
      <MemoryRouter>
        <Sidebar />
      </MemoryRouter>,
    );

    expect(screen.getByText('Withings')).toBeInTheDocument();
    expect(screen.getByText('Conectado')).toBeInTheDocument();
  });

  it('renders the account area and notifications in the topbar', () => {
    render(<Topbar />);

    expect(screen.getByText('Diego')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Notificaciones' })).toBeInTheDocument();
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

    const more = screen.getByRole('button', { name: 'Más', hidden: true });
    expect(more).toHaveAttribute('aria-expanded', 'false');
    await user.click(more);

    expect(more).toHaveAttribute('aria-expanded', 'true');
    const menu = screen.getByRole('menu', { name: 'Más secciones', hidden: true });
    expect(
      within(menu).getByRole('menuitem', { name: 'Lista de compra', hidden: true }),
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
