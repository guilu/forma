import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { TrainingWidget } from './TrainingWidget';
import { NotificationProvider } from '../../components/NotificationProvider';
import { getMuscleMap, getTrainingWeek, getWorkout, type TrainingWeek } from '../../api/training';

/* The card draws the session on the body it works and opens the same detail
   dialog the training page opens, so the widget now reaches three endpoints. */
vi.mock('../../api/training', () => ({
  getTrainingWeek: vi.fn(),
  getMuscleMap: vi.fn(),
  getWorkout: vi.fn(),
  updateSessionStatus: vi.fn(),
  rescheduleSession: vi.fn(),
}));

const trainingMock = vi.mocked(getTrainingWeek);
const muscleMock = vi.mocked(getMuscleMap);
const workoutMock = vi.mocked(getWorkout);

function renderWidget() {
  /* Mirrors App.tsx: the detail dialog's actions report through the shared
     notification region. */
  return render(
    <MemoryRouter>
      <NotificationProvider>
        <TrainingWidget date={new Date('2026-08-04T12:00:00Z')} />
      </NotificationProvider>
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
          bodyView: 'FRONT',
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
          bodyView: 'FRONT',
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
    muscleMock.mockReset();
    muscleMock.mockResolvedValue({
      sessionId: 'TUESDAY:STRENGTH',
      muscles: [
        { muscle: 'pecho', load: 'HIGH' },
        { muscle: 'tríceps', load: 'MEDIUM' },
      ],
    });
    workoutMock.mockReset();
    workoutMock.mockResolvedValue({ workoutType: 'PUSH', items: [] });
  });

  it('shows a loading state while the request resolves', () => {
    trainingMock.mockReturnValue(new Promise(() => {}));

    renderWidget();

    expect(screen.getByRole('status')).toHaveTextContent('Cargando tu semana de entrenamiento');
  });

  it("shows today's session and the completed/total tally", async () => {
    trainingMock.mockResolvedValue(week);

    renderWidget();

    expect(await screen.findByText('Fuerza - Tren superior')).toBeInTheDocument();
    expect(screen.getByText(/45 min/)).toBeInTheDocument();
    expect(screen.getByText('1 de 2 sesiones completadas')).toBeInTheDocument();
  });

  it('draws both sheets with the muscles the session works', async () => {
    trainingMock.mockResolvedValue(week);

    renderWidget();

    // One sheet per view: a push day's triceps live on the back, so a single
    // body would silently drop half of what the session works.
    const figures = await screen.findByRole('img', {
      name: /Músculos trabajados: Pecho, Tríceps/,
    });
    const sheets = figures.querySelectorAll('[data-silhouette]');
    expect([...sheets].map((sheet) => sheet.getAttribute('data-silhouette'))).toEqual([
      'male/front',
      'male/back',
    ]);
    // The muscles are lit, not just listed: one mask per worked muscle the
    // sheet can draw.
    expect(figures.querySelectorAll('[data-muscle]').length).toBeGreaterThan(0);
  });

  it('opens the session detail dialog from anywhere on the card', async () => {
    trainingMock.mockResolvedValue(week);
    const user = userEvent.setup();

    renderWidget();

    await user.click(
      await screen.findByRole('button', { name: 'Ver el detalle de Fuerza - Tren superior' }),
    );

    const dialog = await screen.findByRole('dialog');
    expect(dialog).toHaveAccessibleName('Martes · Fuerza');
    expect(within(dialog).getByRole('button', { name: 'Completar' })).toBeInTheDocument();
  });

  it('never offers to open a rest day, since there is no session behind it', async () => {
    trainingMock.mockResolvedValue({
      days: [
        { dayOfWeek: 'MONDAY', rest: false, sessions: week.days[0].sessions },
        { dayOfWeek: 'TUESDAY', rest: true, sessions: [] },
      ],
    });

    renderWidget();

    expect(await screen.findByText('Hoy es día de descanso.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Ver el detalle/ })).not.toBeInTheDocument();
  });

  it('shows an empty state when there are no sessions this week', async () => {
    trainingMock.mockResolvedValue({ days: [{ dayOfWeek: 'MONDAY', rest: true, sessions: [] }] });

    renderWidget();

    // Loading and empty are both announced via role="status" (FOR-60 shared
    // states), so wait for the terminal content instead of the first match.
    await waitFor(() => {
      expect(screen.getByRole('status')).toHaveTextContent(
        'No hay entrenamientos planificados esta semana',
      );
    });
  });

  it('shows an error state when the request fails', async () => {
    trainingMock.mockRejectedValue(new Error('network'));

    renderWidget();

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudo cargar tu entrenamiento',
    );
  });

  /* The CTA that used to sit inside the card is gone: a button inside a
     clickable card is a control inside a control. The route lives in the
     section header instead. */
  it('links to the training feature page from the section header', async () => {
    trainingMock.mockResolvedValue(week);

    renderWidget();

    expect(await screen.findByRole('link', { name: 'Ver plan' })).toHaveAttribute(
      'href',
      '/app/training',
    );
    expect(screen.queryByRole('link', { name: 'Ver plan completo' })).not.toBeInTheDocument();
  });
});
