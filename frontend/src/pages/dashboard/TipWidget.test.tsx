import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { getWeeklyInsights, type WeeklyInsights } from '../../api/insights';
import { TipWidget } from './TipWidget';

vi.mock('../../api/insights', () => ({ getWeeklyInsights: vi.fn() }));

const insightsMock = vi.mocked(getWeeklyInsights);

const insights: WeeklyInsights = {
  checkIn: {
    weekStartDate: '2026-08-24',
    plannedRunningSessions: 3,
    completedRunningSessions: 2,
    plannedStrengthSessions: 2,
    completedStrengthSessions: 1,
  },
  main: {
    category: 'TRAINING',
    severity: 'ACTION',
    message: 'Reserva dos huecos concretos para completar tu entrenamiento.',
    reason: 'Has completado 3 de las 5 sesiones planificadas esta semana.',
    createdAt: '2026-08-27T08:00:00Z',
  },
  secondary: [],
  generatedAt: '2026-08-27T08:00:00Z',
};

function renderWidget() {
  return render(
    <MemoryRouter>
      <TipWidget />
    </MemoryRouter>,
  );
}

describe('TipWidget', () => {
  beforeEach(() => {
    insightsMock.mockReset();
  });

  it('shows an honest loading state while the recommendation is requested', () => {
    insightsMock.mockReturnValue(new Promise(() => {}));

    renderWidget();

    expect(screen.getByRole('heading', { name: 'Recomendación destacada' })).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('Cargando tu recomendación');
  });

  it('renders the backend-prioritized main recommendation and its explanation', async () => {
    insightsMock.mockResolvedValue(insights);

    renderWidget();

    expect(await screen.findByText(insights.main.message)).toBeInTheDocument();
    expect(screen.getByText(insights.main.reason)).toBeInTheDocument();
    expect(screen.getByText('Acción')).toBeInTheDocument();
    expect(document.querySelector('[aria-hidden="true"]')).not.toBeInTheDocument();
  });

  it('uses the backend insufficient-data recommendation as its fallback', async () => {
    insightsMock.mockResolvedValue({
      ...insights,
      main: {
        ...insights.main,
        category: 'BODY',
        severity: 'INFO',
        message: 'Aún no hay suficientes datos para una recomendación.',
        reason: 'Necesitamos al menos una medición y una semana de entrenamiento.',
      },
    });

    renderWidget();

    expect(
      await screen.findByText('Aún no hay suficientes datos para una recomendación.'),
    ).toBeInTheDocument();
    expect(screen.getByText('Info')).toBeInTheDocument();
  });

  it('shows an error state when the recommendation cannot be loaded', async () => {
    insightsMock.mockRejectedValue(new Error('network'));

    renderWidget();

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudo cargar tu recomendación',
    );
  });
});
