import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { LoadingState } from './LoadingState';

describe('LoadingState', () => {
  it('renders a default message announced via role="status"', () => {
    render(<LoadingState />);

    expect(screen.getByRole('status')).toHaveTextContent('Cargando…');
  });

  it('renders a caller-supplied domain-specific message', () => {
    render(<LoadingState message="Cargando tus mediciones…" />);

    expect(screen.getByRole('status')).toHaveTextContent('Cargando tus mediciones…');
  });
});
