import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { PlanBanner } from './PlanBanner';

/**
 * The banner used to say "Tu plan está en marcha" unconditionally, even to a
 * brand-new account with zero plans — the first thing it told a new user was
 * false. `hasPlan` is the account's real plan state (lifted from the same read
 * {@link DashboardPage} already makes for `NutritionWidget`), so these pin the
 * three states honestly: a real plan, no plan yet, and "don't know yet" while
 * that read is still in flight.
 */
function renderBanner(hasPlan: boolean | undefined) {
  return render(
    <MemoryRouter>
      <PlanBanner hasPlan={hasPlan} />
    </MemoryRouter>,
  );
}

describe('PlanBanner', () => {
  it('tells the truth when there is a plan under way', () => {
    renderBanner(true);

    expect(screen.getByText('Tu plan está en marcha 🚀')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ver mi progreso' })).toHaveAttribute(
      'href',
      '/app/progress',
    );
  });

  it('tells the truth when there is no plan, and offers the creator that actually works', () => {
    renderBanner(false);

    expect(screen.queryByText('Tu plan está en marcha 🚀')).not.toBeInTheDocument();
    expect(screen.getByText('Todavía no tienes un plan')).toBeInTheDocument();
    const cta = screen.getByRole('link', { name: 'Crear mi plan' });
    expect(cta).toHaveAttribute('href', '/app/nutrition/plans');
  });

  it('renders nothing while the plan check is still in flight, rather than flash the wrong state', () => {
    const { container } = renderBanner(undefined);

    expect(container).toBeEmptyDOMElement();
  });
});
