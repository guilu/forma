import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { TrendWidget } from './TrendWidget';
import { listBodyMeasurements, type BodyMeasurement } from '../../api/bodyMeasurements';

vi.mock('../../api/bodyMeasurements', () => ({ listBodyMeasurements: vi.fn() }));

const listMock = vi.mocked(listBodyMeasurements);

const base: BodyMeasurement = {
  measuredAt: '2026-07-05T08:00:00Z',
  source: 'MANUAL',
  weightKg: 73.6,
  bodyFatPercentage: 14.7,
  leanMassKg: 62.8,
  bmi: 22.7,
};

describe('TrendWidget', () => {
  beforeEach(() => {
    listMock.mockReset();
  });

  it('shows the honest "not enough data" copy with fewer than two measurements', async () => {
    listMock.mockResolvedValue([base]);

    render(<TrendWidget />);

    expect(
      await screen.findByText(/Aún no hay suficientes datos para mostrar la tendencia/),
    ).toBeInTheDocument();
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });

  it('plots the trend line once there are at least two measurements', async () => {
    listMock.mockResolvedValue([
      base,
      { ...base, measuredAt: '2026-06-28T08:00:00Z', weightKg: 74.1 },
    ]);

    render(<TrendWidget />);

    expect(await screen.findByRole('img', { name: /Tendencia de peso/ })).toBeInTheDocument();
  });

  it('shows an error state when the request fails', async () => {
    listMock.mockRejectedValue(new Error('network'));

    render(<TrendWidget />);

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar tu tendencia');
  });
});
