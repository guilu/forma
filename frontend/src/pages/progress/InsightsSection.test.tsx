import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { InsightsSection } from './InsightsSection';
import { getWeeklyInsights, type WeeklyInsights } from '../../api/insights';

vi.mock('../../api/insights', () => ({ getWeeklyInsights: vi.fn() }));

const insightsMock = vi.mocked(getWeeklyInsights);

function renderSection() {
  return render(<InsightsSection />);
}

const baseInsights: WeeklyInsights = {
  checkIn: {
    weekStartDate: '2026-07-06',
    latestWeightKg: 70.2,
    latestBodyFatPercentage: 18.4,
    latestLeanMassKg: 55.1,
    plannedRunningSessions: 3,
    completedRunningSessions: 3,
    plannedStrengthSessions: 3,
    completedStrengthSessions: 2,
  },
  main: {
    category: 'BODY',
    severity: 'ACTION',
    message: 'El peso baja rápido; considera aumentar un poco las calorías para frenar la pérdida.',
    reason:
      'El peso baja 1.5 kg en 7 días (~-2.1% por semana), por encima del 1% semanal recomendado.',
    relatedMetric: 'weeklyWeightChangeKg',
    createdAt: '2026-07-10T08:00:00Z',
  },
  secondary: [
    {
      category: 'TRAINING',
      severity: 'INFO',
      message: 'Semana muy constante; mantén este ritmo.',
      reason: 'Se completaron 5 de 6 sesiones planificadas.',
      createdAt: '2026-07-10T08:00:00Z',
    },
  ],
  generatedAt: '2026-07-10T08:00:00Z',
};

describe('InsightsSection', () => {
  beforeEach(() => {
    insightsMock.mockReset();
  });

  it('shows a loading state while the request resolves', () => {
    insightsMock.mockReturnValue(new Promise(() => {}));

    renderSection();

    expect(screen.getByRole('status')).toHaveTextContent('Cargando tus recomendaciones');
  });

  it('renders the main recommendation message, severity badge and reason', async () => {
    insightsMock.mockResolvedValue(baseInsights);

    renderSection();

    expect(await screen.findByText(baseInsights.main.message)).toBeInTheDocument();
    expect(screen.getByText(baseInsights.main.reason)).toBeInTheDocument();
    expect(screen.getByText('Acción')).toBeInTheDocument();
  });

  it('renders the related check-in signals alongside the recommendation', async () => {
    insightsMock.mockResolvedValue(baseInsights);

    renderSection();

    await screen.findByText(baseInsights.main.message);

    expect(screen.getByText('70.2 kg')).toBeInTheDocument();
    expect(screen.getByText('18.4 %')).toBeInTheDocument();
    expect(screen.getByText('55.1 kg')).toBeInTheDocument();
    expect(screen.getByText('3 de 3 sesiones')).toBeInTheDocument();
    expect(screen.getByText('2 de 3 sesiones')).toBeInTheDocument();
  });

  it('omits body signals that are absent from the check-in', async () => {
    insightsMock.mockResolvedValue({
      ...baseInsights,
      checkIn: {
        weekStartDate: '2026-07-06',
        plannedRunningSessions: 0,
        completedRunningSessions: 0,
        plannedStrengthSessions: 0,
        completedStrengthSessions: 0,
      },
    });

    renderSection();

    await screen.findByText(baseInsights.main.message);

    expect(screen.queryByText('Peso')).not.toBeInTheDocument();
    expect(screen.queryByText('Grasa corporal')).not.toBeInTheDocument();
    expect(screen.queryByText('Masa magra')).not.toBeInTheDocument();
    expect(screen.getAllByText('0 de 0 sesiones')).toHaveLength(2);
  });

  it('renders secondary recommendations when present', async () => {
    insightsMock.mockResolvedValue(baseInsights);

    renderSection();

    expect(await screen.findByText('Semana muy constante; mantén este ritmo.')).toBeInTheDocument();
    expect(screen.getByText('Se completaron 5 de 6 sesiones planificadas.')).toBeInTheDocument();
  });

  it('shows only the main recommendation when there are no secondary ones', async () => {
    insightsMock.mockResolvedValue({ ...baseInsights, secondary: [] });

    renderSection();

    await screen.findByText(baseInsights.main.message);

    expect(screen.queryByText('Otras recomendaciones')).not.toBeInTheDocument();
  });

  it('always shows a non-medical disclaimer', async () => {
    insightsMock.mockResolvedValue(baseInsights);

    renderSection();

    expect(
      await screen.findByText(
        /no sustituyen el diagnóstico ni el consejo de un profesional sanitario/i,
      ),
    ).toBeInTheDocument();
  });

  it('renders an insufficient-data INFO recommendation the same way (backend always returns a main)', async () => {
    insightsMock.mockResolvedValue({
      ...baseInsights,
      checkIn: {
        weekStartDate: '2026-07-06',
        plannedRunningSessions: 0,
        completedRunningSessions: 0,
        plannedStrengthSessions: 0,
        completedStrengthSessions: 0,
      },
      main: {
        category: 'BODY',
        severity: 'INFO',
        message: 'Aún no hay suficientes datos para una recomendación.',
        reason: 'Necesitamos al menos una medición y una semana de entrenamiento.',
        createdAt: '2026-07-10T08:00:00Z',
      },
      secondary: [],
    });

    renderSection();

    expect(
      await screen.findByText('Aún no hay suficientes datos para una recomendación.'),
    ).toBeInTheDocument();
    expect(screen.getByText('Info')).toBeInTheDocument();
  });

  it('shows an error state with a retry action when the request fails', async () => {
    insightsMock.mockRejectedValue(new Error('network'));

    renderSection();

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudieron cargar tus recomendaciones',
    );

    insightsMock.mockResolvedValue(baseInsights);
    fireEvent.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByText(baseInsights.main.message)).toBeInTheDocument();
    expect(insightsMock).toHaveBeenCalledTimes(2);
  });
});
