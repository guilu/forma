import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { SettingsPage } from './SettingsPage';
import { listIntegrations } from '../api/integrations';
import { ThemeProvider } from '../theme/ThemeContext';

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

    expect(screen.getByRole('heading', { name: 'Configuración' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Perfil y preferencias' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Unidades' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Conexiones e integraciones' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Objetivos por defecto' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Notificaciones' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Seguridad y datos' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Acerca de FORMA' })).toBeInTheDocument();
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
});
