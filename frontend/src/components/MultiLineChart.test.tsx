import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MultiLineChart, type Series } from './MultiLineChart';

const series: Series[] = [
  {
    label: 'Peso (kg)',
    color: 'var(--color-accent)',
    points: [
      { t: 1, y: 74 },
      { t: 2, y: 73 },
      { t: 3, y: 72 },
    ],
  },
  {
    label: 'Grasa (%)',
    color: 'var(--color-warning)',
    points: [
      { t: 1, y: 16 },
      { t: 2, y: 15 },
      { t: 3, y: 14 },
    ],
  },
];

describe('MultiLineChart', () => {
  it('renders one polyline per series with the accessible summary and a legend', () => {
    const { container } = render(
      <MultiLineChart
        series={series}
        startLabel="11 may"
        endLabel="8 jun"
        ariaLabel="Tendencia de peso y grasa"
      />,
    );

    expect(screen.getByRole('img', { name: 'Tendencia de peso y grasa' })).toBeInTheDocument();
    expect(container.querySelectorAll('polyline')).toHaveLength(2);
    // Legend names each series.
    expect(screen.getByText('Peso (kg)')).toBeInTheDocument();
    expect(screen.getByText('Grasa (%)')).toBeInTheDocument();
    // Shared x-axis labels.
    expect(screen.getByText('11 may')).toBeInTheDocument();
    expect(screen.getByText('8 jun')).toBeInTheDocument();
  });
});
