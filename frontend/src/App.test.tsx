import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import type { ReactNode } from 'react';
import { App } from './App';

let authStatus: 'authenticated' | 'anonymous' = 'authenticated';

afterEach(() => {
  authStatus = 'authenticated';
});

vi.mock('./auth/AuthContext', () => ({
  AuthProvider: ({ children }: { children: ReactNode }) => children,
  useAuth: () => ({
    status: authStatus,
    user: authStatus === 'authenticated' ? { id: 'user-1', email: 'persona@example.com' } : null,
    bootstrapError: false,
    logout: vi.fn(),
    refreshCurrentUser: vi.fn(),
  }),
}));

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
// Route pages are code-split (app/routes.tsx), so these waits cover a dynamic
// import resolving, not just a render. Under full-suite load that can exceed
// testing-library's 1s default, which made these assertions flaky.
const CHUNK_TIMEOUT = { timeout: 5000 };

describe('App', () => {
  it('renders the public landing on the index route', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    );

    // Generic greeting: no profile mocked here, so the name stays unset
    // (FOR-169 empty first-run).
    expect(
      screen.getByRole('heading', { level: 1, name: /Entrena\. Nutre\. Evoluciona\./ }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Hola 👋' })).not.toBeInTheDocument();
    // FOR-185: the navigation bar is rendered by the global RootLayout on every
    // route, not by the landing page itself. This case has an authenticated
    // session mocked, so the bar shows its account face rather than the public
    // anchors — see layout/shell.test.tsx for the anonymous variant.
    expect(screen.getByRole('banner')).toBeInTheDocument();
  });

  it('renders the Dashboard at the protected /app entry point', async () => {
    render(
      <MemoryRouter initialEntries={['/app']}>
        <App />
      </MemoryRouter>,
    );

    expect(
      await screen.findByRole('heading', { name: 'Hola 👋' }, CHUNK_TIMEOUT),
    ).toBeInTheDocument();
    expect(screen.getAllByRole('navigation').length).toBeGreaterThan(0);
  });

  it.each([
    '/app',
    '/app/mediciones',
    '/app/entrenamiento',
    '/app/nutricion',
    '/app/lista-compra',
    '/app/progreso',
    '/app/objetivos',
    '/app/ajustes',
    '/app/ajustes/integraciones',
    '/onboarding',
  ])('redirects an anonymous user from protected route %s', (path) => {
    authStatus = 'anonymous';
    render(
      <MemoryRouter initialEntries={[path]}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument();
  });

  it.each([
    ['/', /Entrena\. Nutre\. Evoluciona\./],
    ['/auth?code=abc&state=xyz', /Conexión con Withings/],
  ])('keeps %s public for anonymous users', async (path, heading) => {
    authStatus = 'anonymous';
    render(
      <MemoryRouter initialEntries={[path]}>
        <App />
      </MemoryRouter>,
    );

    expect(
      await screen.findByRole('heading', { name: heading }, CHUNK_TIMEOUT),
    ).toBeInTheDocument();
  });

  it('renders a known section route', async () => {
    render(
      <MemoryRouter initialEntries={['/app/nutricion']}>
        <App />
      </MemoryRouter>,
    );

    expect(
      await screen.findByRole('heading', { name: 'Nutrición' }, CHUNK_TIMEOUT),
    ).toBeInTheDocument();
  });

  it('renders the FOR-58 settings screen at /ajustes', async () => {
    render(
      <MemoryRouter initialEntries={['/app/ajustes']}>
        <App />
      </MemoryRouter>,
    );

    expect(
      await screen.findByRole('heading', { name: 'Configuración' }, CHUNK_TIMEOUT),
    ).toBeInTheDocument();
  });

  it('renders the FOR-57 integrations screen at its standalone sub-route', async () => {
    render(
      <MemoryRouter initialEntries={['/app/ajustes/integraciones']}>
        <App />
      </MemoryRouter>,
    );

    expect(
      await screen.findByRole('heading', { name: 'Integraciones' }, CHUNK_TIMEOUT),
    ).toBeInTheDocument();
  });

  it('renders the FOR-59 onboarding flow at /onboarding, outside the AppShell', async () => {
    render(
      <MemoryRouter initialEntries={['/onboarding']}>
        <App />
      </MemoryRouter>,
    );

    expect(
      await screen.findByRole('heading', { name: 'Perfil' }, CHUNK_TIMEOUT),
    ).toBeInTheDocument();
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

    expect(
      await screen.findByRole('heading', { name: 'Conexión con Withings' }, CHUNK_TIMEOUT),
    ).toBeInTheDocument();
    // Same rationale as /onboarding: a mid-flow OAuth landing renders outside
    // AppShell, so the persistent sidebar/mobile nav must not be present.
    expect(screen.queryAllByRole('navigation')).toHaveLength(0);
  });

  it('falls back to the not-found page for unknown routes', async () => {
    render(
      <MemoryRouter initialEntries={['/does-not-exist']}>
        <App />
      </MemoryRouter>,
    );

    expect(
      await screen.findByRole('heading', { name: 'Página no encontrada' }, CHUNK_TIMEOUT),
    ).toBeInTheDocument();
  });
});
