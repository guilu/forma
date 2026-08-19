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

  it("expands today's column inside the week strip", async () => {
    getWeekMock.mockResolvedValue(week);

    renderPage();

    // Direct sibling of the page <h1> (no intervening heading), so per FOR-112
    // it must render as <h2>. Named after the day it opens, not "hoy" alone:
    // the arrows move the expanded column to any day of the week.
    const todayHeading = await screen.findByRole('heading', {
      name: 'Hoy · Lunes',
      level: 2,
    });
    const todayCard = todayHeading.closest('li') as HTMLElement;

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

    const todayCard = (await screen.findByRole('heading', { name: 'Hoy · Lunes' })).closest(
      'li',
    ) as HTMLElement;
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
    await screen.findByRole('heading', { name: 'Hoy · Lunes' });

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

    const todayCard = (await screen.findByRole('heading', { name: 'Hoy · Lunes' })).closest(
      'li',
    ) as HTMLElement;
    expect(within(todayCard).getByRole('button', { name: 'Entrenar' })).toBeInTheDocument();
    expect(within(todayCard).queryByRole('button', { name: 'Saltar' })).not.toBeInTheDocument();

    await userEvent.click(within(todayCard).getByRole('button', { name: 'Entrenar' }));
    expect(screen.getByTestId('location')).toHaveTextContent('/app/training/MONDAY%3ASTRENGTH');
  });

  it('renders the week strip with running, strength and rest days', async () => {
    getWeekMock.mockResolvedValue(week);

    renderPage();
    // Direct sibling of the page <h1>, so it must render as <h2> (FOR-112).
    await screen.findByRole('heading', { name: 'Hoy · Lunes', level: 2 });

    expect(screen.getByText('Tirada larga')).toBeInTheDocument();
    // Tuesday is a compact column: today (Monday) is the one that expands.
    const tuesday = screen.getByRole('heading', { name: 'Martes', level: 2 }).closest('li');
    expect(tuesday).not.toBeNull();
    // A compact column draws one body, never the front/back pair the expanded
    // day gets: at this width a pair would halve each one and neither would
    // read. Queried by attribute, not by role: the silhouette is decorative
    // (`alt=""`), which makes it a presentation node rather than an image.
    expect((tuesday as HTMLElement).querySelectorAll('[data-silhouette]')).toHaveLength(1);
    // Sunday is a rest day: shown, with no session controls for it. Queried by
    // accessible name, not by text: the strip prints "DOM" and carries the whole
    // word on the heading, so this also pins that the short label never reaches
    // assistive tech.
    const sundayHeading = screen.getByRole('heading', { name: 'Domingo', level: 2 });
    const sundayDay = sundayHeading.closest('li');
    expect(sundayDay).not.toBeNull();
    expect(sundayDay).toHaveTextContent('Descanso');
    expect(sundayDay?.querySelector('button')).toBeNull();
  });

  it('opens the session detail for a strength session', async () => {
    getWeekMock.mockResolvedValue(week);
    const user = userEvent.setup();

    renderPage();
    await screen.findByRole('heading', { name: 'Hoy · Lunes' });

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
    await screen.findByRole('heading', { name: 'Hoy · Lunes' });
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
    await screen.findByRole('heading', { name: 'Hoy · Lunes' });
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
    await screen.findByRole('heading', { name: 'Hoy · Lunes' });

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
    await screen.findByRole('heading', { name: 'Hoy · Lunes' });

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

    const runningTile = screen
      .getByRole('heading', { name: 'Carreras', level: 2 })
      .closest('div') as HTMLElement;
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
    await screen.findByRole('heading', { name: 'Hoy · Lunes' });

    await user.click(screen.getByRole('button', { name: /Carrera.*Tirada larga/s }));
    await user.click(screen.getByRole('button', { name: 'Completar' }));

    const region = screen.getByRole('log');
    expect(await within(region).findByText(/marcado como completado/i)).toBeInTheDocument();
  });

  it('counts the week once in the stats strip, with no percentages to disagree', async () => {
    getWeekMock.mockResolvedValue(week);

    renderPage();
    await screen.findByRole('heading', { name: 'Hoy · Lunes' });

    const stats = within(screen.getByRole('region', { name: 'Resumen de la semana' }));
    const tile = (name: string) =>
      within(stats.getByRole('heading', { name, level: 2 }).closest('div') as HTMLElement);

    expect(tile('Sesiones').getByText('0 / 2')).toBeInTheDocument();
    expect(tile('Carreras').getByText('0 / 1')).toBeInTheDocument();
    expect(tile('Fuerza').getByText('0 / 1')).toBeInTheDocument();
    // The three rings the old summary card drew are gone with it: a percentage
    // of two sessions says nothing the fraction beside it does not.
    expect(stats.queryByText('0%')).toBeNull();
    expect(stats.getByRole('link', { name: /Ver estadísticas completas/i })).toHaveAttribute(
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

    // Monday expands by default and keeps its whole title, so the prefix rule
    // has to be read on a compact column: move the expansion off Monday first.
    await screen.findByRole('heading', { name: 'Hoy · Lunes' });
    await userEvent.click(screen.getByRole('button', { name: 'Día siguiente' }));
    const strip = screen.getByRole('list', { name: 'Semana de entrenamiento' });

    expect(within(strip).getByText('Empuje')).toBeInTheDocument();
    expect(within(strip).queryByText('Fuerza · Empuje')).not.toBeInTheDocument();
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
    await screen.findByRole('heading', { name: 'Hoy · Lunes' });

    const weekday = screen.getByTestId('date-weekday');

    expect(weekday.textContent).toMatch(
      /^(Lunes|Martes|Miércoles|Jueves|Viernes|Sábado|Domingo),$/,
    );
  });

  it('names each day status on the dot itself, with no legend to key it to', async () => {
    getWeekMock.mockResolvedValue(week);
    renderPage();
    await screen.findByRole('heading', { name: 'Hoy · Lunes' });

    // The legend row went with the calendar card it lived in. The dot carries
    // its own name instead — for assistive tech via `aria-label`, and for a
    // sighted reader via the tooltip, so nothing depends on remembering a key.
    const tuesday = screen.getByRole('heading', { name: 'Martes', level: 2 }).closest('li');
    expect(within(tuesday as HTMLElement).getByLabelText('Planificado')).toHaveAttribute(
      'title',
      'Planificado',
    );
    expect(screen.queryByRole('list', { name: 'Leyenda del calendario' })).toBeNull();
  });

  it('shows each day status in the card corner', async () => {
    getWeekMock.mockResolvedValue(week);
    renderPage();
    await screen.findByRole('heading', { name: 'Hoy · Lunes' });

    // Read on Tuesday: Monday is the expanded column, which states its status
    // in words rather than as a dot.
    const tuesday = screen.getByRole('heading', { name: 'Martes', level: 2 }).closest('li');
    expect(within(tuesday as HTMLElement).getByLabelText('Planificado')).toBeInTheDocument();
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
      const todayCard = (await screen.findByRole('heading', { name: 'Hoy · Lunes' })).closest(
        'li',
      ) as HTMLElement;

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
      const todayCard = (await screen.findByRole('heading', { name: 'Hoy · Lunes' })).closest(
        'li',
      ) as HTMLElement;

      expect(await within(todayCard).findByText('Enfoque: Pecho, Tríceps')).toBeInTheDocument();
    });
  });

  /*
   * A run is one session with exactly two states, pending or done, so the ring
   * beside it could only ever read 0% or 100% — a progress dial for something
   * that has no progress. The run card drops it and gives the figure the middle
   * of the card, the way the rest day already does.
   */
  /*
   * The dial went with the card it lived in. It counted *sessions*, and a day
   * holds one or two, so it could only ever read 0%, 50% or 100% — three
   * positions on a control that looks like it has a hundred. The expanded day
   * states its status in a word instead, which is the same fact without the
   * pretence of precision.
   */
  describe('el estado del día', () => {
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

    it('is stated as a word, with no percentage dial anywhere on the page', async () => {
      getWeekMock.mockResolvedValue(week);

      renderPage();
      const monday = (await screen.findByRole('heading', { name: 'Hoy · Lunes' })).closest(
        'li',
      ) as HTMLElement;

      expect(within(monday).getByText('Planificado')).toBeInTheDocument();
      expect(screen.queryByText('0%')).toBeNull();
      expect(screen.queryByText('En progreso')).toBeNull();
      expect(screen.queryByText(/0 \/ 1 sesión/)).toBeNull();
    });

    it('says the same for a run, which only ever has two of them', async () => {
      getWeekMock.mockResolvedValue(runningToday);

      renderPage();
      const monday = (await screen.findByRole('heading', { name: 'Hoy · Lunes' })).closest(
        'li',
      ) as HTMLElement;

      expect(within(monday).getByText('Series')).toBeInTheDocument();
      expect(within(monday).getByText('Planificado')).toBeInTheDocument();
      expect(within(monday).getByRole('button', { name: 'Completar carrera' })).toBeInTheDocument();
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
      const runningTile = screen
        .getByRole('heading', { name: 'Carreras', level: 2 })
        .closest('div') as HTMLElement;
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
  describe('the date arrows move the expanded column across the week', () => {
    it('expands the chosen day and names it', async () => {
      getWeekMock.mockResolvedValue(week);
      const user = userEvent.setup();

      renderPage();
      await screen.findByRole('heading', { name: 'Hoy · Lunes' });

      await user.click(screen.getByRole('button', { name: 'Día siguiente' }));

      const card = (await screen.findByRole('heading', { name: 'Martes' })).closest(
        'li',
      ) as HTMLElement;
      expect(within(card).getByText('Tirada larga')).toBeInTheDocument();
      expect(screen.getByTestId('date-weekday')).toHaveTextContent('Martes,');
    });

    it('stops at the edges of the week the API actually returns', async () => {
      getWeekMock.mockResolvedValue(week);
      const user = userEvent.setup();

      renderPage();
      await screen.findByRole('heading', { name: 'Hoy · Lunes' });

      // TODAY is the Monday of the fixture week, so there is nothing before it.
      expect(screen.getByRole('button', { name: 'Día anterior' })).toBeDisabled();

      for (let step = 0; step < 6; step += 1) {
        await user.click(screen.getByRole('button', { name: 'Día siguiente' }));
      }
      expect(screen.getByRole('button', { name: 'Día siguiente' })).toBeDisabled();
      expect(await screen.findByRole('heading', { name: 'Domingo' })).toBeInTheDocument();
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
    await screen.findByRole('heading', { name: 'Hoy · Lunes' });

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

    const sunday = (await screen.findByRole('heading', { name: 'Hoy · Domingo' })).closest(
      'li',
    ) as HTMLElement;
    expect(within(sunday).getByText('Descanso')).toBeInTheDocument();
    expect(within(sunday).getByText('El plan no trae sesión para este día.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Entrenar' })).toBeNull();
  });

  // FOR-143: streak + weekly-history widgets, consuming the FOR-139 endpoints
  // to replace the "RACHA ACTUAL"/weekly-history gap this page's doc comment
  // documented (mockup docs/3-entrenamiento.png). Each fetches independently
  // of the training week and of each other (FOR-60 pattern).
  describe('streak tile (FOR-143)', () => {
    it('shows the current and longest streak once loaded', async () => {
      getWeekMock.mockResolvedValue(week);
      getStreakMock.mockResolvedValue({
        currentStreakDays: 4,
        longestStreakDays: 12,
        asOf: '2026-07-06',
      });

      renderPage();

      const card = (await screen.findByRole('heading', { name: 'Racha', level: 2 })).closest(
        'div',
      ) as HTMLElement;
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

      const card = (await screen.findByRole('heading', { name: 'Racha' })).closest(
        'div',
      ) as HTMLElement;
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
      await screen.findByRole('heading', { name: 'Hoy · Lunes' });
      expect(screen.getByText('Cargando racha…')).toBeInTheDocument();
    });

    it('shows an error scoped to the streak card and recovers on retry', async () => {
      getWeekMock.mockResolvedValue(week);
      getStreakMock.mockRejectedValueOnce(new Error('network'));
      const user = userEvent.setup();

      renderPage();

      const card = (await screen.findByRole('heading', { name: 'Racha' })).closest(
        'div',
      ) as HTMLElement;
      expect(await within(card).findByRole('alert')).toHaveTextContent(
        'No se pudo cargar tu racha',
      );
      // The sibling tiles in the same strip still rendered normally.
      expect(screen.getByRole('heading', { name: 'Sesiones', level: 2 })).toBeInTheDocument();

      getStreakMock.mockResolvedValue({
        currentStreakDays: 4,
        longestStreakDays: 12,
        asOf: '2026-07-06',
      });
      await user.click(within(card).getByRole('button', { name: 'Reintentar' }));

      expect(await within(card).findByText('4')).toBeInTheDocument();
    });
  });

  /*
   * FOR — "Entrenamiento sin scroll": the week stops being a card and becomes the
   * page. Seven columns across, the selected day expanded inside the row rather
   * than living in a card of its own, and one strip of counters underneath.
   *
   * Design canvas: docs/design/entrenamiento-sin-scroll (direction C).
   *
   * These tests describe the shape the redesign has to hold, not its pixels: one
   * list for the whole week, one expanded day inside it, and counters that only
   * count what the API actually returns.
   */
  describe('la semana como columna vertebral', () => {
    /*
     * A day column is found by its own heading: the expanded one reads
     * "Hoy · Lunes", a compact one carries the whole weekday as the accessible
     * name behind its abbreviation. Indexing the list items instead would count
     * the nested list a two-session day renders.
     */
    const dayColumn = (heading: string) =>
      screen.getByRole('heading', { name: heading }).closest('li') as HTMLElement;

    const fullWeek: TrainingWeek = {
      days: [
        {
          dayOfWeek: 'MONDAY',
          rest: false,
          sessions: [
            {
              id: 'MONDAY:STRENGTH',
              kind: 'STRENGTH',
              bodyView: 'BACK',
              title: 'Fuerza · Tirón',
              detail: '5 ejercicios',
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
              title: 'Carrera · Series',
              detail: '4.0 km',
              status: 'PLANNED',
            },
          ],
        },
        {
          dayOfWeek: 'WEDNESDAY',
          rest: false,
          sessions: [
            {
              id: 'WEDNESDAY:STRENGTH',
              kind: 'STRENGTH',
              bodyView: 'FRONT',
              title: 'Fuerza · Empuje',
              detail: '6 ejercicios',
              status: 'COMPLETED',
            },
          ],
        },
        { dayOfWeek: 'SUNDAY', rest: true, sessions: [] },
      ],
    };

    beforeEach(() => {
      getWeekMock.mockResolvedValue(fullWeek);
    });

    it('renders one list for the whole week, with a card per day', async () => {
      renderPage();

      const strip = await screen.findByRole('list', { name: 'Semana de entrenamiento' });
      // One heading per day the API returned, and nothing else: the week is the
      // page now, so there is no second calendar to keep in sync with this one.
      // Counted by heading rather than by list item because a day with two
      // sessions nests a list of its own.
      expect(within(strip).getAllByRole('heading', { level: 2 })).toHaveLength(4);
      expect(screen.queryByRole('heading', { name: 'Calendario semanal' })).toBeNull();
      expect(screen.queryByRole('heading', { name: 'Entrenamiento de hoy' })).toBeNull();
    });

    it('expands the selected day inside the strip and leaves the rest compact', async () => {
      renderPage();

      await screen.findByRole('list', { name: 'Semana de entrenamiento' });
      const monday = dayColumn('Hoy · Lunes');
      const tuesday = dayColumn('Martes');

      // The expanded day carries the whole session: full title, its detail and
      // the actions. Today is Monday (fixed clock at the top of this file).
      expect(within(monday).getByText('Fuerza · Tirón')).toBeInTheDocument();
      expect(within(monday).getByText('5 ejercicios')).toBeInTheDocument();
      expect(within(monday).getByRole('button', { name: 'Entrenar' })).toBeInTheDocument();

      // A compact day names its session and opens the detail; it carries no
      // actions of its own, which is what keeps seven columns readable.
      expect(within(tuesday).getByText('Series')).toBeInTheDocument();
      expect(within(tuesday).queryByRole('button', { name: 'Entrenar' })).toBeNull();
    });

    it('moves the expanded column with the date arrows', async () => {
      renderPage();

      await screen.findByRole('list', { name: 'Semana de entrenamiento' });
      await userEvent.click(screen.getByRole('button', { name: 'Día siguiente' }));

      const monday = dayColumn('Lunes');
      const tuesday = dayColumn('Martes');
      expect(within(tuesday).getByText('Carrera · Series')).toBeInTheDocument();
      expect(
        within(tuesday).getByRole('button', { name: 'Completar carrera' }),
      ).toBeInTheDocument();
      expect(within(monday).queryByRole('button', { name: 'Entrenar' })).toBeNull();
    });

    it('counts the week once, in one strip, and drops the metrics nothing backs', async () => {
      renderPage();

      const stats = await screen.findByRole('region', { name: 'Resumen de la semana' });
      const view = within(stats);

      expect(view.getByRole('heading', { name: 'Sesiones', level: 2 })).toBeInTheDocument();
      expect(view.getByText('1 / 3')).toBeInTheDocument(); // completadas / planificadas
      expect(view.getByRole('heading', { name: 'Carreras', level: 2 })).toBeInTheDocument();
      expect(view.getByRole('heading', { name: 'Fuerza', level: 2 })).toBeInTheDocument();

      // Volumen, duración y calorías eran constantes en el propio componente: no
      // hay endpoint que las devuelva, así que dejan de mostrarse.
      expect(screen.queryByText(/Volumen total/i)).toBeNull();
      expect(screen.queryByText(/Duración total/i)).toBeNull();
      expect(screen.queryByText(/Calorías estimadas/i)).toBeNull();
      expect(screen.queryByText(/vs semana anterior/i)).toBeNull();
    });

    it('carries the streak in the same strip instead of a card of its own', async () => {
      renderPage();

      const stats = await screen.findByRole('region', { name: 'Resumen de la semana' });

      expect(await within(stats).findByText('4')).toBeInTheDocument();
      expect(within(stats).getByRole('heading', { name: 'Racha', level: 2 })).toBeInTheDocument();
      expect(screen.queryByRole('heading', { name: 'Racha actual' })).toBeNull();
      expect(screen.queryByRole('heading', { name: 'Resumen semanal' })).toBeNull();
      expect(screen.queryByRole('heading', { name: 'Distribución semanal' })).toBeNull();
    });

    it('lets a rest day pull a session across from another day', async () => {
      vi.setSystemTime(new Date('2026-07-12T09:00:00')); // domingo
      rescheduleMock.mockResolvedValue(fullWeek);

      renderPage();

      await screen.findByRole('list', { name: 'Semana de entrenamiento' });
      const sunday = dayColumn('Hoy · Domingo');

      expect(within(sunday).getByText('Descanso')).toBeInTheDocument();
      await userEvent.selectOptions(
        within(sunday).getByLabelText('Mover una sesión a este día'),
        'TUESDAY:RUNNING',
      );

      expect(rescheduleMock).toHaveBeenCalledWith('TUESDAY:RUNNING', 'SUNDAY');
    });
  });
});
