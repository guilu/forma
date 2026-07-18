import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TrainingPage } from './TrainingPage';
import { NotificationProvider } from '../components/NotificationProvider';
import {
  getMuscleMap,
  getTrainingWeek,
  updateSessionStatus,
  type TrainingWeek,
} from '../api/training';
import { getStreak, getWeeklyHistory } from '../api/progress';

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
  getMuscleMap: vi.fn(),
}));

// FOR-143: streak + weekly-history widgets fetch independently of the week
// (mirrors ProgressPage's InsightsSection/InsightsHistorySection pattern) —
// mocked here so the many pre-existing week-focused tests below aren't
// coupled to this data; dedicated behavior is covered by the tests at the
// bottom of this file.
vi.mock('../api/progress', () => ({
  getStreak: vi.fn(),
  getWeeklyHistory: vi.fn(),
}));

const getWeekMock = vi.mocked(getTrainingWeek);
const updateMock = vi.mocked(updateSessionStatus);
const getMuscleMapMock = vi.mocked(getMuscleMap);
const getStreakMock = vi.mocked(getStreak);
const getWeeklyHistoryMock = vi.mocked(getWeeklyHistory);

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
    getMuscleMapMock.mockReset();
    // Default: no muscles (matches a non-strength/no-data response) so tests
    // that open a strength detail without asserting on the muscle map don't
    // hang on an unresolved promise.
    getMuscleMapMock.mockResolvedValue({ sessionId: '', muscles: [] });
    getStreakMock.mockReset();
    getWeeklyHistoryMock.mockReset();
    // Defaults distinct from every other assertion in this file (day names,
    // "N/M" tallies, muscle labels) so pre-existing tests never accidentally
    // match this widget's text.
    getStreakMock.mockResolvedValue({
      currentStreakDays: 4,
      longestStreakDays: 12,
      asOf: '2026-07-06',
    });
    getWeeklyHistoryMock.mockResolvedValue({
      weeks: [
        { weekStart: '2026-06-22', planned: 7, completed: 5 },
        { weekStart: '2026-06-29', planned: 7, completed: 7 },
      ],
    });
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

  it('loads and renders the FOR-136 muscle map for a strength session, grouped and normalized', async () => {
    getWeekMock.mockResolvedValue(week);
    getMuscleMapMock.mockResolvedValue({
      sessionId: 'MONDAY:STRENGTH',
      muscles: [
        { muscle: 'pecho', load: 'HIGH' },
        { muscle: 'hombro', load: 'MEDIUM' },
        { muscle: 'hombro anterior', load: 'HIGH' },
      ],
    });
    const user = userEvent.setup();

    renderPage();
    await screen.findByRole('heading', { name: 'Calendario semanal' });

    await user.click(screen.getByRole('button', { name: 'Ver detalle' }));

    expect(getMuscleMapMock).toHaveBeenCalledWith('MONDAY:STRENGTH');
    const dialog = await screen.findByRole('dialog', { name: /Lunes · Fuerza/ });
    expect(within(dialog).getByText('Pecho')).toBeInTheDocument();
    // "hombro" + "hombro anterior" merge into one "Hombro" group, keeping the
    // higher (HIGH) load (FOR-53 spec: frontend-owned normalization).
    expect(within(dialog).getAllByText('Hombro')).toHaveLength(1);
    expect(within(dialog).getAllByText('Carga alta')).toHaveLength(2); // Pecho + merged Hombro
  });

  it('shows a calm error and preserves the rest of the detail when the muscle map fails to load', async () => {
    getWeekMock.mockResolvedValue(week);
    getMuscleMapMock.mockRejectedValue(new Error('network'));
    const user = userEvent.setup();

    renderPage();
    await screen.findByRole('heading', { name: 'Calendario semanal' });

    await user.click(screen.getByRole('button', { name: 'Ver detalle' }));

    const dialog = await screen.findByRole('dialog', { name: /Lunes · Fuerza/ });
    expect(
      await within(dialog).findByText(/no se pudieron cargar los músculos trabajados/i),
    ).toBeInTheDocument();
    // The rest of the detail (exercise-breakdown gap notice) still renders.
    expect(within(dialog).getByText(/no está disponible todavía/)).toBeInTheDocument();
  });

  it('does not fetch a muscle map for a running session (FOR-136 is strength-only)', async () => {
    getWeekMock.mockResolvedValue(week);
    const user = userEvent.setup();

    renderPage();
    await screen.findByText('Tirada larga');

    await user.click(screen.getByRole('button', { name: /Carrera.*Tirada larga/s }));

    await screen.findByRole('dialog', { name: /Martes · Carrera/ });
    expect(getMuscleMapMock).not.toHaveBeenCalled();
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

  // FOR-143: streak + weekly-history widgets, consuming the FOR-139 endpoints
  // to replace the "RACHA ACTUAL"/weekly-history gap this page's doc comment
  // documented (mockup docs/3-entrenamiento.png). Each fetches independently
  // of the training week and of each other (FOR-60 pattern).
  describe('streak widget (FOR-143)', () => {
    it('shows the current and longest streak once loaded', async () => {
      getWeekMock.mockResolvedValue(week);
      getStreakMock.mockResolvedValue({
        currentStreakDays: 4,
        longestStreakDays: 12,
        asOf: '2026-07-06',
      });

      renderPage();

      const heading = await screen.findByRole('heading', { name: 'Racha actual', level: 2 });
      const card = heading.closest('section') as HTMLElement;
      expect(await within(card).findByText('4')).toBeInTheDocument();
      expect(within(card).getByText(/Récord: 12 días/)).toBeInTheDocument();
    });

    it('shows a zero streak as a calm normal state, not an error', async () => {
      getWeekMock.mockResolvedValue(week);
      getStreakMock.mockResolvedValue({
        currentStreakDays: 0,
        longestStreakDays: 0,
        asOf: '2026-07-06',
      });

      renderPage();

      const heading = await screen.findByRole('heading', { name: 'Racha actual' });
      const card = heading.closest('section') as HTMLElement;
      expect(await within(card).findByText('0')).toBeInTheDocument();
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('shows a loading state while the streak request resolves', async () => {
      getWeekMock.mockResolvedValue(week);
      getStreakMock.mockReturnValue(new Promise(() => {}));

      renderPage();

      // Wait for the training week itself to resolve first, so the streak
      // card has actually mounted and started its own (never-resolving)
      // fetch — otherwise this could pass merely because the outer page is
      // still on its own "Cargando tu semana…" loading state.
      await screen.findByRole('heading', { name: 'Calendario semanal' });
      expect(screen.getByText('Cargando racha…')).toBeInTheDocument();
    });

    it('shows an error scoped to the streak card and recovers on retry', async () => {
      getWeekMock.mockResolvedValue(week);
      getStreakMock.mockRejectedValueOnce(new Error('network'));
      const user = userEvent.setup();

      renderPage();

      const heading = await screen.findByRole('heading', { name: 'Racha actual' });
      const card = heading.closest('section') as HTMLElement;
      expect(await within(card).findByRole('alert')).toHaveTextContent(
        'No se pudo cargar tu racha',
      );
      // The weekly summary (a sibling widget) still rendered normally.
      expect(screen.getByRole('heading', { name: 'Resumen semanal' })).toBeInTheDocument();

      getStreakMock.mockResolvedValue({
        currentStreakDays: 4,
        longestStreakDays: 12,
        asOf: '2026-07-06',
      });
      await user.click(within(card).getByRole('button', { name: 'Reintentar' }));

      expect(await within(card).findByText('4')).toBeInTheDocument();
    });
  });

  describe('weekly-history widget (FOR-143)', () => {
    it('renders one bar per week with its completed/planned days', async () => {
      getWeekMock.mockResolvedValue(week);
      getWeeklyHistoryMock.mockResolvedValue({
        weeks: [
          { weekStart: '2026-06-22', planned: 7, completed: 5 },
          { weekStart: '2026-06-29', planned: 7, completed: 7 },
        ],
      });

      renderPage();

      const heading = await screen.findByRole('heading', { name: 'Historial semanal', level: 2 });
      const card = heading.closest('section') as HTMLElement;
      expect(await within(card).findByText('5 de 7 días')).toBeInTheDocument();
      expect(within(card).getAllByRole('listitem')).toHaveLength(2);
      expect(within(card).getByText('7 de 7 días')).toBeInTheDocument();
    });

    it('renders zero-valued weeks as visible bars, not an empty/error state', async () => {
      getWeekMock.mockResolvedValue(week);
      getWeeklyHistoryMock.mockResolvedValue({
        weeks: [{ weekStart: '2026-07-06', planned: 7, completed: 0 }],
      });

      renderPage();

      const heading = await screen.findByRole('heading', { name: 'Historial semanal' });
      const card = heading.closest('section') as HTMLElement;
      expect(await within(card).findByText('0 de 7 días')).toBeInTheDocument();
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('shows an empty message if the series itself is empty', async () => {
      getWeekMock.mockResolvedValue(week);
      getWeeklyHistoryMock.mockResolvedValue({ weeks: [] });

      renderPage();

      const heading = await screen.findByRole('heading', { name: 'Historial semanal' });
      const card = heading.closest('section') as HTMLElement;
      expect(await within(card).findByText(/Todavía no hay historial semanal/)).toBeInTheDocument();
    });

    it('shows a loading state while the weekly-history request resolves', async () => {
      getWeekMock.mockResolvedValue(week);
      getWeeklyHistoryMock.mockReturnValue(new Promise(() => {}));

      renderPage();

      // Same rationale as the streak loading test above: wait for the
      // training week to resolve so the card has actually mounted.
      await screen.findByRole('heading', { name: 'Calendario semanal' });
      expect(screen.getByText('Cargando historial semanal…')).toBeInTheDocument();
    });

    it('shows an error scoped to the weekly-history card and recovers on retry', async () => {
      getWeekMock.mockResolvedValue(week);
      getWeeklyHistoryMock.mockRejectedValueOnce(new Error('network'));
      const user = userEvent.setup();

      renderPage();

      const heading = await screen.findByRole('heading', { name: 'Historial semanal' });
      const card = heading.closest('section') as HTMLElement;
      expect(await within(card).findByRole('alert')).toHaveTextContent(
        'No se pudo cargar el historial semanal',
      );

      getWeeklyHistoryMock.mockResolvedValue({
        weeks: [{ weekStart: '2026-06-29', planned: 7, completed: 7 }],
      });
      await user.click(within(card).getByRole('button', { name: 'Reintentar' }));

      expect(await within(card).findByText('7 de 7 días')).toBeInTheDocument();
    });
  });
});
