import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { App } from '../App';
import type { ReactNode } from 'react';

vi.mock('../auth/AuthContext', () => ({
  AuthProvider: ({ children }: { children: ReactNode }) => children,
  useAuth: () => ({
    status: 'authenticated',
    user: { id: 'user-1', email: 'persona@example.com' },
    bootstrapError: false,
    logout: vi.fn(),
    refreshCurrentUser: vi.fn(),
  }),
}));

// The Dashboard (index route, FOR-51) fetches from several feature APIs on mount;
// stub them all so this navigation test stays hermetic.
vi.mock('../api/bodyMeasurements', () => ({
  listBodyMeasurements: vi.fn().mockResolvedValue([]),
}));
vi.mock('../api/training', () => ({
  getTrainingWeek: vi.fn().mockResolvedValue({ days: [] }),
}));
vi.mock('../api/nutrition', () => ({
  getDayConsumption: vi.fn().mockResolvedValue({
    dayType: null,
    consumed: { kcal: 0, proteinG: 0, carbsG: 0, fatG: 0 },
    target: null,
  }),
  getNutritionDay: vi.fn().mockResolvedValue({ type: 'RUNNING', targets: {}, meals: [] }),
}));
vi.mock('../api/shopping', () => ({
  getShoppingList: vi
    .fn()
    .mockResolvedValue({ items: [], budget: { weeklyEur: 0, monthlyEur: 0 } }),
}));
vi.mock('../api/insights', () => ({
  // Progreso's insights-history section calls this on mount; without it the
  // mocked module throws on the missing export and the page never renders.
  getInsightsHistory: vi.fn().mockResolvedValue([]),
  getWeeklyInsights: vi.fn().mockResolvedValue({
    checkIn: { weekStartDate: '2026-07-06' },
    main: { category: 'BODY', severity: 'INFO', message: 'm', reason: 'r', createdAt: 'now' },
    secondary: [],
    generatedAt: 'now',
  }),
}));

/**
 * Interaction example (FOR-87). Template for future UI stories: drive the UI with
 * `@testing-library/user-event` and assert the resulting state through the
 * accessible DOM. Here, clicking a sidebar link navigates and marks the link as
 * the current page — no backend, no product data.
 */
// Route pages are code-split (app/routes.tsx), so these waits cover a dynamic
// import resolving, not just a render. Under full-suite load that can exceed
// testing-library's 1s default, which made these assertions flaky.
const CHUNK_TIMEOUT = { timeout: 5000 };

describe('sidebar navigation', () => {
  it('navigates to a section when its link is clicked', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={['/app']}>
        <App />
      </MemoryRouter>,
    );

    // Starts on the Dashboard (generic greeting — no profile is mocked here, so
    // the name stays unset, FOR-169 empty first-run).
    expect(
      await screen.findByRole('heading', { name: 'Hola 👋' }, CHUNK_TIMEOUT),
    ).toBeInTheDocument();

    // "Progreso" is a secondary section, so it appears once (sidebar only).
    const link = screen.getByRole('link', { name: 'Progreso' });
    await user.click(link);

    // Sections are code-split (app/routes.tsx), so the heading arrives with the
    // route's chunk rather than on the click itself.
    expect(
      await screen.findByRole('heading', { name: 'Progreso' }, CHUNK_TIMEOUT),
    ).toBeInTheDocument();
    expect(link).toHaveAttribute('aria-current', 'page');
  });

  // FOR-61: core navigation must be reachable and operable without a mouse —
  // focusing a nav link and pressing Enter (native `<a>` semantics) must
  // navigate exactly like a click does.
  it('navigates to a section using only the keyboard', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={['/app']}>
        <App />
      </MemoryRouter>,
    );

    const link = screen.getByRole('link', { name: 'Mediciones' });
    link.focus();
    expect(link).toHaveFocus();

    await user.keyboard('{Enter}');

    expect(
      await screen.findByRole('heading', { name: 'Mediciones' }, CHUNK_TIMEOUT),
    ).toBeInTheDocument();
    expect(link).toHaveAttribute('aria-current', 'page');
  });
});
