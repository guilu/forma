import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { App } from '../App';

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
    expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();

    // "Objetivos" is a secondary section, so it appears once (sidebar only).
    const link = screen.getByRole('link', { name: 'Objetivos' });
    await user.click(link);

    expect(screen.getByRole('heading', { name: 'Objetivos' })).toBeInTheDocument();
    expect(link).toHaveAttribute('aria-current', 'page');
  });
});
