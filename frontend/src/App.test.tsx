import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { App } from './App';

// Pages that fetch on mount (Dashboard, Nutrition) are stubbed so this routing
// smoke test stays hermetic (no real network). The Dashboard (FOR-51) composes
// widgets from body/training/nutrition/shopping/insights, so all five are stubbed.
vi.mock('./api/bodyMeasurements', () => ({
  listBodyMeasurements: vi.fn().mockResolvedValue([]),
}));
vi.mock('./api/training', () => ({
  getTrainingWeek: vi.fn().mockResolvedValue({ days: [] }),
}));
vi.mock('./api/nutrition', () => ({
  getNutritionDay: vi.fn().mockResolvedValue({ type: 'RUNNING', targets: {}, meals: [] }),
}));
vi.mock('./api/shopping', () => ({
  getShoppingList: vi
    .fn()
    .mockResolvedValue({ items: [], budget: { weeklyEur: 0, monthlyEur: 0 } }),
}));
vi.mock('./api/insights', () => ({
  getWeeklyInsights: vi.fn().mockResolvedValue({
    checkIn: { weekStartDate: '2026-07-06' },
    main: { category: 'BODY', severity: 'INFO', message: 'm', reason: 'r', createdAt: 'now' },
    secondary: [],
    generatedAt: 'now',
  }),
}));
vi.mock('./api/integrations', () => ({
  listIntegrations: vi.fn().mockResolvedValue([]),
  completeIntegrationCallback: vi.fn().mockResolvedValue({
    provider: 'WITHINGS',
    status: 'CONNECTED',
    connectedAt: '2026-07-16T15:00:00Z',
  }),
}));

/**
 * Router smoke tests (FOR-81, index-route content owned by FOR-51): the shell
 * mounts, the index route renders the Dashboard, a known route resolves, and
 * unknown routes fall back to the not-found page.
 */
describe('App', () => {
  it('renders the Dashboard on the index route', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    );

    // Generic greeting: no profile mocked here, so the name stays unset
    // (FOR-169 empty first-run).
    expect(screen.getByRole('heading', { name: 'Hola 👋' })).toBeInTheDocument();
    // The persistent shell navigation is present.
    expect(screen.getAllByRole('navigation').length).toBeGreaterThan(0);
  });

  it('renders a known section route', () => {
    render(
      <MemoryRouter initialEntries={['/nutricion']}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: 'Nutrición' })).toBeInTheDocument();
  });

  it('renders the FOR-58 settings screen at /ajustes', () => {
    render(
      <MemoryRouter initialEntries={['/ajustes']}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: 'Configuración' })).toBeInTheDocument();
  });

  it('renders the FOR-57 integrations screen at its standalone sub-route', () => {
    render(
      <MemoryRouter initialEntries={['/ajustes/integraciones']}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: 'Integraciones' })).toBeInTheDocument();
  });

  it('renders the FOR-59 onboarding flow at /onboarding, outside the AppShell', () => {
    render(
      <MemoryRouter initialEntries={['/onboarding']}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: 'Perfil' })).toBeInTheDocument();
    // Onboarding is not a nav section (app/navigation.ts) and is not wrapped in
    // AppShell, so the persistent sidebar/mobile nav must not be present.
    expect(screen.queryAllByRole('navigation')).toHaveLength(0);
  });

  it('renders the FOR-133 auth callback route at /auth, outside the AppShell', async () => {
    render(
      <MemoryRouter initialEntries={['/auth?code=abc&state=xyz']}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: 'Conexión con Withings' })).toBeInTheDocument();
    // Same rationale as /onboarding: a mid-flow OAuth landing renders outside
    // AppShell, so the persistent sidebar/mobile nav must not be present.
    expect(screen.queryAllByRole('navigation')).toHaveLength(0);
  });

  it('falls back to the not-found page for unknown routes', () => {
    render(
      <MemoryRouter initialEntries={['/does-not-exist']}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: 'Página no encontrada' })).toBeInTheDocument();
  });
});
