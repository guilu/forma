import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { LineChart, type ChartPoint } from './LineChart';

const points: ChartPoint[] = [
  { t: Date.parse('2026-07-01T08:00:00Z'), y: 75.1, dateLabel: '1 jul' },
  { t: Date.parse('2026-07-03T08:00:00Z'), y: 74.2, dateLabel: '3 jul' },
  { t: Date.parse('2026-07-05T08:00:00Z'), y: 73.6, dateLabel: '5 jul' },
];

describe('LineChart', () => {
  it('renders an accessible chart with the provided label', () => {
    render(<LineChart points={points} formatValue={(v) => v.toFixed(1)} ariaLabel="Peso: baja" />);

    expect(screen.getByRole('img', { name: 'Peso: baja' })).toBeInTheDocument();
  });

  it('draws one series line with a gradient fill and no per-point dots', () => {
    const { container } = render(
      <LineChart points={points} formatValue={(v) => v.toFixed(1)} ariaLabel="Peso" />,
    );

    expect(container.querySelectorAll('.recharts-area-curve')).toHaveLength(1);
    expect(container.querySelectorAll('.recharts-area-area')).toHaveLength(1);
    // A dot per measurement turns a dense series into beads on a string; only
    // the hovered point gets one.
    expect(container.querySelectorAll('.recharts-dot')).toHaveLength(0);
  });

  it('strips the axes and grid in the sparkline variant', () => {
    const { container } = render(
      <LineChart
        variant="spark"
        points={points}
        formatValue={(v) => v.toFixed(1)}
        ariaLabel="Peso"
      />,
    );

    // Still a labelled, readable trend...
    expect(screen.getByRole('img', { name: 'Peso' })).toBeInTheDocument();
    expect(container.querySelectorAll('.recharts-area-curve')).toHaveLength(1);
    // ...but none of the furniture, which is illegible at tile height.
    expect(container.querySelector('.recharts-cartesian-grid')).toBeNull();
    expect(screen.queryByText('1 jul')).not.toBeInTheDocument();
  });

  /**
   * Stacked charts only compare if they share an x scale. Left to itself each
   * one spans its own first and last measurement, so two metrics recorded on
   * different days would draw the same week at two different widths — and the
   * card's single pair of date labels would be a lie for at least one of them.
   */
  it('pins the x axis to the given window instead of to the data', () => {
    const from = Date.parse('2026-06-01T00:00:00Z');
    const to = Date.parse('2026-08-01T00:00:00Z');

    const { container: loose } = render(
      <LineChart points={points} formatValue={(v) => v.toFixed(1)} ariaLabel="Peso" />,
    );
    const { container: pinned } = render(
      <LineChart
        points={points}
        xDomain={[from, to]}
        formatValue={(v) => v.toFixed(1)}
        ariaLabel="Peso"
      />,
    );

    const startX = (container: HTMLElement) =>
      Number(
        /^M([\d.]+),/.exec(
          container.querySelector('.recharts-area-curve')?.getAttribute('d') ?? '',
        )?.[1],
      );

    // Without a window the first point sits on the plot's left edge; with one it
    // starts a month in, where 1 July actually falls inside June–August.
    expect(startX(pinned)).toBeGreaterThan(startX(loose));
  });

  it('labels the first and last dates and the y range', () => {
    render(<LineChart points={points} formatValue={(v) => v.toFixed(1)} ariaLabel="Peso" />);

    expect(screen.getByText('1 jul')).toBeInTheDocument();
    expect(screen.getByText('5 jul')).toBeInTheDocument();
    // y-axis min/max labels.
    expect(screen.getByText('75.1')).toBeInTheDocument();
    expect(screen.getByText('73.6')).toBeInTheDocument();
  });
});
