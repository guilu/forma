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

  it('draws a marker per point and a single series line', () => {
    const { container } = render(
      <LineChart points={points} formatValue={(v) => v.toFixed(1)} ariaLabel="Peso" />,
    );

    expect(container.querySelectorAll('circle')).toHaveLength(points.length);
    expect(container.querySelectorAll('polyline')).toHaveLength(1);
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
