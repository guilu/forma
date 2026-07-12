import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ErrorState } from './ErrorState';

describe('ErrorState', () => {
  it('renders the message announced via role="alert"', () => {
    render(<ErrorState message="No se pudieron cargar tus mediciones." />);

    expect(screen.getByRole('alert')).toHaveTextContent('No se pudieron cargar tus mediciones.');
  });

  it('re-invokes the loader when the retry action is used', async () => {
    const onRetry = vi.fn();
    render(<ErrorState message="No se pudo cargar." onRetry={onRetry} />);

    await userEvent.setup().click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it('renders no retry button when onRetry is not supplied', () => {
    render(<ErrorState message="No se pudo cargar." />);

    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('never renders raw exception/stack detail by default, even when supplied', () => {
    render(
      <ErrorState
        message="No se pudo cargar."
        detail="TypeError: Cannot read properties of undefined at fetchMeasurements (bodyMeasurements.ts:42)"
      />,
    );

    expect(
      screen.queryByText(/TypeError|at fetchMeasurements|bodyMeasurements\.ts/),
    ).not.toBeInTheDocument();
  });

  it('only renders dev-only detail when the caller explicitly opts in via showDetail', () => {
    render(
      <ErrorState
        message="No se pudo cargar."
        detail="TypeError: Cannot read properties of undefined (bodyMeasurements.ts:42)"
        showDetail
      />,
    );

    expect(screen.getByText(/TypeError/)).toBeInTheDocument();
  });

  it('accepts a custom retry label', async () => {
    const onRetry = vi.fn();
    render(
      <ErrorState message="No se pudo cargar." onRetry={onRetry} retryLabel="Volver a intentar" />,
    );

    expect(screen.getByRole('button', { name: 'Volver a intentar' })).toBeInTheDocument();
  });
});
