import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { SettingsPage } from './SettingsPage';
import { listIntegrations } from '../api/integrations';
import { ThemeProvider } from '../theme/ThemeContext';
import { axe } from '../test/axe';

// The "Tema" row (FOR-62) reads `useTheme()`, so every render needs a
// ThemeProvider ancestor — App.tsx provides one at the route-tree level, but
// this file mounts SettingsPage standalone.
function renderSettingsPage() {
  return render(
    <ThemeProvider>
      <SettingsPage />
    </ThemeProvider>,
  );
}

// `vi.mock` factories are hoisted above top-level const declarations, so the
// fixture is defined inline here rather than referenced from an outer const.
vi.mock('../api/integrations', () => ({
  listIntegrations: vi.fn().mockResolvedValue([
    {
      providerId: 'WITHINGS',
      providerName: 'Withings',
      description: 'Sincroniza automáticamente tus datos de salud y composición corporal.',
      status: 'CONNECTED',
      lastSyncAt: '2026-07-10T08:15:00Z',
    },
    {
      providerId: 'GOOGLE_FIT',
      providerName: 'Google Fit',
      description: 'Sincroniza tu actividad y entrenamientos.',
      status: 'NOT_CONNECTED',
    },
  ]),
}));

/**
 * FOR-58: the Ajustes screen composes every grouped section and honestly
 * distinguishes editable (real, working) content from read-only/inert
 * content. Per-section behavior detail lives in each section's own test file;
 * this is the composition-level smoke test.
 */
describe('SettingsPage', () => {
  it('renders every grouped section from the spec', () => {
    renderSettingsPage();

    expect(screen.getByRole('heading', { name: 'Configuración', level: 1 })).toBeInTheDocument();
    // Every section card is a direct sibling of the page <h1> (no intervening
    // <h2>), so per FOR-112 each section title must render as <h2>.
    expect(
      screen.getByRole('heading', { name: 'Perfil y preferencias', level: 2 }),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Unidades', level: 2 })).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Conexiones e integraciones', level: 2 }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Objetivos por defecto', level: 2 }),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Notificaciones', level: 2 })).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Seguridad y datos', level: 2 }),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Acerca de FORMA', level: 2 })).toBeInTheDocument();
  });

  it('shows the profile summary with name and email', () => {
    renderSettingsPage();

    expect(screen.getByText('Usuario FORMA')).toBeInTheDocument();
    expect(screen.getByText('usuario@forma.app')).toBeInTheDocument();
  });

  it('mounts the reused FOR-57 integrations section', () => {
    renderSettingsPage();

    expect(vi.mocked(listIntegrations)).toHaveBeenCalled();
  });

  it('distinguishes editable content (Conexiones, real buttons) from read-only/inert content', async () => {
    renderSettingsPage();

    // Editable: Conexiones renders real, enabled action buttons (FOR-57, already working).
    const withingsButton = await screen.findByRole('button', { name: 'Sincronizar ahora' });
    expect(withingsButton).toBeEnabled();

    // Inert: "Editar perfil" is a real button but disabled — visible entry point, not active.
    expect(screen.getByRole('button', { name: 'Editar perfil' })).toBeDisabled();

    // Read-only: profile fields are plain text, not any kind of control.
    expect(screen.queryByRole('button', { name: /Sexo/ })).not.toBeInTheDocument();
    expect(screen.getByText('No especificado')).toBeInTheDocument();
  });

  it('marks unsupported security/data options as inert, not active', () => {
    renderSettingsPage();

    expect(screen.getByText('Autenticación en dos pasos')).toBeInTheDocument();
    expect(screen.getByText('Exportar mis datos')).toBeInTheDocument();
    // Every inert row carries a visible "Próximamente" marker.
    expect(screen.getAllByText('Próximamente').length).toBeGreaterThan(0);
    // No button/link renders for these unsupported flows.
    expect(
      screen.queryByRole('button', { name: 'Autenticación en dos pasos' }),
    ).not.toBeInTheDocument();
  });

  it('has no accessibility violations across all grouped sections (FOR-114)', async () => {
    const { container } = renderSettingsPage();
    // Includes SecuritySection's inert "Próximamente" rows -- confirms the
    // visible-but-non-interactive pattern doesn't trip an axe violation
    // (FOR-114 edge case).
    await screen.findByRole('button', { name: 'Sincronizar ahora' });

    expect(await axe(container)).toHaveNoViolations();
  });
});
