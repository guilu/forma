import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { NoPlanEmptyState } from './NoPlanEmptyState';

/**
 * The CTA used to point at `/plan`, the public anonymous funnel: a logged-in
 * user following it filled a form that produced nothing for their account,
 * since leads are deliberately not linked to `users` (V61). This pins it at
 * the in-app creator that actually works for a signed-in account instead.
 */
describe('NoPlanEmptyState', () => {
  it('sends a signed-in user to the plan creator that works, not the public funnel', () => {
    render(
      <MemoryRouter>
        <NoPlanEmptyState />
      </MemoryRouter>,
    );

    const cta = screen.getByRole('link', { name: 'Crear mi plan' });
    expect(cta).toHaveAttribute('href', '/app/nutrition/plans');
  });
});
