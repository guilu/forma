import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { InsightWidget } from './InsightWidget';
import { getWeeklyInsights, type WeeklyInsights } from '../../api/insights';

vi.mock('../../api/insights', () => ({ getWeeklyInsights: vi.fn() }));

const insightsMock = vi.mocked(getWeeklyInsights);

function renderWidget() {
  return render(
    <MemoryRouter>
      <InsightWidget />
    </MemoryRouter>,
  );
}

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
    severity: 'ACTION',
    message: 'El peso baja rápido; considera aumentar un poco las calorías.',
    reason: 'El peso baja 1.5 kg en 7 días, por encima del 1% semanal recomendado.',
    createdAt: '2026-07-10T08:00:00Z',
  },
  secondary: [],
  generatedAt: '2026-07-10T08:00:00Z',
};

describe('InsightWidget', () => {
  beforeEach(() => {
    insightsMock.mockReset();
  });

  it('shows a loading state while the request resolves', () => {
    insightsMock.mockReturnValue(new Promise(() => {}));

    renderWidget();

    expect(screen.getByRole('status')).toHaveTextContent('Cargando tu recomendación');
  });

  it('renders the main recommendation message, reason and severity', async () => {
    insightsMock.mockResolvedValue(insights);

    renderWidget();

    expect(
      await screen.findByText('El peso baja rápido; considera aumentar un poco las calorías.'),
    ).toBeInTheDocument();
    expect(
      screen.getByText('El peso baja 1.5 kg en 7 días, por encima del 1% semanal recomendado.'),
    ).toBeInTheDocument();
    expect(screen.getByText('Acción')).toBeInTheDocument();
  });

  it('renders an insufficient-data INFO recommendation the same way (backend always returns a main)', async () => {
    insightsMock.mockResolvedValue({
      ...insights,
      main: {
        category: 'BODY',
        severity: 'INFO',
        message: 'Aún no hay suficientes datos para una recomendación.',
        reason: 'Necesitamos al menos una medición y una semana de entrenamiento.',
        createdAt: '2026-07-10T08:00:00Z',
      },
    });

    renderWidget();

    expect(
      await screen.findByText('Aún no hay suficientes datos para una recomendación.'),
    ).toBeInTheDocument();
    expect(screen.getByText('Info')).toBeInTheDocument();
  });

  it('shows an error state when the request fails', async () => {
    insightsMock.mockRejectedValue(new Error('network'));

    renderWidget();

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudo cargar tu recomendación',
    );
  });
});
