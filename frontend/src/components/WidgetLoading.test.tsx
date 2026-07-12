import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { WidgetLoading } from './WidgetLoading';

describe('WidgetLoading', () => {
  it('announces loading via role="status" with a default sr-only label', () => {
    render(<WidgetLoading />);

    expect(screen.getByRole('status')).toHaveTextContent('Cargando…');
  });

  it('renders the requested number of skeleton bars to reserve space and avoid layout jump', () => {
    const { container } = render(<WidgetLoading rows={4} />);

    // Skeleton bars are decorative (aria-hidden); assert via DOM structure instead of role.
    expect(container.querySelectorAll('[aria-hidden="true"]')).toHaveLength(4);
  });

  it('accepts a caller-supplied accessible label', () => {
    render(<WidgetLoading label="Cargando tu semana…" />);

    expect(screen.getByRole('status')).toHaveTextContent('Cargando tu semana…');
  });
});
