import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ChartContainer } from './ChartContainer';
import { LineChart, type ChartPoint } from './LineChart';

const points: ChartPoint[] = [
  { t: Date.parse('2026-07-01T08:00:00Z'), y: 75.1, dateLabel: '1 jul' },
  { t: Date.parse('2026-07-05T08:00:00Z'), y: 73.6, dateLabel: '5 jul' },
];

describe('ChartContainer', () => {
  it('renders a titled card wrapping its chart children by default', () => {
    render(
      <ChartContainer title="Evolución de peso">
        <LineChart points={points} formatValue={(v) => v.toFixed(1)} ariaLabel="Peso" />
      </ChartContainer>,
    );

    expect(screen.getByRole('heading', { name: 'Evolución de peso' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Peso' })).toBeInTheDocument();
  });

  it('renders an optional header action', () => {
    render(
      <ChartContainer title="Evolución de peso" action={<button type="button">Ver todo</button>}>
        <LineChart points={points} formatValue={(v) => v.toFixed(1)} ariaLabel="Peso" />
      </ChartContainer>,
    );

    expect(screen.getByRole('button', { name: 'Ver todo' })).toBeInTheDocument();
  });

  it('renders a loading frame instead of chart children', () => {
    render(
      <ChartContainer title="Evolución de peso" state="loading">
        <LineChart points={points} formatValue={(v) => v.toFixed(1)} ariaLabel="Peso" />
      </ChartContainer>,
    );

    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(screen.queryByRole('img', { name: 'Peso' })).not.toBeInTheDocument();
  });

  it('renders an empty frame with a default message', () => {
    render(
      <ChartContainer title="Evolución de peso" state="empty">
        <LineChart points={points} formatValue={(v) => v.toFixed(1)} ariaLabel="Peso" />
      </ChartContainer>,
    );

    expect(screen.getByText('Todavía no hay datos suficientes.')).toBeInTheDocument();
    expect(screen.queryByRole('img', { name: 'Peso' })).not.toBeInTheDocument();
  });

  it('renders a custom empty message when provided', () => {
    render(
      <ChartContainer title="Evolución de peso" state="empty" emptyMessage="Sin mediciones aún.">
        <LineChart points={points} formatValue={(v) => v.toFixed(1)} ariaLabel="Peso" />
      </ChartContainer>,
    );

    expect(screen.getByText('Sin mediciones aún.')).toBeInTheDocument();
  });
});
