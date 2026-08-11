import { act, fireEvent, render, screen, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
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

  afterEach(() => {
    vi.useRealTimers();
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
    expect(within(exercises).getAllByRole('spinbutton')).toHaveLength(14);
    expect(
      within(exercises).getByRole('spinbutton', {
        name: 'Repeticiones, Sentadilla goblet, serie 1',
      }),
    ).toHaveValue(10);
    expect(within(exercises).getByText('Descanso: 90 s')).toBeInTheDocument();
    expect(screen.getByText('100%')).toBeInTheDocument();
    expect(screen.getByText('Cuádriceps')).toBeInTheDocument();
  });

  it('tracks elapsed time, editable set volume and the rest countdown', async () => {
    vi.useFakeTimers({ toFake: ['Date', 'setInterval', 'clearInterval'] });
    vi.setSystemTime(new Date('2026-08-11T08:00:00Z'));
    weekMock.mockResolvedValueOnce({
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
              status: 'PLANNED',
              workoutType: 'LEGS',
            },
          ],
        },
      ],
    });
    renderPage();

    await screen.findByRole('heading', { name: 'Pierna y core', level: 1 });
    expect(screen.getByText('00:00')).toBeInTheDocument();
    expect(screen.getByText('0 kg')).toBeInTheDocument();

    fireEvent.change(screen.getByRole('spinbutton', { name: 'Peso, Sentadilla goblet, serie 1' }), {
      target: { value: '70' },
    });
    fireEvent.change(
      screen.getByRole('spinbutton', { name: 'Repeticiones, Sentadilla goblet, serie 1' }),
      { target: { value: '10' } },
    );
    fireEvent.click(screen.getByRole('button', { name: 'Completar Sentadilla goblet, serie 1' }));

    expect(screen.getByText('700 kg')).toBeInTheDocument();
    expect(screen.getByText('90s')).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Reabrir Sentadilla goblet, serie 1' }),
    ).toBeInTheDocument();

    act(() => {
      vi.advanceTimersByTime(1000);
    });
    expect(screen.getByText('00:01')).toBeInTheDocument();
    expect(screen.getByText('89s')).toBeInTheDocument();
  });
});
