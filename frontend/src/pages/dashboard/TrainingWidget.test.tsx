import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { TrainingWidget } from './TrainingWidget';
import { getTrainingWeek, type TrainingWeek } from '../../api/training';

vi.mock('../../api/training', () => ({ getTrainingWeek: vi.fn() }));

const trainingMock = vi.mocked(getTrainingWeek);

function renderWidget() {
  return render(
    <MemoryRouter>
      <TrainingWidget />
    </MemoryRouter>,
  );
}

const week: TrainingWeek = {
  days: [
    {
      dayOfWeek: 'MONDAY',
      rest: false,
      sessions: [
        {
          id: 'MONDAY:RUNNING',
          kind: 'RUNNING',
          title: 'Running - Intervalos',
          detail: '5 km',
          status: 'COMPLETED',
        },
      ],
    },
    {
      dayOfWeek: 'TUESDAY',
      rest: false,
      sessions: [
        {
          id: 'TUESDAY:STRENGTH',
          kind: 'STRENGTH',
          title: 'Fuerza - Tren superior',
          detail: '45 min',
          status: 'PLANNED',
        },
      ],
    },
  ],
};

describe('TrainingWidget', () => {
  beforeEach(() => {
    trainingMock.mockReset();
  });

  it('shows a loading state while the request resolves', () => {
    trainingMock.mockReturnValue(new Promise(() => {}));

    renderWidget();

    expect(screen.getByRole('status')).toHaveTextContent('Cargando tu semana de entrenamiento');
  });

  it('shows the next planned session and the completed/total tally', async () => {
    trainingMock.mockResolvedValue(week);

    renderWidget();

    expect(await screen.findByText('Fuerza - Tren superior')).toBeInTheDocument();
    expect(screen.getByText('45 min')).toBeInTheDocument();
    expect(screen.getByText('1 de 2 sesiones completadas')).toBeInTheDocument();
  });

  it('shows an empty state when there are no sessions this week', async () => {
    trainingMock.mockResolvedValue({ days: [{ dayOfWeek: 'MONDAY', rest: true, sessions: [] }] });

    renderWidget();

    expect(await screen.findByRole('status')).toHaveTextContent(
      'No hay entrenamientos planificados esta semana',
    );
  });

  it('shows an error state when the request fails', async () => {
    trainingMock.mockRejectedValue(new Error('network'));

    renderWidget();

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudo cargar tu entrenamiento',
    );
  });

  it('links to the training feature page', async () => {
    trainingMock.mockResolvedValue(week);

    renderWidget();

    expect(await screen.findByRole('link', { name: 'Ver más' })).toHaveAttribute(
      'href',
      '/entrenamiento',
    );
  });
});
