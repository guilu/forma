import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { App } from '../App';

// The Dashboard (index route, FOR-51) fetches from several feature APIs on mount;
// stub them all so this navigation test stays hermetic.
vi.mock('../api/bodyMeasurements', () => ({
  listBodyMeasurements: vi.fn().mockResolvedValue([]),
}));
vi.mock('../api/training', () => ({
  getTrainingWeek: vi.fn().mockResolvedValue({ days: [] }),
}));
vi.mock('../api/nutrition', () => ({
  getNutritionDay: vi.fn().mockResolvedValue({ type: 'RUNNING', targets: {}, meals: [] }),
}));
vi.mock('../api/shopping', () => ({
  getShoppingList: vi
    .fn()
    .mockResolvedValue({ items: [], budget: { weeklyEur: 0, monthlyEur: 0 } }),
}));
vi.mock('../api/insights', () => ({
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
describe('sidebar navigation', () => {
  it('navigates to a section when its link is clicked', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    );

    // Starts on the Dashboard.
    expect(screen.getByRole('heading', { name: 'Hola Diego 👋' })).toBeInTheDocument();

    // "Objetivos" is a secondary section, so it appears once (sidebar only).
    const link = screen.getByRole('link', { name: 'Objetivos' });
    await user.click(link);

    expect(screen.getByRole('heading', { name: 'Objetivos' })).toBeInTheDocument();
    expect(link).toHaveAttribute('aria-current', 'page');
  });

  // FOR-61: core navigation must be reachable and operable without a mouse —
  // focusing a nav link and pressing Enter (native `<a>` semantics) must
  // navigate exactly like a click does.
  it('navigates to a section using only the keyboard', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    );

    const link = screen.getByRole('link', { name: 'Mediciones' });
    link.focus();
    expect(link).toHaveFocus();

    await user.keyboard('{Enter}');

    expect(screen.getByRole('heading', { name: 'Mediciones' })).toBeInTheDocument();
    expect(link).toHaveAttribute('aria-current', 'page');
  });
});
