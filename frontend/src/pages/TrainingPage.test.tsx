import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, useLocation } from 'react-router-dom';
import { TrainingPage } from './TrainingPage';
import { NotificationProvider } from '../components/NotificationProvider';
import {
  getMuscleMap,
  getTrainingWeek,
  rescheduleSession,
  updateSessionStatus,
  type TrainingWeek,
} from '../api/training';
import { getStreak } from '../api/progress';
import { getProfile } from '../api/profile';

/** TrainingPage calls `useNotify()` (FOR-63), which requires a provider. */
function renderPage() {
  return render(
    <MemoryRouter>
      <NotificationProvider>
        <TrainingPage />
        <LocationProbe />
      </NotificationProvider>
    </MemoryRouter>,
  );
}

function LocationProbe() {
  const location = useLocation();
  return <div data-testid="location">{location.pathname}</div>;
}

vi.mock('../api/training', () => ({
  getTrainingWeek: vi.fn(),
  updateSessionStatus: vi.fn(),
  getMuscleMap: vi.fn(),
  rescheduleSession: vi.fn(),
}));

// FOR-143: streak + weekly-history widgets fetch independently of the week
// (mirrors ProgressPage's InsightsSection/InsightsHistorySection pattern) —
// mocked here so the many pre-existing week-focused tests below aren't
// coupled to this data; dedicated behavior is covered by the tests at the
// bottom of this file.
vi.mock('../api/progress', () => ({
  getStreak: vi.fn(),
}));

vi.mock('../api/profile', () => ({ getProfile: vi.fn() }));

const getWeekMock = vi.mocked(getTrainingWeek);
const rescheduleMock = vi.mocked(rescheduleSession);
const updateMock = vi.mocked(updateSessionStatus);
const getMuscleMapMock = vi.mocked(getMuscleMap);
const getStreakMock = vi.mocked(getStreak);
const getProfileMock = vi.mocked(getProfile);

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
          bodyView: 'BACK',
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
          bodyView: 'FRONT',
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
    getProfileMock.mockReset();
    getProfileMock.mockResolvedValue({ sex: 'MALE' } as never);
    // Defaults distinct from every other assertion in this file (day names,
    // "N/M" tallies, muscle labels) so pre-existing tests never accidentally
    // match this widget's text.
    getStreakMock.mockResolvedValue({
      currentStreakDays: 4,
      longestStreakDays: 12,
      asOf: '2026-07-06',
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
    expect(within(todayCard).getByRole('button', { name: 'Entrenar' })).toBeInTheDocument();
    // Both sheets, not just the session's own `bodyView`: the muscles a session
    // works do not respect the split (a pull day hits the lats and the biceps),
    // so showing one side would hide half of what it trains.
    expect(
      within(todayCard)
        .getAllByRole('presentation', { hidden: true })
        .filter((element) => element.hasAttribute('data-silhouette'))
        .map((element) => element.getAttribute('data-silhouette')),
    ).toEqual(['male/front', 'male/back']);
  });

  it('uses the female anatomy asset when the persisted profile is female', async () => {
    getWeekMock.mockResolvedValue(week);
    getProfileMock.mockResolvedValue({ sex: 'FEMALE' } as never);

    renderPage();

    const todayCard = (
      await screen.findByRole('heading', { name: 'Entrenamiento de hoy' })
    ).closest('section') as HTMLElement;
    expect(
      within(todayCard)
        .getAllByRole('presentation', { hidden: true })
        .filter((element) => element.hasAttribute('data-silhouette'))
        .map((element) => element.getAttribute('data-silhouette')),
    ).toEqual(['female/front', 'female/back']);
  });

  it('does not render the temporary muscle-groups or weekly-history cards', async () => {
    getWeekMock.mockResolvedValue(week);

    renderPage();
    await screen.findByRole('heading', { name: 'Entrenamiento de hoy' });

    expect(
      screen.queryByRole('heading', { name: 'Grupos musculares trabajados esta semana' }),
    ).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Historial semanal' })).not.toBeInTheDocument();
  });

  it('opens the live workout without marking a planned strength session completed', async () => {
    getWeekMock.mockResolvedValue(week);
    const user = userEvent.setup();

    renderPage();
    await user.click(await screen.findByRole('button', { name: 'Entrenar' }));

    expect(screen.getByTestId('location')).toHaveTextContent('/app/training/MONDAY%3ASTRENGTH');
    expect(updateMock).not.toHaveBeenCalled();
  });

  it('shows review actions instead of mutation actions when today is completed', async () => {
    const completedWeek: TrainingWeek = {
      days: week.days.map((day) =>
        day.dayOfWeek === 'MONDAY'
          ? {
              ...day,
              sessions: day.sessions.map((session) => ({ ...session, status: 'COMPLETED' })),
            }
          : day,
      ),
    };
    getWeekMock.mockResolvedValue(completedWeek);

    renderPage();

    const todayCard = (
      await screen.findByRole('heading', { name: 'Entrenamiento de hoy' })
    ).closest('section') as HTMLElement;
    expect(within(todayCard).getByRole('button', { name: 'Entrenar' })).toBeInTheDocument();
    expect(within(todayCard).queryByRole('button', { name: 'Saltar' })).not.toBeInTheDocument();

    await userEvent.click(within(todayCard).getByRole('button', { name: 'Entrenar' }));
    expect(screen.getByTestId('location')).toHaveTextContent('/app/training/MONDAY%3ASTRENGTH');
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
    const mondayDay = screen.getByRole('heading', { name: 'Lunes', level: 3 }).closest('li');
    expect(mondayDay).not.toBeNull();
    // Strength days draw the front sheet only, even for a pull session whose
    // own bodyView is BACK: at card size a front/back pair would halve each
    // body. The detail view is where both sheets are shown.
    // Queried by attribute, not by role: the silhouette is decorative
    // (`alt=""`), which makes it a presentation node rather than an image.
    expect(
      (mondayDay as HTMLElement)
        .querySelector('[data-silhouette]')
        ?.getAttribute('data-silhouette'),
    ).toBe('male/front');
    // Sunday is a rest day: shown, with no session controls for it. Queried by
    // accessible name, not by text: the strip prints "DOM" and carries the whole
    // word on the heading, so this also pins that the short label never reaches
    // assistive tech.
    const sundayHeading = screen.getByRole('heading', { name: 'Domingo', level: 3 });
    const sundayDay = sundayHeading.closest('li');
    expect(sundayDay).not.toBeNull();
    expect(sundayDay).toHaveTextContent('Descanso');
    expect(sundayDay?.querySelector('button')).toBeNull();
    expect(screen.getByRole('img', { name: 'Progreso semanal' })).toBeInTheDocument();
  });

  it('opens the session detail for a strength session', async () => {
    getWeekMock.mockResolvedValue(week);
    const user = userEvent.setup();

    renderPage();
    await screen.findByRole('heading', { name: 'Calendario semanal' });

    await user.click(screen.getByRole('button', { name: 'Detalle' }));

    expect(screen.getByRole('dialog', { name: /Lunes · Fuerza/ })).toBeInTheDocument();
    // Documented gap: no exercise-level breakdown is available from the API.
    expect(screen.getByText(/no está disponible todavía/)).toBeInTheDocument();
  });

  it('moves a session to another day from its detail', async () => {
    getWeekMock.mockResolvedValue(week);
    rescheduleMock.mockResolvedValue({ days: [] });
    const user = userEvent.setup();

    renderPage();
    await screen.findByRole('heading', { name: 'Calendario semanal' });
    await user.click(screen.getByRole('button', { name: 'Detalle' }));

    const dialog = await screen.findByRole('dialog', { name: /Lunes · Fuerza/ });
    await user.selectOptions(within(dialog).getByLabelText('Mover a otro día'), 'WEDNESDAY');

    expect(rescheduleMock).toHaveBeenCalledWith('MONDAY:STRENGTH', 'WEDNESDAY');
    // The week is refetched so the calendar redraws on the session's new day.
    await waitFor(() => expect(getWeekMock).toHaveBeenCalledTimes(2));
  });

  it('reports a failed move without losing the session detail', async () => {
    getWeekMock.mockResolvedValue(week);
    rescheduleMock.mockRejectedValue(new Error('network'));
    const user = userEvent.setup();

    renderPage();
    await screen.findByRole('heading', { name: 'Calendario semanal' });
    await user.click(screen.getByRole('button', { name: 'Detalle' }));

    const dialog = await screen.findByRole('dialog', { name: /Lunes · Fuerza/ });
    await user.selectOptions(within(dialog).getByLabelText('Mover a otro día'), 'WEDNESDAY');

    expect(await screen.findByRole('alert')).toHaveTextContent(/No se pudo mover/i);
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

    await user.click(screen.getByRole('button', { name: 'Detalle' }));

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

    await user.click(screen.getByRole('button', { name: 'Detalle' }));

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
    // Not "never called": today's card derives the focus of its *strength*
    // session from the same endpoint. What must never happen is asking for the
    // muscle map of a run.
    expect(getMuscleMapMock).not.toHaveBeenCalledWith('TUESDAY:RUNNING');
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

    const runningTile = screen.getByRole('listitem', { name: 'Carreras' });
    expect(runningTile).not.toBeNull();
    expect(within(runningTile as HTMLElement).getByText('0 / 1')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Carrera.*Tirada larga/s }));
    await user.click(screen.getByRole('button', { name: 'Completar' }));

    await waitFor(() => expect(updateMock).toHaveBeenCalledWith('TUESDAY:RUNNING', 'COMPLETED'));
    await waitFor(() =>
      expect(within(runningTile as HTMLElement).getByText('1 / 1')).toBeInTheDocument(),
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
    const summaryHeading = screen.getByRole('heading', { name: 'Resumen semanal', level: 2 });
    const summary = summaryHeading.closest('section');
    expect(summary).not.toBeNull();
    const summaryView = within(summary as HTMLElement);
    expect(summaryView.getByText('0 / 2')).toBeInTheDocument(); // Sesiones totales
    expect(summaryView.getAllByText('0 / 1')).toHaveLength(2); // Carreras + Fuerza tiles
    expect(
      summaryView.getByRole('heading', { name: 'Sesiones totales', level: 3 }),
    ).toBeInTheDocument();
    expect(summaryView.getByText('Sesiones completadas')).toBeInTheDocument();
    expect(summaryView.getByRole('heading', { name: 'Carreras', level: 3 })).toBeInTheDocument();
    expect(summaryView.getByRole('heading', { name: 'Fuerza', level: 3 })).toBeInTheDocument();
    // The kind is already the row's own heading, so the caption only has to say
    // what the number counts.
    expect(summaryView.getAllByText('Completadas')).toHaveLength(2);
    expect(summaryView.getAllByText('0%')).toHaveLength(3);
    expect(summaryView.getByRole('link', { name: /Ver estadísticas completas/i })).toHaveAttribute(
      'href',
      '/app/progress',
    );
  });

  /*
   * The day card already carries a "Fuerza"/"Carrera" badge, so repeating the
   * kind inside the title spent two of the card's four lines saying the same
   * thing — and the titles wrapped because of it.
   */
  it('drops the kind prefix from the calendar day titles the badge already states', async () => {
    getWeekMock.mockResolvedValue(week);
    renderPage();

    const calendar = await screen.findByRole('list', {
      name: 'Calendario semanal de entrenamiento',
    });

    expect(within(calendar).getByText('Empuje')).toBeInTheDocument();
    expect(within(calendar).queryByText('Fuerza · Empuje')).not.toBeInTheDocument();
  });

  /*
   * Weekday and date are separate elements on purpose: FOR-193 cut the long
   * date form because it pushed the training header onto a second row at
   * 390px, so the weekday is the part the stylesheet can drop on a phone while
   * the compact date stays.
   */
  it('leads the header date with the weekday, as its own droppable element', async () => {
    getWeekMock.mockResolvedValue(week);
    renderPage();
    await screen.findByRole('heading', { name: 'Calendario semanal' });

    const weekday = screen.getByTestId('date-weekday');

    expect(weekday.textContent).toMatch(
      /^(Lunes|Martes|Miércoles|Jueves|Viernes|Sábado|Domingo),$/,
    );
  });

  it('keys the calendar legend to the three session statuses and nothing else', async () => {
    getWeekMock.mockResolvedValue(week);
    renderPage();
    await screen.findByRole('heading', { name: 'Calendario semanal' });

    const legend = screen.getByRole('list', { name: 'Leyenda del calendario' });

    for (const entry of ['Completado', 'Pendiente', 'Saltado']) {
      expect(within(legend).getByText(entry)).toBeInTheDocument();
    }
    // The kinds left the legend when the badges left the cards, and "Hoy" went
    // with them: a colour key for something the grid never draws in that colour
    // is noise, and the highlighted card is its own label.
    for (const gone of ['Hoy', 'Fuerza', 'Carrera', 'Descanso']) {
      expect(within(legend).queryByText(gone)).not.toBeInTheDocument();
    }
  });

  it('shows each day status in the card corner', async () => {
    getWeekMock.mockResolvedValue(week);
    renderPage();
    await screen.findByRole('heading', { name: 'Calendario semanal' });

    const monday = screen.getByRole('heading', { name: 'Lunes', level: 3 }).closest('li');
    expect(within(monday as HTMLElement).getByLabelText('Pendiente')).toBeInTheDocument();
  });

  /*
   * Today's card used to print a fixed "Duración estimada: 55 min" and
   * "Enfoque: Pecho, Hombros, Tríceps" under every session, so a *running* day
   * announced a chest-and-triceps focus. Nothing in the training API backs
   * either field, so the card now shows only what the session really carries.
   */
  describe("today's card only states what the session actually carries", () => {
    const runningToday: TrainingWeek = {
      days: week.days.map((day) =>
        day.dayOfWeek === 'MONDAY'
          ? {
              ...day,
              sessions: [
                {
                  id: 'MONDAY:RUNNING',
                  kind: 'RUNNING',
                  bodyView: 'FRONT',
                  title: 'Series',
                  detail: '4.0 km',
                  status: 'PLANNED',
                },
              ],
            }
          : day,
      ),
    };

    it('never attaches a muscle focus or an invented duration to a run', async () => {
      getWeekMock.mockResolvedValue(runningToday);

      renderPage();
      const todayCard = (
        await screen.findByRole('heading', { name: 'Entrenamiento de hoy' })
      ).closest('section') as HTMLElement;

      expect(within(todayCard).getByText('4.0 km')).toBeInTheDocument();
      expect(within(todayCard).queryByText(/Enfoque/)).not.toBeInTheDocument();
      expect(within(todayCard).queryByText(/Duración estimada/)).not.toBeInTheDocument();
      expect(within(todayCard).queryByText(/ejercicios/)).not.toBeInTheDocument();
    });

    it('derives the focus of a strength session from its real muscle map', async () => {
      getWeekMock.mockResolvedValue(week);
      getMuscleMapMock.mockResolvedValue({
        sessionId: 'MONDAY:STRENGTH',
        muscles: [
          { muscle: 'pecho', load: 'HIGH' },
          { muscle: 'tríceps', load: 'MEDIUM' },
        ],
      });

      renderPage();
      const todayCard = (
        await screen.findByRole('heading', { name: 'Entrenamiento de hoy' })
      ).closest('section') as HTMLElement;

      expect(await within(todayCard).findByText('Enfoque: Pecho, Tríceps')).toBeInTheDocument();
    });
  });

  /*
   * A run has no per-exercise screen to open, so its action marks completion
   * in place — and has to be undoable, or a mistaken tap is permanent.
   */
  describe('completing a run from the card', () => {
    const runningToday = (status: 'PLANNED' | 'COMPLETED'): TrainingWeek => ({
      days: week.days.map((day) =>
        day.dayOfWeek === 'MONDAY'
          ? {
              ...day,
              sessions: [
                {
                  id: 'MONDAY:RUNNING',
                  kind: 'RUNNING',
                  bodyView: 'FRONT',
                  title: 'Series',
                  detail: '4.0 km',
                  status,
                },
              ],
            }
          : day,
      ),
    });

    it('offers "Completar carrera" and reflects it in the weekly summary', async () => {
      getWeekMock
        .mockResolvedValueOnce(runningToday('PLANNED'))
        .mockResolvedValue(runningToday('COMPLETED'));
      updateMock.mockResolvedValue({ id: 'MONDAY:RUNNING', status: 'COMPLETED' });
      const user = userEvent.setup();

      renderPage();
      await screen.findByRole('button', { name: 'Completar carrera' });

      // Two runs in the fixture week (Monday and Tuesday), neither done yet.
      const runningTile = screen.getByRole('listitem', { name: 'Carreras' });
      expect(within(runningTile).getByText('0 / 2')).toBeInTheDocument();

      await user.click(screen.getByRole('button', { name: 'Completar carrera' }));

      expect(updateMock).toHaveBeenCalledWith('MONDAY:RUNNING', 'COMPLETED');
      await waitFor(() => expect(within(runningTile).getByText('1 / 2')).toBeInTheDocument());
    });

    it('turns the action into an undo that puts the run back to planned', async () => {
      getWeekMock
        .mockResolvedValueOnce(runningToday('COMPLETED'))
        .mockResolvedValue(runningToday('PLANNED'));
      updateMock.mockResolvedValue({ id: 'MONDAY:RUNNING', status: 'PLANNED' });
      const user = userEvent.setup();

      renderPage();
      const undo = await screen.findByRole('button', {
        name: 'Desmarcar la carrera como completada',
      });

      await user.click(undo);

      expect(updateMock).toHaveBeenCalledWith('MONDAY:RUNNING', 'PLANNED');
      expect(await screen.findByRole('button', { name: 'Completar carrera' })).toBeInTheDocument();
    });
  });

  /*
   * The arrows used to be inert decoration. They now walk the composed week,
   * which is the only range the API has: `GET /training/week` returns the
   * current week and takes no date (docs/api/training-week.md).
   */
  describe('the date arrows move the card across the week', () => {
    it('swaps the card to the chosen day and names it', async () => {
      getWeekMock.mockResolvedValue(week);
      const user = userEvent.setup();

      renderPage();
      await screen.findByRole('heading', { name: 'Entrenamiento de hoy' });

      await user.click(screen.getByRole('button', { name: 'Día siguiente' }));

      const card = (
        await screen.findByRole('heading', { name: 'Entrenamiento del martes' })
      ).closest('section') as HTMLElement;
      expect(within(card).getByText('Tirada larga')).toBeInTheDocument();
      expect(screen.getByTestId('date-weekday')).toHaveTextContent('Martes,');
    });

    it('stops at the edges of the week the API actually returns', async () => {
      getWeekMock.mockResolvedValue(week);
      const user = userEvent.setup();

      renderPage();
      await screen.findByRole('heading', { name: 'Entrenamiento de hoy' });

      // TODAY is the Monday of the fixture week, so there is nothing before it.
      expect(screen.getByRole('button', { name: 'Día anterior' })).toBeDisabled();

      for (let step = 0; step < 6; step += 1) {
        await user.click(screen.getByRole('button', { name: 'Día siguiente' }));
      }
      expect(screen.getByRole('button', { name: 'Día siguiente' })).toBeDisabled();
      expect(
        await screen.findByRole('heading', { name: 'Entrenamiento del domingo' }),
      ).toBeInTheDocument();
    });
  });

  it('shows an error when marking fails and preserves the prior status', async () => {
    getWeekMock.mockResolvedValue({
      days: week.days.map((day) =>
        day.dayOfWeek === 'MONDAY'
          ? {
              ...day,
              sessions: [
                {
                  id: 'MONDAY:RUNNING',
                  kind: 'RUNNING',
                  bodyView: 'FRONT',
                  title: 'Rodaje suave',
                  detail: '3 km',
                  status: 'PLANNED',
                },
              ],
            }
          : day,
      ),
    });
    updateMock.mockRejectedValue(new Error('network'));
    const user = userEvent.setup();

    renderPage();
    await screen.findByRole('heading', { name: 'Calendario semanal' });

    await user.click(screen.getByRole('button', { name: 'Completar carrera' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo actualizar la sesión');
    // Status stayed PLANNED — the run is still offered for completion.
    expect(screen.getByRole('button', { name: 'Completar carrera' })).toBeInTheDocument();
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
      expect(screen.getByRole('status')).toHaveTextContent('No existe ningún plan planificado.');
    });
  });

  it('renders a rest day today with no session actions', async () => {
    vi.setSystemTime(new Date('2026-07-12T09:00:00')); // Sunday
    getWeekMock.mockResolvedValue(week);

    renderPage();

    expect(await screen.findByText('Hoy es día de descanso.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Entrenar' })).toBeNull();
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
});
