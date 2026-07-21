import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { FirstSummaryWidget } from './FirstSummaryWidget';
import { listBodyMeasurements, type BodyMeasurement } from '../../api/bodyMeasurements';

vi.mock('../../api/bodyMeasurements', () => ({ listBodyMeasurements: vi.fn() }));

const listMock = vi.mocked(listBodyMeasurements);

const measurement: BodyMeasurement = {
  measuredAt: '2026-07-05T08:00:00Z',
  source: 'MANUAL',
  weightKg: 73.6,
  bodyFatPercentage: 14.7,
  leanMassKg: 62.8,
  bmi: 22.7,
};

describe('FirstSummaryWidget', () => {
  beforeEach(() => {
    listMock.mockReset();
  });

  it('celebrates the first recorded measurement with the real count', async () => {
    listMock.mockResolvedValue([measurement]);

    render(<FirstSummaryWidget />);

    expect(await screen.findByText('¡Buen trabajo!')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: '1 medición registrada' })).toBeInTheDocument();
  });

  it('nudges the user to record their first measurement when there are none', async () => {
    listMock.mockResolvedValue([]);

    render(<FirstSummaryWidget />);

    expect(await screen.findByText('¡Empecemos!')).toBeInTheDocument();
    expect(
      screen.getByRole('img', { name: '0 mediciones registradas' }),
    ).toBeInTheDocument();
  });

  it('shows an error state when the request fails', async () => {
    listMock.mockRejectedValue(new Error('network'));

    render(<FirstSummaryWidget />);

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar tu resumen');
  });
});
