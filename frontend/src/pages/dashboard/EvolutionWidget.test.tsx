import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { EvolutionWidget } from './EvolutionWidget';
import { listBodyMeasurements, type BodyMeasurement } from '../../api/bodyMeasurements';

vi.mock('../../api/bodyMeasurements', () => ({ listBodyMeasurements: vi.fn() }));

const listMock = vi.mocked(listBodyMeasurements);

const base: BodyMeasurement = {
  measuredAt: '2026-07-05T08:00:00Z',
  source: 'MANUAL',
  weightKg: 69.2,
  bodyFatPercentage: 12.1,
  leanMassKg: 62.9,
  bmi: 21.3,
};

const history = [
  base,
  { ...base, measuredAt: '2026-06-28T08:00:00Z', weightKg: 70.5, bodyFatPercentage: 13.0 },
];

describe('EvolutionWidget', () => {
  beforeEach(() => {
    listMock.mockReset();
  });

  it('shows the latest value for the default metric and plots the series', async () => {
    listMock.mockResolvedValue(history);

    render(<EvolutionWidget />);

    // Latest weight highlighted.
    expect(await screen.findByText('69.2')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /Evolución de peso/ })).toBeInTheDocument();
  });

  it('re-plots a different backed metric when the selector changes', async () => {
    listMock.mockResolvedValue(history);
    const user = userEvent.setup();

    render(<EvolutionWidget />);
    await screen.findByText('69.2');

    await user.selectOptions(screen.getByRole('combobox', { name: 'Métrica' }), 'fat');

    // Latest body-fat value + a body-fat trend.
    expect(screen.getByText('12.1')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /Evolución de grasa/ })).toBeInTheDocument();
  });

  it('shows an empty state when there are no measurements', async () => {
    listMock.mockResolvedValue([]);

    render(<EvolutionWidget />);

    expect(
      await screen.findByText(/Aún no hay mediciones para mostrar tu evolución/),
    ).toBeInTheDocument();
  });

  it('shows an error state when the request fails', async () => {
    listMock.mockRejectedValue(new Error('network'));

    render(<EvolutionWidget />);

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar tu evolución');
  });
});
