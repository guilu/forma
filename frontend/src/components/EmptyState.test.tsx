import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { EmptyState } from './EmptyState';
import { Button } from './Button';

describe('EmptyState', () => {
  it('renders the feature-empty message announced via role="status"', () => {
    render(<EmptyState title="Aún no hay mediciones." />);

    expect(screen.getByRole('status')).toHaveTextContent('Aún no hay mediciones.');
  });

  it('renders an optional primary action that stays reachable and operable', async () => {
    const onClick = vi.fn();
    render(
      <EmptyState
        title="Aún no hay mediciones."
        action={
          <Button type="button" onClick={onClick}>
            Registrar medición
          </Button>
        }
      />,
    );

    await userEvent.setup().click(screen.getByRole('button', { name: 'Registrar medición' }));
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('renders a distinct message for the filtered-empty variant', () => {
    render(
      <EmptyState
        variant="filtered"
        title="No hay artículos que coincidan con el filtro actual."
      />,
    );

    expect(screen.getByRole('status')).toHaveTextContent(
      'No hay artículos que coincidan con el filtro actual.',
    );
  });

  it('renders feature and filtered variants with distinguishable content', () => {
    const { rerender } = render(<EmptyState title="Aún no hay mediciones." />);
    const featureMarkup = screen.getByRole('status').innerHTML;

    rerender(<EmptyState variant="filtered" title="Sin resultados para este filtro." />);
    const filteredMarkup = screen.getByRole('status').innerHTML;

    expect(featureMarkup).not.toBe(filteredMarkup);
  });
});
