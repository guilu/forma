import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ProgressPage } from './ProgressPage';
import { listBodyMeasurements, type BodyMeasurement } from '../api/bodyMeasurements';

vi.mock('../api/bodyMeasurements', () => ({
  listBodyMeasurements: vi.fn(),
}));

const listMock = vi.mocked(listBodyMeasurements);

function renderProgress() {
  return render(
    <MemoryRouter>
      <ProgressPage />
    </MemoryRouter>,
  );
}

/** Builds `count` measurements, newest-first (like the API), one day apart. */
function measurements(count: number): BodyMeasurement[] {
  return Array.from({ length: count }, (_, i) => ({
    measuredAt: `2026-07-${String(count - i).padStart(2, '0')}T08:00:00Z`,
    source: 'MANUAL',
    weightKg: 80 - i,
    bodyFatPercentage: 20 - i * 0.1,
    leanMassKg: 60 - i,
    bmi: 24,
  }));
}

describe('ProgressPage', () => {
  beforeEach(() => {
    listMock.mockReset();
  });

  it('renders a graph per metric from the measurements', async () => {
    listMock.mockResolvedValue(measurements(4));

    renderProgress();

    // One chart (role img) per metric: weight, body fat %, lean mass.
    expect(await screen.findByRole('img', { name: /Evolución de peso/ })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /Evolución de grasa corporal/ })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /Evolución de masa magra/ })).toBeInTheDocument();
    expect(screen.getAllByRole('img')).toHaveLength(3);
  });

  it('plots only the recent window (last 12 points)', async () => {
    listMock.mockResolvedValue(measurements(15));

    renderProgress();

    // The weight chart's label reports the plotted point count.
    expect(
      await screen.findByRole('img', { name: /Evolución de peso: 12 mediciones/ }),
    ).toBeInTheDocument();
  });

  it('shows an empty state with a link when there are no measurements', async () => {
    listMock.mockResolvedValue([]);

    renderProgress();

    expect(await screen.findByRole('status')).toHaveTextContent('Aún no hay mediciones');
    expect(screen.getByRole('link', { name: 'Registra tu primera medición' })).toHaveAttribute(
      'href',
      '/mediciones',
    );
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });

  it('falls back to a message when there is a single measurement (no line)', async () => {
    listMock.mockResolvedValue(measurements(1));

    renderProgress();

    expect(
      (await screen.findAllByText('Necesitas al menos dos mediciones para ver la evolución.'))
        .length,
    ).toBe(3);
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });

  it('shows an error state when the API call fails', async () => {
    listMock.mockRejectedValue(new Error('network'));

    renderProgress();

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar tu evolución');
  });
});
