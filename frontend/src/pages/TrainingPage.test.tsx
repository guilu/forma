import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TrainingPage } from './TrainingPage';
import { NotificationProvider } from '../components/NotificationProvider';
import { getTrainingWeek, updateSessionStatus, type TrainingWeek } from '../api/training';

/** TrainingPage calls `useNotify()` (FOR-63), which requires a provider. */
function renderPage() {
  return render(
    <NotificationProvider>
      <TrainingPage />
    </NotificationProvider>,
  );
}

vi.mock('../api/training', () => ({
  getTrainingWeek: vi.fn(),
  updateSessionStatus: vi.fn(),
}));

const getWeekMock = vi.mocked(getTrainingWeek);
const updateMock = vi.mocked(updateSessionStatus);

// Fixed "today" = Monday 2026-07-06, so the MONDAY entry below is always
// picked up by the today's-session card regardless of when the suite runs.
const TODAY = new Date('2026-07-06T09:00:00');

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
      dayOfWeek: 'TUESDAY',
      rest: false,
      sessions: [
        {
          id: 'TUESDAY:RUNNING',
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
    // Only Date is mocked — setTimeout/setInterval stay real so RTL's
    // findBy/waitFor polling keeps working without manually advancing timers.
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(TODAY);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('shows a loading state while the request resolves', () => {
    getWeekMock.mockReturnValue(new Promise(() => {}));

    renderPage();

    expect(screen.getByRole('status')).toHaveTextContent('Cargando tu semana');
  });

  it("renders today's session card for the current day of week", async () => {
    getWeekMock.mockResolvedValue(week);

    renderPage();

    // Direct sibling of the page <h1> (no intervening <h2>), so per FOR-112
    // it must render as <h2>.
    const todayHeading = await screen.findByRole('heading', {
      name: 'Entrenamiento de hoy',
      level: 2,
    });
    const todayCard = todayHeading.closest('section') as HTMLElement;

    expect(within(todayCard).getByText('Fuerza · Empuje')).toBeInTheDocument();
    expect(within(todayCard).getByText('3 ejercicios')).toBeInTheDocument();
    expect(
      within(todayCard).getByRole('button', { name: 'Iniciar entrenamiento' }),
    ).toBeInTheDocument();
  });

  it('renders the weekly calendar with running, strength and rest days', async () => {
    getWeekMock.mockResolvedValue(week);

    renderPage();
    // Direct sibling of the page <h1>, so it must render as <h2> (FOR-112).
    await screen.findByRole('heading', { name: 'Calendario semanal', level: 2 });

    expect(screen.getByText('Tirada larga')).toBeInTheDocument();
    // Each day title nested inside the calendar card stays an <h3> — one
    // level below its now-<h2> "Calendario semanal" container.
    expect(screen.getByRole('heading', { name: 'Lunes', level: 3 })).toBeInTheDocument();
    // Sunday is a rest day: shown, with no session controls for it.
    const sundayHeading = screen.getByText('Domingo');
    const sundayDay = sundayHeading.closest('li');
    expect(sundayDay).not.toBeNull();
    expect(sundayDay).toHaveTextContent('Descanso');
    expect(sundayDay?.querySelector('button')).toBeNull();
  });

  it('opens the session detail for a strength session', async () => {
    getWeekMock.mockResolvedValue(week);
    const user = userEvent.setup();

    renderPage();
    await screen.findByRole('heading', { name: 'Calendario semanal' });

    await user.click(screen.getByRole('button', { name: 'Ver detalle' }));

    expect(screen.getByRole('dialog', { name: /Lunes · Fuerza/ })).toBeInTheDocument();
    // Documented gap: no exercise-level breakdown is available from the API.
    expect(screen.getByText(/no está disponible todavía/)).toBeInTheDocument();
  });

  it('opens the session detail for a running session', async () => {
    getWeekMock.mockResolvedValue(week);
    const user = userEvent.setup();

    renderPage();
    await screen.findByText('Tirada larga');

    await user.click(screen.getByRole('button', { name: /Carrera.*Tirada larga/s }));

    expect(screen.getByRole('dialog', { name: /Martes · Carrera/ })).toBeInTheDocument();
  });

  it('marks a session completed and reflects the new status in the calendar and summary', async () => {
    const completed: TrainingWeek = {
      days: week.days.map((day) =>
        day.dayOfWeek === 'TUESDAY'
          ? { ...day, sessions: [{ ...day.sessions[0], status: 'COMPLETED' as const }] }
          : day,
      ),
    };
    getWeekMock.mockResolvedValueOnce(week).mockResolvedValueOnce(completed);
    updateMock.mockResolvedValue({ id: 'TUESDAY:RUNNING', status: 'COMPLETED' });
    const user = userEvent.setup();

    renderPage();
    await screen.findByText('Tirada larga');

    const runningTile = screen.getByRole('heading', { name: 'Carrera' }).closest('section');
    expect(runningTile).not.toBeNull();
    expect(within(runningTile as HTMLElement).getByText('0/1')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Carrera.*Tirada larga/s }));
    await user.click(screen.getByRole('button', { name: 'Completar' }));

    await waitFor(() => expect(updateMock).toHaveBeenCalledWith('TUESDAY:RUNNING', 'COMPLETED'));
    await waitFor(() =>
      expect(within(runningTile as HTMLElement).getByText('1/1')).toBeInTheDocument(),
    );
  });

  it('shows a success notification after marking a session completed (FOR-63)', async () => {
    getWeekMock.mockResolvedValue(week);
    updateMock.mockResolvedValue({ id: 'TUESDAY:RUNNING', status: 'COMPLETED' });
    const user = userEvent.setup();

    renderPage();
    await screen.findByRole('heading', { name: 'Calendario semanal' });

    await user.click(screen.getByRole('button', { name: /Carrera.*Tirada larga/s }));
    await user.click(screen.getByRole('button', { name: 'Completar' }));

    const region = screen.getByRole('log');
    expect(await within(region).findByText(/marcado como completado/i)).toBeInTheDocument();
  });

  it('shows the weekly summary with planned vs completed counts', async () => {
    getWeekMock.mockResolvedValue(week);

    renderPage();
    await screen.findByRole('heading', { name: 'Calendario semanal' });

    // Direct sibling of the page <h1>, so it must render as <h2> (FOR-112).
    expect(screen.getByRole('heading', { name: 'Resumen semanal', level: 2 })).toBeInTheDocument();
    expect(screen.getByText('0/2')).toBeInTheDocument(); // Sesiones totales
    expect(screen.getAllByText('0/1')).toHaveLength(2); // Carrera + Fuerza tiles
    // The MetricCards nested inside "Resumen semanal" stay at the default
    // <h3> — one level below their now-<h2> container, not re-audited.
    expect(screen.getByRole('heading', { name: 'Sesiones totales', level: 3 })).toBeInTheDocument();
  });

  it('shows an error when marking fails and preserves the prior status', async () => {
    getWeekMock.mockResolvedValue(week);
    updateMock.mockRejectedValue(new Error('network'));
    const user = userEvent.setup();

    renderPage();
    await screen.findByRole('heading', { name: 'Calendario semanal' });

    await user.click(screen.getByRole('button', { name: 'Iniciar entrenamiento' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo actualizar la sesión');
    // Status stayed PLANNED — "Iniciar entrenamiento" is still offered.
    expect(screen.getByRole('button', { name: 'Iniciar entrenamiento' })).toBeInTheDocument();
  });

  it('shows an error state with retry when the week fails to load', async () => {
    getWeekMock.mockRejectedValue(new Error('network'));

    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar tu semana');
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });

  it('shows an empty state when the week has no sessions', async () => {
    getWeekMock.mockResolvedValue({ days: [{ dayOfWeek: 'MONDAY', rest: true, sessions: [] }] });

    renderPage();

    // Loading and empty are both announced via role="status" (FOR-60 shared
    // states), so wait for the terminal content instead of the first status
    // match to avoid a race against the still-in-flight loading state.
    await waitFor(() => {
      expect(screen.getByRole('status')).toHaveTextContent(
        'No hay entrenamientos planificados esta semana',
      );
    });
  });

  it('renders a rest day today with no session actions', async () => {
    vi.setSystemTime(new Date('2026-07-12T09:00:00')); // Sunday
    getWeekMock.mockResolvedValue(week);

    renderPage();

    expect(await screen.findByText('Hoy es día de descanso.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Iniciar entrenamiento' })).toBeNull();
  });
});
