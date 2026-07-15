import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, within } from '@testing-library/react';
import { InsightsHistorySection } from './InsightsHistorySection';
import { getInsightsHistory, type WeeklyInsights } from '../../api/insights';

vi.mock('../../api/insights', () => ({ getInsightsHistory: vi.fn() }));

const historyMock = vi.mocked(getInsightsHistory);

function renderSection() {
  return render(<InsightsHistorySection />);
}

function entry(weekStartDate: string, message: string): WeeklyInsights {
  return {
    checkIn: {
      weekStartDate,
      latestWeightKg: 70,
      plannedRunningSessions: 3,
      completedRunningSessions: 3,
      plannedStrengthSessions: 3,
      completedStrengthSessions: 2,
    },
    main: {
      category: 'BODY',
      severity: 'INFO',
      message,
      reason: `Reason for ${weekStartDate}`,
      createdAt: `${weekStartDate}T08:00:00Z`,
    },
    secondary: [],
    generatedAt: `${weekStartDate}T08:00:00Z`,
    deltas: {},
  };
}

const twoWeeks = [
  entry('2026-07-06', 'Semana más reciente'),
  entry('2026-06-29', 'Semana anterior'),
];

describe('InsightsHistorySection', () => {
  beforeEach(() => {
    historyMock.mockReset();
  });

  it('shows a loading state while the request resolves', () => {
    historyMock.mockReturnValue(new Promise(() => {}));

    renderSection();

    expect(screen.getByRole('status')).toHaveTextContent('Cargando semanas anteriores');
  });

  it('lists past periods, most recent first, exactly as returned by the backend', async () => {
    historyMock.mockResolvedValue(twoWeeks);

    renderSection();

    const items = await screen.findAllByRole('button', { name: /semana/i });
    expect(items).toHaveLength(2);
    expect(items[0]).toHaveTextContent('Semana más reciente');
    expect(items[1]).toHaveTextContent('Semana anterior');
  });

  it('renders an empty state, not an error, when there is no history yet', async () => {
    historyMock.mockResolvedValue([]);

    renderSection();

    expect(await screen.findByText(/aún no hay semanas anteriores/i)).toBeInTheDocument();
  });

  it('shows an error state with retry, independent of any other insights section', async () => {
    historyMock.mockRejectedValue(new Error('network'));

    renderSection();

    expect(await screen.findByRole('alert')).toHaveTextContent(/no se pudo cargar/i);

    historyMock.mockResolvedValue(twoWeeks);
    fireEvent.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findAllByRole('button', { name: /semana/i })).toHaveLength(2);
  });

  it('selecting a history entry renders that period full insights (main, secondary, signals)', async () => {
    historyMock.mockResolvedValue(twoWeeks);

    renderSection();

    const button = await screen.findByRole('button', { name: /semana anterior/i });
    fireEvent.click(button);

    expect(await screen.findByText('Reason for 2026-06-29')).toBeInTheDocument();
    expect(within(button).getByText('Semana anterior')).toBeInTheDocument();
  });
});
