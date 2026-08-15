import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ButtonLink } from './ButtonLink';
import type { ButtonVariant } from './Button';

/**
 * Button-shaped navigation tests. The whole point is that it looks like a
 * {@link Button} while staying a real link, so these pin the part that must not
 * regress: it reaches the accessibility tree as a link with an href, never as a
 * button. A `<button>` that navigates loses middle-click, "open in new tab" and
 * the browser's own link affordances.
 */
describe('ButtonLink', () => {
  const variants: ButtonVariant[] = ['primary', 'accent', 'soft', 'secondary', 'ghost'];

  it.each(variants)('renders the %s variant as a link, not a button', (variant) => {
    render(
      <MemoryRouter>
        <ButtonLink variant={variant} to="/app">
          Ver mi progreso
        </ButtonLink>
      </MemoryRouter>,
    );

    expect(screen.getByRole('link', { name: 'Ver mi progreso' })).toHaveAttribute('href', '/app');
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('forwards the caller className so pages keep control of sizing and layout', () => {
    render(
      <MemoryRouter>
        <ButtonLink to="/registro" className="page-local">
          Empezar
        </ButtonLink>
      </MemoryRouter>,
    );

    expect(screen.getByRole('link', { name: 'Empezar' })).toHaveClass('page-local');
  });
});
