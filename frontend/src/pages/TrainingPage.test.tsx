import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { TrainingPage } from './TrainingPage';
import { getTrainingWeek, type TrainingWeek } from '../api/training';

vi.mock('../api/training', () => ({
  getTrainingWeek: vi.fn(),
}));

const getWeekMock = vi.mocked(getTrainingWeek);

const week: TrainingWeek = {
  days: [
    {
      dayOfWeek: 'MONDAY',
      rest: false,
      sessions: [
        { kind: 'STRENGTH', title: 'Fuerza · Empuje', detail: '3 ejercicios', status: 'PLANNED' },
      ],
    },
    {
      dayOfWeek: 'SATURDAY',
      rest: false,
      sessions: [{ kind: 'RUNNING', title: 'Tirada larga', detail: '4.0 km', status: 'PLANNED' }],
    },
    { dayOfWeek: 'SUNDAY', rest: true, sessions: [] },
  ],
};

describe('TrainingPage', () => {
  beforeEach(() => {
    getWeekMock.mockReset();
  });

  it('renders running, strength and rest days from the week', async () => {
    getWeekMock.mockResolvedValue(week);

    render(<TrainingPage />);

    expect(await screen.findByRole('heading', { name: 'Lunes' })).toBeInTheDocument();
    expect(screen.getByText('Fuerza · Empuje')).toBeInTheDocument();
    expect(screen.getByText('Tirada larga')).toBeInTheDocument();
    expect(screen.getByText('4.0 km')).toBeInTheDocument();
    // Sunday is a rest day.
    expect(screen.getByRole('heading', { name: 'Domingo' })).toBeInTheDocument();
    expect(screen.getByText('Descanso')).toBeInTheDocument();
  });

  it('shows an empty state when the week has no sessions', async () => {
    getWeekMock.mockResolvedValue({
      days: [{ dayOfWeek: 'MONDAY', rest: true, sessions: [] }],
    });

    render(<TrainingPage />);

    expect(await screen.findByRole('status')).toHaveTextContent(
      'No hay entrenamientos planificados',
    );
  });

  it('shows an error state when the API call fails', async () => {
    getWeekMock.mockRejectedValue(new Error('network'));

    render(<TrainingPage />);

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar');
  });
});
