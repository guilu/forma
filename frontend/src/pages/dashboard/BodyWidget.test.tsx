import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { BodyWidget } from './BodyWidget';
import { listBodyMeasurements, type BodyMeasurement } from '../../api/bodyMeasurements';

vi.mock('../../api/bodyMeasurements', () => ({ listBodyMeasurements: vi.fn() }));

const listMock = vi.mocked(listBodyMeasurements);

function renderWidget() {
  return render(
    <MemoryRouter>
      <BodyWidget />
    </MemoryRouter>,
  );
}

const latest: BodyMeasurement = {
  measuredAt: '2026-07-05T08:00:00Z',
  source: 'MANUAL',
  weightKg: 73.6,
  bodyFatPercentage: 14.7,
  leanMassKg: 62.8,
  bmi: 22.7,
};

describe('BodyWidget', () => {
  beforeEach(() => {
    listMock.mockReset();
  });

  it('shows a loading state while the request resolves', () => {
    listMock.mockReturnValue(new Promise(() => {}));

    renderWidget();

    expect(screen.getByRole('status')).toHaveTextContent('Cargando tu composición corporal');
  });

  it('renders the four metric cards from the latest measurement', async () => {
    listMock.mockResolvedValue([latest]);

    renderWidget();

    expect(await screen.findByRole('heading', { name: 'Peso' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Grasa corporal' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Masa muscular' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'IMC' })).toBeInTheDocument();
    expect(screen.getByText('73.6')).toBeInTheDocument();
    expect(screen.getByText('14.7')).toBeInTheDocument();
    expect(screen.getByText('62.8')).toBeInTheDocument();
    expect(screen.getByText('22.7')).toBeInTheDocument();
  });

  it('renders a weight sparkline when there are at least two measurements', async () => {
    listMock.mockResolvedValue([
      latest,
      { ...latest, measuredAt: '2026-06-28T08:00:00Z', weightKg: 74.1 },
    ]);

    renderWidget();

    expect(await screen.findByRole('img', { name: /Evolución de peso/ })).toBeInTheDocument();
  });

  it('omits the sparkline with a single measurement', async () => {
    listMock.mockResolvedValue([latest]);

    renderWidget();

    await screen.findByRole('heading', { name: 'Peso' });
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });

  it('shows an empty state with no measurements', async () => {
    listMock.mockResolvedValue([]);

    renderWidget();

    // Loading and empty are both announced via role="status" (FOR-60 shared
    // states), so wait for the terminal content instead of the first match.
    await waitFor(() => {
      expect(screen.getByRole('status')).toHaveTextContent('Aún no hay mediciones');
    });
    expect(screen.queryByRole('heading', { name: 'Peso' })).not.toBeInTheDocument();
  });

  it('shows an error state when the request fails', async () => {
    listMock.mockRejectedValue(new Error('network'));

    renderWidget();

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudo cargar tu composición corporal',
    );
  });

  it('captions each tile with the measurement count (FOR-164: honest count, not an invented delta)', async () => {
    listMock.mockResolvedValue([latest]);

    renderWidget();

    await screen.findByRole('heading', { name: 'Peso' });
    // One caption per tile (4), all reading "1 medición".
    expect(screen.getAllByText('1 medición')).toHaveLength(4);
  });
});
