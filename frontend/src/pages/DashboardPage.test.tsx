import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { DashboardPage } from './DashboardPage';
import { listBodyMeasurements, type BodyMeasurement } from '../api/bodyMeasurements';

vi.mock('../api/bodyMeasurements', () => ({
  listBodyMeasurements: vi.fn(),
}));

const listMock = vi.mocked(listBodyMeasurements);

function renderDashboard() {
  return render(
    <MemoryRouter>
      <DashboardPage />
    </MemoryRouter>,
  );
}

const latest: BodyMeasurement = {
  measuredAt: '2026-07-05T08:00:00Z',
  source: 'MANUAL',
  weightKg: 73.6,
  bodyFatPercentage: 14.7,
  fatMassKg: 10.8,
  leanMassKg: 62.8,
  bmi: 22.7,
};

describe('DashboardPage', () => {
  beforeEach(() => {
    listMock.mockReset();
  });

  it('renders the five metric cards from the latest (first) measurement', async () => {
    const older: BodyMeasurement = { ...latest, weightKg: 99.9 };
    listMock.mockResolvedValue([latest, older]);

    renderDashboard();

    // Cards labels.
    expect(await screen.findByRole('heading', { name: 'Peso' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Grasa corporal' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Masa grasa' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Masa magra' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'IMC' })).toBeInTheDocument();

    // Values come from the first (latest) item, not the older one.
    expect(screen.getByText('73.6')).toBeInTheDocument();
    expect(screen.queryByText('99.9')).not.toBeInTheDocument();
    expect(screen.getByText('14.7')).toBeInTheDocument();
    expect(screen.getByText('10.8')).toBeInTheDocument();
    expect(screen.getByText('22.7')).toBeInTheDocument();
  });

  it('rounds to one decimal without fake precision', async () => {
    listMock.mockResolvedValue([{ ...latest, weightKg: 73.456 }]);

    renderDashboard();

    expect(await screen.findByText('73.5')).toBeInTheDocument();
  });

  it('shows an empty state with a link to add a measurement when there are none', async () => {
    listMock.mockResolvedValue([]);

    renderDashboard();

    expect(await screen.findByRole('status')).toHaveTextContent('Aún no hay mediciones');
    expect(screen.getByRole('link', { name: 'Registra tu primera medición' })).toHaveAttribute(
      'href',
      '/mediciones',
    );
    // No metric cards in the empty state.
    expect(screen.queryByRole('heading', { name: 'Peso' })).not.toBeInTheDocument();
  });

  it('shows an error state when the API call fails', async () => {
    listMock.mockRejectedValue(new Error('network'));

    renderDashboard();

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudieron cargar');
    expect(screen.queryByRole('heading', { name: 'Peso' })).not.toBeInTheDocument();
  });
});
