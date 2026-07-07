import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TrainingPage } from './TrainingPage';
import { getTrainingWeek, updateSessionStatus, type TrainingWeek } from '../api/training';

vi.mock('../api/training', () => ({
  getTrainingWeek: vi.fn(),
  updateSessionStatus: vi.fn(),
}));

const getWeekMock = vi.mocked(getTrainingWeek);
const updateMock = vi.mocked(updateSessionStatus);

const week: TrainingWeek = {
  days: [
    {
      dayOfWeek: 'MONDAY',
      rest: false,
      sessions: [
        {
          id: 'MONDAY:STRENGTH',
          kind: 'STRENGTH',
          title: 'Fuerza · Empuje',
          detail: '3 ejercicios',
          status: 'PLANNED',
        },
      ],
    },
    {
      dayOfWeek: 'SATURDAY',
      rest: false,
      sessions: [
        {
          id: 'SATURDAY:RUNNING',
          kind: 'RUNNING',
          title: 'Tirada larga',
          detail: '4.0 km',
          status: 'PLANNED',
        },
      ],
    },
    { dayOfWeek: 'SUNDAY', rest: true, sessions: [] },
  ],
};

describe('TrainingPage', () => {
  beforeEach(() => {
    getWeekMock.mockReset();
    updateMock.mockReset();
  });

  it('renders running, strength and rest days with their status', async () => {
    getWeekMock.mockResolvedValue(week);

    render(<TrainingPage />);

    expect(await screen.findByRole('heading', { name: 'Lunes' })).toBeInTheDocument();
    expect(screen.getByText('Tirada larga')).toBeInTheDocument();
    expect(screen.getByText('Descanso')).toBeInTheDocument();
    expect(screen.getAllByText('Planificado').length).toBe(2);
  });

  it('marks a session completed and reflects the new status', async () => {
    // Initial load PLANNED; after marking, the refetch returns COMPLETED.
    const completed: TrainingWeek = {
      days: week.days.map((day) =>
        day.dayOfWeek === 'SATURDAY'
          ? { ...day, sessions: [{ ...day.sessions[0], status: 'COMPLETED' as const }] }
          : day,
      ),
    };
    getWeekMock.mockResolvedValueOnce(week).mockResolvedValueOnce(completed);
    updateMock.mockResolvedValue({ id: 'SATURDAY:RUNNING', status: 'COMPLETED' });
    const user = userEvent.setup();

    render(<TrainingPage />);
    await screen.findByText('Tirada larga');

    // The Saturday session's "Completar" is the second one (Monday strength is first).
    await user.click(screen.getAllByRole('button', { name: 'Completar' })[1]);

    await waitFor(() => expect(updateMock).toHaveBeenCalledWith('SATURDAY:RUNNING', 'COMPLETED'));
    expect(await screen.findByText('Completado')).toBeInTheDocument();
  });

  it('shows an error when marking fails', async () => {
    getWeekMock.mockResolvedValue(week);
    updateMock.mockRejectedValue(new Error('network'));
    const user = userEvent.setup();

    render(<TrainingPage />);
    await screen.findByText('Tirada larga');

    await user.click(screen.getAllByRole('button', { name: 'Saltar' })[0]);

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo actualizar la sesión');
  });

  it('shows an error state when the week fails to load', async () => {
    getWeekMock.mockRejectedValue(new Error('network'));

    render(<TrainingPage />);

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar tu semana');
  });
});
