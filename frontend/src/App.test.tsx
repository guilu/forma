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

    expect(screen.getByRole('heading', { name: 'Hola Diego 👋' })).toBeInTheDocument();
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

  it('falls back to the not-found page for unknown routes', () => {
    render(
      <MemoryRouter initialEntries={['/does-not-exist']}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: 'Página no encontrada' })).toBeInTheDocument();
  });
});
