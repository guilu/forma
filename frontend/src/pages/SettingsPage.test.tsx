import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { SettingsPage } from './SettingsPage';
import { listIntegrations } from '../api/integrations';
import { ThemeProvider } from '../theme/ThemeContext';
import { NotificationProvider } from '../components/NotificationProvider';
import { axe } from '../test/axe';

// The "Tema" row (FOR-62) reads `useTheme()`, and `ProfileSection` (FOR-119)
// calls `useNotify()`, so every render needs both providers — App.tsx
// provides them at the route-tree level, but this file mounts SettingsPage
// standalone.
function renderSettingsPage() {
  return render(
    <ThemeProvider>
      <NotificationProvider>
        <SettingsPage />
      </NotificationProvider>
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

// FOR-119: ProfileSection/UnitsSection now read the real FOR-107 profile
// endpoint instead of a static mock; `sex` is left unset here on purpose so
// the existing "No especificado" assertion below still exercises the
// first-run-default display path.
// FOR-120: ThemeProvider (wrapping SettingsPage above) also reads/persists
// through this module now, so `themeMode` and `updateThemeMode` are included
// too -- 'SYSTEM' matches ThemeProvider's own default local mode, so its
// mount-time reconciliation is a no-op here.
vi.mock('../api/profile', () => ({
  getProfile: vi.fn().mockResolvedValue({
    name: 'Usuario FORMA',
    email: 'usuario@forma.app',
    heightCm: 178,
    activityLevel: 'MODERATE',
    mainGoal: 'COMPOSICION',
    unitPreferences: { weightUnit: 'KG', heightUnit: 'CM', distanceUnit: 'KM', energyUnit: 'KCAL' },
    themeMode: 'SYSTEM',
  }),
  updateProfileFields: vi.fn(),
  updateThemeMode: vi.fn().mockResolvedValue(undefined),
}));

/**
 * FOR-58: the Ajustes screen composes every grouped section and honestly
 * distinguishes editable (real, working) content from read-only/inert
 * content. Per-section behavior detail lives in each section's own test file;
 * this is the composition-level smoke test.
 */
describe('SettingsPage', () => {
  it('renders every grouped section from the spec', async () => {
    renderSettingsPage();

    expect(screen.getByRole('heading', { name: 'Configuración', level: 1 })).toBeInTheDocument();
    // Every section card is a direct sibling of the page <h1> (no intervening
    // <h2>), so per FOR-112 each section title must render as <h2>.
    // ProfileSection/UnitsSection (FOR-119) now fetch before rendering their
    // heading, so this must await it rather than assert synchronously.
    expect(
      await screen.findByRole('heading', { name: 'Perfil y preferencias', level: 2 }),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Unidades', level: 2 })).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Conexiones e integraciones', level: 2 }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Objetivos por defecto', level: 2 }),
    ).toBeInTheDocument();
    // FOR-119: distinct entry point, resolving FOR-58's folded-in deferral.
    expect(
      screen.getByRole('heading', {
        name: 'Preferencias de entrenamiento y nutrición',
        level: 2,
      }),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Notificaciones', level: 2 })).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Seguridad y datos', level: 2 }),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Soporte y ayuda', level: 2 })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Acerca de FORMA', level: 2 })).toBeInTheDocument();
  });

  it('shows the profile summary with name and email loaded from GET /api/v1/profile', async () => {
    renderSettingsPage();

    expect(await screen.findByText('Usuario FORMA')).toBeInTheDocument();
    expect(screen.getByText('usuario@forma.app')).toBeInTheDocument();
  });

  it('mounts the reused FOR-57 integrations section', () => {
    renderSettingsPage();

    expect(vi.mocked(listIntegrations)).toHaveBeenCalled();
  });

  it('distinguishes editable content (Conexiones, Editar perfil) from read-only/inert content', async () => {
    renderSettingsPage();

    // Editable: Conexiones renders real, enabled action buttons (FOR-57, already working).
    const withingsButton = await screen.findByRole('button', { name: 'Sincronizar ahora' });
    expect(withingsButton).toBeEnabled();

    // Editable (FOR-119): "Editar perfil" is now a real, enabled entry point.
    expect(await screen.findByRole('button', { name: 'Editar perfil' })).toBeEnabled();

    // Read-only: profile fields are plain text, not any kind of control.
    expect(screen.queryByRole('button', { name: /Sexo/ })).not.toBeInTheDocument();
    // First-run default display (sex left unset in the fixture above).
    expect(screen.getAllByText('No especificado').length).toBeGreaterThan(0);
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
    // Wait out ProfileSection/UnitsSection's fetch (FOR-119) so the scan runs
    // against their settled, ready state rather than the loading spinner.
    await screen.findByRole('button', { name: 'Editar perfil' });

    expect(await axe(container)).toHaveNoViolations();
  });
});
