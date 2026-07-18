import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ProgressPage } from './ProgressPage';
import { NotificationProvider } from '../components/NotificationProvider';
import { listBodyMeasurements, type BodyMeasurement } from '../api/bodyMeasurements';
import { getInsightsHistory, getWeeklyInsights, type WeeklyInsights } from '../api/insights';
import { listProgressPhotos } from '../api/progress';
import { axe } from '../test/axe';

vi.mock('../api/bodyMeasurements', () => ({
  listBodyMeasurements: vi.fn(),
}));

// ProgressPage also hosts the FOR-56 InsightsSection and the FOR-124
// InsightsHistorySection, which call the FOR-45/FOR-110 endpoints
// independently — mock them so these measurement-focused tests aren't
// coupled to insights data (dedicated insights tests live in
// ./progress/InsightsSection.test.tsx and ./progress/InsightsHistorySection.test.tsx).
vi.mock('../api/insights', () => ({
  getWeeklyInsights: vi.fn(),
  getInsightsHistory: vi.fn(),
}));

// FOR-144: ProgressPage also hosts ProgressPhotosSection, which calls the
// FOR-140 photos endpoint independently — mock it so these
// measurement-focused tests aren't coupled to photo data (dedicated tests
// live in ./progress/ProgressPhotosSection.test.tsx).
vi.mock('../api/progress', () => ({
  listProgressPhotos: vi.fn(),
}));

const listMock = vi.mocked(listBodyMeasurements);
const insightsMock = vi.mocked(getWeeklyInsights);
const historyMock = vi.mocked(getInsightsHistory);
const photosMock = vi.mocked(listProgressPhotos);

const insights: WeeklyInsights = {
  checkIn: {
    weekStartDate: '2026-07-06',
    plannedRunningSessions: 3,
    completedRunningSessions: 3,
    plannedStrengthSessions: 3,
    completedStrengthSessions: 2,
  },
  main: {
    category: 'BODY',
    severity: 'INFO',
    message: 'Aún no hay suficientes datos para una recomendación.',
    reason: 'Necesitamos al menos una medición y una semana de entrenamiento.',
    createdAt: '2026-07-10T08:00:00Z',
  },
  secondary: [],
  generatedAt: '2026-07-10T08:00:00Z',
};

// FOR-144: ProgressPhotosSection calls `useNotify()`, which requires a
// provider (mirrors IntegrationsSection.test.tsx's FOR-123 precedent).
function renderProgress() {
  return render(
    <MemoryRouter>
      <NotificationProvider>
        <ProgressPage />
      </NotificationProvider>
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
    insightsMock.mockReset();
    insightsMock.mockResolvedValue(insights);
    historyMock.mockReset();
    historyMock.mockResolvedValue([]);
    photosMock.mockReset();
    photosMock.mockResolvedValue([]);
  });

  it('renders a graph per metric from the measurements', async () => {
    listMock.mockResolvedValue(measurements(4));

    renderProgress();

    // One chart (role img) per metric: weight, body fat %, lean mass.
    expect(await screen.findByRole('img', { name: /Evolución de peso/ })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /Evolución de grasa corporal/ })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /Evolución de masa magra/ })).toBeInTheDocument();
    expect(screen.getAllByRole('img')).toHaveLength(3);

    // Each metric card title is a direct sibling of the page <h1> (no
    // intervening <h2>), so per FOR-112 it must render as <h2>.
    expect(
      screen.getByRole('heading', { name: 'Evolución de peso', level: 2 }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Evolución de grasa corporal', level: 2 }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Evolución de masa magra', level: 2 }),
    ).toBeInTheDocument();
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

    expect(await screen.findByText('Aún no hay mediciones.')).toBeInTheDocument();
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

  it('renders the FOR-56 insights section independently of the measurement charts', async () => {
    listMock.mockResolvedValue([]);

    renderProgress();

    expect(
      await screen.findByRole('heading', { name: 'Recomendaciones de la semana' }),
    ).toBeInTheDocument();
    expect(
      screen.getByText('Aún no hay suficientes datos para una recomendación.'),
    ).toBeInTheDocument();
  });

  it('renders the FOR-144 progress-photos section independently of the measurement charts', async () => {
    listMock.mockResolvedValue([]);
    photosMock.mockResolvedValue([]);

    renderProgress();

    expect(await screen.findByRole('heading', { name: 'Fotos de progreso' })).toBeInTheDocument();
    expect(screen.getByText('Aún no tienes fotos de progreso.')).toBeInTheDocument();
  });

  it('has no accessibility violations with three rendered charts (FOR-114)', async () => {
    listMock.mockResolvedValue(measurements(4));

    const { container } = renderProgress();
    await screen.findByRole('img', { name: /Evolución de peso/ });

    expect(await axe(container)).toHaveNoViolations();
  });
});
