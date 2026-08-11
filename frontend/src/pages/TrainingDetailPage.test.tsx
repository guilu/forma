import { render, screen, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getMuscleMap, getTrainingWeek, getWorkout } from '../api/training';
import { TrainingDetailPage } from './TrainingDetailPage';

vi.mock('../api/training', () => ({
  getTrainingWeek: vi.fn(),
  getWorkout: vi.fn(),
  getMuscleMap: vi.fn(),
  updateSessionStatus: vi.fn(),
}));

const weekMock = vi.mocked(getTrainingWeek);
const workoutMock = vi.mocked(getWorkout);
const muscleMapMock = vi.mocked(getMuscleMap);

function renderPage() {
  render(
    <MemoryRouter initialEntries={['/app/training/SUNDAY%3ASTRENGTH']}>
      <Routes>
        <Route path="/app/training/:sessionId" element={<TrainingDetailPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('TrainingDetailPage', () => {
  beforeEach(() => {
    weekMock.mockReset();
    workoutMock.mockReset();
    muscleMapMock.mockReset();
    weekMock.mockResolvedValue({
      days: [
        {
          dayOfWeek: 'SUNDAY',
          rest: false,
          sessions: [
            {
              id: 'SUNDAY:STRENGTH',
              kind: 'STRENGTH',
              title: 'Fuerza · Pierna y core',
              detail: '5 ejercicios',
              status: 'COMPLETED',
              workoutType: 'LEGS',
            },
          ],
        },
      ],
    });
    workoutMock.mockResolvedValue({
      workoutType: 'LEGS',
      items: [
        {
          exerciseId: 'goblet-squat',
          exerciseName: 'Sentadilla goblet',
          order: 1,
          sets: 4,
          repScheme: 'RANGE',
          repsMin: 10,
          repsMax: 15,
          restSeconds: 90,
          rir: 2,
        },
        {
          exerciseId: 'dead-bug',
          exerciseName: 'Dead bug',
          order: 2,
          sets: 3,
          repScheme: 'RANGE',
          repsMin: 10,
          repsMax: 15,
          restSeconds: 45,
          rir: 2,
        },
      ],
    });
    muscleMapMock.mockResolvedValue({
      sessionId: 'SUNDAY:STRENGTH',
      muscles: [
        { muscle: 'cuádriceps', load: 'HIGH' },
        { muscle: 'glúteos', load: 'MEDIUM' },
        { muscle: 'core', load: 'LOW' },
      ],
    });
  });

  it('renders the selected real workout prescription in a dedicated main screen', async () => {
    renderPage();

    expect(
      await screen.findByRole('heading', { name: 'Pierna y core', level: 1 }),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Volver a Entrenamiento/i })).toHaveAttribute(
      'href',
      '/app/training',
    );
    expect(workoutMock).toHaveBeenCalledWith('LEGS');
    expect(muscleMapMock).toHaveBeenCalledWith('SUNDAY:STRENGTH');

    const exercises = screen.getByRole('region', { name: 'Ejercicios del entrenamiento' });
    expect(
      within(exercises).getByRole('heading', { name: 'Sentadilla goblet' }),
    ).toBeInTheDocument();
    expect(within(exercises).getByRole('heading', { name: 'Dead bug' })).toBeInTheDocument();
    expect(within(exercises).getAllByText('10–15')).toHaveLength(7);
    expect(within(exercises).getByText('Descanso: 90 s')).toBeInTheDocument();
    expect(screen.getByText('100%')).toBeInTheDocument();
    expect(screen.getByText('Cuádriceps')).toBeInTheDocument();
  });
});
