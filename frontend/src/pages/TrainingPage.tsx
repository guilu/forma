import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { MuscleSilhouette, type AnatomySex } from '../components/MuscleSilhouette';
import { Button } from '../components/Button';
import { NoPlanEmptyState } from '../components/NoPlanEmptyState';
import { ErrorState } from '../components/ErrorState';
import { Icon, type IconName } from '../components/Icon';
import { LoadingState } from '../components/LoadingState';
import { Modal } from '../components/Modal';
import { useNotify } from '../components/NotificationProvider';
import { useAnatomySex } from '../hooks/useAnatomySex';
import { StatusPill } from '../components/StatusPill';
import { statusLabel } from '../components/statusLabels';
import { WidgetLoading } from '../components/WidgetLoading';
import { IconButton } from '../components/IconButton';
import { ApiRequestError } from '../api/client';
import { getStreak, type Streak } from '../api/progress';
import {
  getMuscleMap,
  getTrainingWeek,
  getWorkout,
  rescheduleSession,
  updateSessionStatus,
  type DayOfWeek,
  type MuscleWorked,
  type SessionStatus,
  type TrainingDay,
  type TrainingSession,
  type TrainingWeek,
  type WorkoutItem,
} from '../api/training';
import { groupMusclesForDisplay, type MuscleGroupDisplay } from './trainingMuscleLabels';
import { overlayFromMuscleMap, roleForLoad, viewForMuscle } from './trainingMuscleOverlay';
import { displayTitle, prescriptionSummary } from './trainingPrescription';
import { useSessionMuscles } from './useSessionMuscles';
import { formatShortDate, formatWeekday } from './dateLabel';
import styles from './TrainingPage.module.css';

/**
 * Training page (FOR-26/FOR-27), redesigned so the whole week fits the window:
 * one strip of days with the selected one expanded in place, and one row of
 * counters under it. Design canvas: `docs/design/entrenamiento-sin-scroll`
 * (direction C). Everything is read from the FOR-26 training week API
 * (`GET /api/v1/training/week`); completion is the FOR-27 `PATCH …/status`
 * call and moving a session is `PATCH …/schedule`. Renders the API read model
 * directly (ADR-006); no training rule (scheduling, progression) lives here.
 *
 * <p>The redesign removed three blocks rather than shrinking them, and the
 * reason in each case is that the page was saying the same thing twice:
 * <ul>
 *   <li>"Entrenamiento de hoy" and "Calendario semanal" were the same day drawn
 *       at two sizes, kept in step by hand. Today is now simply the column that
 *       expands.
 *   <li>"Resumen semanal", the "Sesiones completadas" tile and "Distribución
 *       semanal" all counted the same sessions; the strip counts them once.
 *   <li>"Volumen total", "Duración total" and "Calorías estimadas" were fixed
 *       strings in this file. No calories/volume/duration field exists anywhere
 *       in the training domain or API, so they are gone rather than moved.
 * </ul>
 *
 * <p>Still not backed by any endpoint (documented gap, not invented — AGENTS.md
 * "repository state has priority"):
 * <ul>
 *   <li>Per-exercise rows (series/reps/peso/descanso/estado) and per-exercise
 *       completion — the FOR-25 {@code WorkoutTemplateService} exists in the
 *       backend but is never wired to a controller, so the frontend only ever
 *       sees each session's plain {@code detail} summary string (e.g. "3
 *       ejercicios"). Shown as a labelled placeholder in the session detail
 *       dialog instead of a fabricated table.
 *   <li>"Editar entrenamiento" — no endpoint mutates workout templates.
 * </ul>
 *
 * <p>What *is* backed: the muscle-worked heatmap (FOR-136, {@code GET
 * …/sessions/{id}/muscle-map}), normalized for display by
 * {@code trainingMuscleLabels} (spec FOR-53: the frontend, not the backend,
 * owns that normalization); and the streak tile (FOR-143 over the FOR-139
 * {@code GET …/progress/streak} endpoint), which is a real nutrition meal-log
 * consistency signal, not a fabricated training streak.
 *
 * <p>The weekly counts are *not* the FOR-28 {@code WeeklyTrainingSummary} —
 * that calculation is application-layer only and is not exposed over HTTP. This
 * page tallies the sessions {@code GET /training/week} already returned,
 * exactly like the FOR-51 {@code TrainingWidget} does.
 *
 * <p>The date arrows move which column is expanded, within the composed week
 * and no further: `docs/api/training-week.md` states the week has "no dates, no
 * week navigation", so they stop at Monday and Sunday.
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly week: TrainingWeek };

interface DetailTarget {
  readonly dayOfWeek: string;
  readonly session: TrainingSession;
}

const DAY_LABELS: Record<string, string> = {
  MONDAY: 'Lunes',
  TUESDAY: 'Martes',
  WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves',
  FRIDAY: 'Viernes',
  SATURDAY: 'Sábado',
  SUNDAY: 'Domingo',
};

/**
 * Day names for the seven-column week strip, where the full ones do not fit.
 *
 * <p>At 1280px each card is about 76px wide and carries a status dot pinned to
 * its corner, which left "MIÉRCOLES" overlapping that dot by 13px and "DOMINGO"
 * by 8. Shortening the label fixes it at every width, which shrinking the type
 * would not: the full names come back the moment the window narrows or an
 * eighth column appears.
 *
 * <p>Only the strip uses these. Everywhere with room — today's card, the detail
 * dialog, the reschedule menu, the toast — keeps the whole word, and so does
 * every screen reader, since the heading carries the full name as its
 * accessible name.
 */
const DAY_LABELS_SHORT: Record<string, string> = {
  MONDAY: 'Lun',
  TUESDAY: 'Mar',
  WEDNESDAY: 'Mié',
  THURSDAY: 'Jue',
  FRIDAY: 'Vie',
  SATURDAY: 'Sáb',
  SUNDAY: 'Dom',
};

const KIND_LABELS: Record<TrainingSession['kind'], string> = {
  RUNNING: 'Carrera',
  STRENGTH: 'Fuerza',
};

const MARK_ERROR = 'No se pudo actualizar la sesión. Inténtalo de nuevo.';
const MOVE_ERROR = 'No se pudo mover la sesión. Inténtalo de nuevo.';

/** JS `Date#getDay()` (0 = Sunday) indexed to the backend's `dayOfWeek` names. */
const JS_DAY_TO_ENUM = [
  'SUNDAY',
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
] as const;

function todayDayOfWeek(): string {
  return JS_DAY_TO_ENUM[new Date().getDay()];
}

/**
 * The week the arrows walk, in the order the API composes it.
 *
 * <p>Monday-to-Sunday and nothing beyond: `GET /training/week` returns the
 * *current* week and accepts no date parameter (`docs/api/training-week.md` —
 * "no dates, no week navigation"). Stepping past either end would have no data
 * to show, so the controls stop there instead of promising a week the backend
 * cannot answer for.
 */
const WEEK_ORDER = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
] as const;

function weekIndexOf(dayOfWeek: string): number {
  const index = WEEK_ORDER.indexOf(dayOfWeek as (typeof WEEK_ORDER)[number]);
  return index === -1 ? 0 : index;
}

/** The real calendar date of a day in the composed week, relative to today. */
function dateOfWeekday(dayOfWeek: string): Date {
  const date = new Date();
  date.setDate(date.getDate() + (weekIndexOf(dayOfWeek) - weekIndexOf(todayDayOfWeek())));
  return date;
}

/**
 * "Fuerza · Empuje" -> "Empuje" for the calendar day cards, where the kind is
 * already on the badge right above the title. The today card keeps the full
 * title: it has one session and no grid column to fit it into.
 */
function stripKindPrefix(title: string): string {
  return title.replace(/^(?:Fuerza|Carrera)\s*·\s*/i, '');
}

function tally(sessions: readonly TrainingSession[]): { completed: number; planned: number } {
  return {
    completed: sessions.filter((s) => s.status === 'COMPLETED').length,
    planned: sessions.length,
  };
}

export function TrainingPage() {
  const notify = useNotify();
  const navigate = useNavigate();
  const [state, setState] = useState<State>({ status: 'loading' });
  const anatomySex = useAnatomySex();
  const [pendingId, setPendingId] = useState<string | undefined>(undefined);
  const [detailTarget, setDetailTarget] = useState<DetailTarget | undefined>(undefined);
  const [selectedDay, setSelectedDay] = useState<string>(() => todayDayOfWeek());

  const load = useCallback(async () => {
    try {
      const week = await getTrainingWeek();
      setState({ status: 'ready', week });
    } catch {
      setState({ status: 'error' });
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function mark(sessionId: string, status: SessionStatus) {
    setPendingId(sessionId);
    try {
      await updateSessionStatus(sessionId, status);
      await load();
      setDetailTarget((current) =>
        current && current.session.id === sessionId
          ? { ...current, session: { ...current.session, status } }
          : current,
      );
      // Success feedback for the "complete training" key action (FOR-63:
      // "Success feedback: toast or inline confirmation after key actions").
      // Skipping a session is not a "success" moment, so it stays silent
      // (ui-guidelines.md: "no guilt language" cuts both ways here).
      if (status === 'COMPLETED') {
        notify.success('Entrenamiento marcado como completado.');
      }
    } catch (error) {
      // Every failure goes to the notification region, the same place a success
      // does — rather than to a band this page draws for itself, which is how
      // the app ended up telling you about a failure in a different spot on
      // every screen.
      notify.error(error instanceof ApiRequestError ? error.message : MARK_ERROR);
    } finally {
      setPendingId(undefined);
    }
  }

  /**
   * Moves a session to another day of this week. The week is refetched rather
   * than patched in place: the move changes which day every other session
   * shares it with, so the calendar has to redraw anyway. The detail closes
   * because the session it was showing is no longer on the day it was opened
   * from.
   */
  async function move(sessionId: string, day: DayOfWeek) {
    setPendingId(sessionId);
    try {
      await rescheduleSession(sessionId, day);
      await load();
      setDetailTarget(undefined);
      notify.success(`Sesión movida a ${DAY_LABELS[day].toLocaleLowerCase('es-ES')}.`);
    } catch (error) {
      notify.error(error instanceof ApiRequestError ? error.message : MOVE_ERROR);
    } finally {
      setPendingId(undefined);
    }
  }

  const selectedIndex = weekIndexOf(selectedDay);
  const selectedDate = dateOfWeekday(selectedDay);

  return (
    <div className={styles.wrapper}>
      <header className={styles.header}>
        <div className={styles.titles}>
          <h1 className={styles.title}>Entrenamiento</h1>
          <p className={styles.subtitle}>Sigue tu plan y mejora cada día.</p>
        </div>
        {/* Date navigator. Bounded by the composed week: `GET /training/week`
            returns the current week and takes no date, so the arrows walk
            Monday-to-Sunday and stop there rather than asking for a week the
            API cannot answer for (docs/api/training-week.md). */}
        <div className={styles.dateNav}>
          <Icon name="calendar" size={16} className={styles.dateIcon} />
          <span className={styles.dateLabel}>
            <span className={styles.dateWeekday} data-testid="date-weekday">
              {formatWeekday(selectedDate)},
            </span>{' '}
            {formatShortDate(selectedDate)}
          </span>
          <IconButton
            variant="ghost"
            size="sm"
            label="Día anterior"
            disabled={selectedIndex === 0}
            onClick={() => setSelectedDay(WEEK_ORDER[selectedIndex - 1])}
          >
            <Icon name="chevron" size={16} className={styles.dateArrowPrev} />
          </IconButton>
          <IconButton
            variant="ghost"
            size="sm"
            label="Día siguiente"
            disabled={selectedIndex === WEEK_ORDER.length - 1}
            onClick={() => setSelectedDay(WEEK_ORDER[selectedIndex + 1])}
          >
            <Icon name="chevron" size={16} />
          </IconButton>
        </div>
      </header>

      {renderContent(
        state,
        mark,
        move,
        pendingId,
        setDetailTarget,
        (session) => navigate(`/app/training/${encodeURIComponent(session.id)}`),
        load,
        selectedDay,
        anatomySex,
      )}

      {detailTarget && (
        <SessionDetailModal
          target={detailTarget}
          onClose={() => setDetailTarget(undefined)}
          move={move}
          mark={mark}
          pending={pendingId === detailTarget.session.id}
          anatomySex={anatomySex}
        />
      )}
    </div>
  );
}

function renderContent(
  state: State,
  mark: (id: string, status: SessionStatus) => void,
  move: (id: string, day: DayOfWeek) => void,
  pendingId: string | undefined,
  openDetail: (target: DetailTarget) => void,
  openTraining: (session: TrainingSession) => void,
  reload: () => void,
  selectedDay: string,
  anatomySex: AnatomySex,
) {
  if (state.status === 'loading') {
    return <LoadingState message="Cargando tu semana…" />;
  }

  if (state.status === 'error') {
    return (
      <ErrorState
        message="No se pudo cargar tu semana de entrenamiento. Inténtalo de nuevo más tarde."
        onRetry={reload}
      />
    );
  }

  const hasAnySession = state.week.days.some((day) => day.sessions.length > 0);
  if (!hasAnySession) {
    return <NoPlanEmptyState />;
  }

  return (
    <div className={styles.layout}>
      <WeekStrip
        days={state.week.days}
        selectedDay={selectedDay}
        mark={mark}
        move={move}
        pendingId={pendingId}
        openDetail={openDetail}
        openTraining={openTraining}
        anatomySex={anatomySex}
      />
      <WeekStats days={state.week.days} />
    </div>
  );
}

/**
 * The week, as the page rather than as a card on it.
 *
 * <p>This replaces the pair of blocks the page used to carry — an
 * "Entrenamiento de hoy" card above a "Calendario semanal" card — which were
 * the same information drawn twice: today appeared once in full and again as
 * one of seven columns, and the two had to be kept in step by hand. Here the
 * selected day is simply the column that expands, so there is one source for
 * what a day says and nothing to synchronise.
 *
 * <p>That is also what buys the page its height back. Design canvas:
 * {@code docs/design/entrenamiento-sin-scroll}, direction C.
 */
function WeekStrip({
  days,
  selectedDay,
  mark,
  move,
  pendingId,
  openDetail,
  openTraining,
  anatomySex,
}: {
  readonly days: readonly TrainingDay[];
  readonly selectedDay: string;
  readonly mark: (id: string, status: SessionStatus) => void;
  readonly move: (id: string, day: DayOfWeek) => void;
  readonly pendingId: string | undefined;
  readonly openDetail: (target: DetailTarget) => void;
  readonly openTraining: (session: TrainingSession) => void;
  readonly anatomySex: AnatomySex;
}) {
  const todayEnum = todayDayOfWeek();

  return (
    <ul className={styles.weekStrip} aria-label="Semana de entrenamiento">
      {days.map((day) => {
        const expanded = day.dayOfWeek === selectedDay;
        return (
          <li
            key={day.dayOfWeek}
            className={expanded ? styles.weekDayExpanded : styles.weekDay}
            aria-current={day.dayOfWeek === todayEnum ? 'date' : undefined}
          >
            {expanded ? (
              <ExpandedDay
                day={day}
                isToday={day.dayOfWeek === todayEnum}
                days={days}
                mark={mark}
                move={move}
                pendingId={pendingId}
                openDetail={openDetail}
                openTraining={openTraining}
                anatomySex={anatomySex}
              />
            ) : (
              <CompactDay day={day} openDetail={openDetail} anatomySex={anatomySex} />
            )}
          </li>
        );
      })}
    </ul>
  );
}

/**
 * One of the six columns that are not the selected day.
 *
 * <p>Name, status dot, body and title — and no actions. At this width a pair
 * of buttons would take the whole card and leave no room for the thing the
 * column is for, so acting on another day goes through its detail, which is
 * where moving and marking already live.
 */
function CompactDay({
  day,
  openDetail,
  anatomySex,
}: {
  readonly day: TrainingDay;
  readonly openDetail: (target: DetailTarget) => void;
  readonly anatomySex: AnatomySex;
}) {
  return (
    <>
      <div className={styles.compactHead}>
        {/* Abbreviated on screen, whole word to assistive tech: "MIÉ" is a
            glance-able column header for someone reading the strip and a worse
            label for someone hearing it one card at a time. */}
        <h2
          className={styles.compactDayName}
          aria-label={DAY_LABELS[day.dayOfWeek] ?? day.dayOfWeek}
        >
          {DAY_LABELS_SHORT[day.dayOfWeek] ?? day.dayOfWeek}
        </h2>
        {!day.rest && (
          <span
            className={styles.compactStatus}
            data-status={dayStatus(day)}
            role="img"
            aria-label={SESSION_STATUS_LABELS[dayStatus(day)]}
            /* The legend row this strip replaced is gone with the calendar card,
               so the dot has to carry its own key for a sighted reader too. */
            title={SESSION_STATUS_LABELS[dayStatus(day)]}
          />
        )}
      </div>

      {day.rest ? (
        <div className={styles.compactRest}>
          <MuscleSilhouette className={styles.compactFigure} sex={anatomySex} variant="rest" />
          <span className={styles.compactTitle}>Descanso</span>
        </div>
      ) : (
        <ul className={styles.compactSessions}>
          {day.sessions.map((session) => (
            <li key={session.id}>
              <button
                type="button"
                className={styles.compactButton}
                /*
                 * Names what the column no longer writes down: the kind and the
                 * status live in the silhouette and the corner dot, both
                 * decorative, so without this a screen reader would hear only
                 * "Tirada larga".
                 */
                aria-label={`${KIND_LABELS[session.kind]} · ${stripKindPrefix(session.title)}. ${
                  SESSION_STATUS_LABELS[session.status]
                }`}
                onClick={() => openDetail({ dayOfWeek: day.dayOfWeek, session })}
              >
                <DayFigure session={session} sex={anatomySex} />
                <span className={styles.compactTitle}>{stripKindPrefix(session.title)}</span>
                <span className={styles.compactDetail}>{session.detail}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}

/**
 * The selected day, opened in place.
 *
 * <p>Everything the old "Entrenamiento de hoy" card carried, minus the
 * completion ring: the ring counted sessions, a day holds one or two, and a
 * dial that can only read 0% or 100% is a decoration that asks to be read. The
 * status says the same thing in a word.
 */
function ExpandedDay({
  day,
  isToday,
  days,
  mark,
  move,
  pendingId,
  openDetail,
  openTraining,
  anatomySex,
}: {
  readonly day: TrainingDay;
  readonly isToday: boolean;
  readonly days: readonly TrainingDay[];
  readonly mark: (id: string, status: SessionStatus) => void;
  readonly move: (id: string, day: DayOfWeek) => void;
  readonly pendingId: string | undefined;
  readonly openDetail: (target: DetailTarget) => void;
  readonly openTraining: (session: TrainingSession) => void;
  readonly anatomySex: AnatomySex;
}) {
  const label = DAY_LABELS[day.dayOfWeek] ?? day.dayOfWeek;
  const sessions = day.sessions;
  const visual = sessions.find((session) => session.kind === 'STRENGTH') ?? sessions[0];

  return (
    <>
      <div className={styles.expandedHead}>
        <h2 className={styles.expandedDayName}>{isToday ? `Hoy · ${label}` : label}</h2>
        {!day.rest && (
          /* Wrapped because `StatusPill` takes no className, and this is the
             element the open-day entrance animation hangs off. */
          <span className={styles.expandedStatus}>
            <StatusPill kind="training" value={dayStatus(day)} />
          </span>
        )}
      </div>

      {day.rest ? (
        <RestDay day={day} days={days} move={move} pendingId={pendingId} anatomySex={anatomySex} />
      ) : (
        <>
          <div className={styles.expandedSessions}>
            {sessions.map((session) => (
              <div key={session.id} className={styles.expandedSession}>
                <p className={styles.expandedTitle}>{session.title}</p>
                <p className={styles.sessionDetail}>{session.detail}</p>
                {session.kind === 'STRENGTH' && <SessionFocus sessionId={session.id} />}
              </div>
            ))}
          </div>

          <ExpandedFigures session={visual} sex={anatomySex} />

          <div className={styles.expandedActions}>
            {sessions.map((session) => (
              <div
                key={session.id}
                className={styles.expandedActionGroup}
                role="group"
                aria-label={session.title}
              >
                {session.status === 'PLANNED' && (
                  <IconButton
                    variant="ghost"
                    size="lg"
                    label="Saltar la sesión"
                    title="Saltar la sesión"
                    disabled={pendingId === session.id}
                    onClick={() => mark(session.id, 'SKIPPED')}
                  >
                    <Icon name="skip" size={19} />
                  </IconButton>
                )}
                <IconButton
                  variant="surface"
                  size="lg"
                  label="Ver el detalle"
                  title="Ver el detalle"
                  onClick={() => openDetail({ dayOfWeek: day.dayOfWeek, session })}
                >
                  <Icon name="menu" size={19} />
                </IconButton>
                {session.kind === 'STRENGTH' ? (
                  <IconButton
                    variant="soft"
                    size="lg"
                    label="Entrenar"
                    title="Entrenar"
                    onClick={() => openTraining(session)}
                  >
                    <Icon name="play" size={19} />
                  </IconButton>
                ) : session.status === 'COMPLETED' ? (
                  /* A run has no per-exercise screen to open, so its action marks
                     completion in place — and undoes it, or a mistaken tap would
                     be permanent. Back to PLANNED, not SKIPPED: undoing means
                     "this did not happen yet". */
                  <IconButton
                    variant="surface"
                    size="lg"
                    label="Desmarcar la carrera como completada"
                    title="Desmarcar la carrera como completada"
                    loading={pendingId === session.id}
                    onClick={() => mark(session.id, 'PLANNED')}
                  >
                    <Icon name="checkCircle" size={19} />
                  </IconButton>
                ) : (
                  <IconButton
                    variant="soft"
                    size="lg"
                    label="Completar carrera"
                    title="Completar carrera"
                    loading={pendingId === session.id}
                    onClick={() => mark(session.id, 'COMPLETED')}
                  >
                    {/* The finish line, not a tick: a tick is the same mark the
                        calendar dots already use for "done", and this is the
                        control that *makes* it done. */}
                    <Icon name="flag" size={19} />
                  </IconButton>
                )}
              </div>
            ))}
          </div>
        </>
      )}
    </>
  );
}

/**
 * The selected day when the plan leaves it empty.
 *
 * <p>There is nothing to complete, so the action cannot be "Entrenar". The one
 * real thing a rest day affords is pulling a session across from another day,
 * which is the {@code PATCH …/schedule} move the detail already uses — offered
 * here as a picker because from this side the question is "which session", not
 * "which day".
 */
function RestDay({
  day,
  days,
  move,
  pendingId,
  anatomySex,
}: {
  readonly day: TrainingDay;
  readonly days: readonly TrainingDay[];
  readonly move: (id: string, day: DayOfWeek) => void;
  readonly pendingId: string | undefined;
  readonly anatomySex: AnatomySex;
}) {
  const movable = days.flatMap((other) =>
    other.dayOfWeek === day.dayOfWeek
      ? []
      : other.sessions.map((session) => ({ session, from: other.dayOfWeek })),
  );

  return (
    <div className={styles.restLayout}>
      <p className={styles.expandedTitle}>Descanso</p>
      <p className={styles.rest}>El plan no trae sesión para este día.</p>
      <div className={styles.restFigure}>
        <MuscleSilhouette className={styles.restBody} sex={anatomySex} variant="rest" />
      </div>
      {movable.length > 0 && (
        <label className={styles.moveField}>
          <span className={styles.moveLabel}>Mover una sesión a este día</span>
          <select
            className={styles.moveSelect}
            value=""
            disabled={pendingId !== undefined}
            onChange={(event) => move(event.target.value, day.dayOfWeek as DayOfWeek)}
          >
            <option value="" disabled>
              Elige una sesión
            </option>
            {movable.map(({ session, from }) => (
              <option key={session.id} value={session.id}>
                {stripKindPrefix(session.title)} — {DAY_LABELS[from] ?? from}
              </option>
            ))}
          </select>
        </label>
      )}
    </div>
  );
}

/**
 * The body illustration on today's card.
 *
 * <p>A strength session shows **both sheets**, front and back, because the
 * muscles it works do not respect the split: a pull day hits the lats and the
 * triceps on the back sheet and the biceps on the front one, and drawing only
 * the session's own `bodyView` would hide half of what it trains. The two
 * silhouettes share one overlay, and each renders the codes it can draw.
 *
 * <p>Running and rest have their own single whole-body art, so they show one
 * figure and no muscles — there is no muscle map behind them to show.
 */
function ExpandedFigures({
  session,
  sex,
}: {
  readonly session: TrainingSession;
  readonly sex: AnatomySex;
}) {
  const isStrength = session.kind === 'STRENGTH';
  // Only strength sessions have a muscle map; asking for a run's is a wasted
  // round trip that always answers empty.
  const muscles = useSessionMuscles(isStrength ? session.id : undefined);
  const overlay = overlayFromMuscleMap(muscles);
  const worked = Object.keys(overlay).length > 0;

  if (!isStrength) {
    return (
      <div className={styles.expandedFigures}>
        <MuscleSilhouette
          className={styles.expandedFigure}
          sex={sex}
          variant={session.kind === 'RUNNING' ? 'running' : 'rest'}
        />
      </div>
    );
  }

  return (
    <div className={styles.expandedFigures}>
      {/*
       * Labelled once for the pair rather than twice: to a screen reader this
       * is one illustration of one session, and announcing a front and a back
       * image separately would describe the layout instead of the content.
       * Without a muscle map there is nothing to announce at all, so it falls
       * back to being decorative.
       */}
      <div
        className={styles.expandedFigurePair}
        role={worked ? 'img' : undefined}
        aria-label={worked ? muscleSummary(muscles) : undefined}
      >
        <MuscleSilhouette
          className={styles.expandedFigure}
          sex={sex}
          view="front"
          muscles={overlay}
        />
        <MuscleSilhouette
          className={styles.expandedFigure}
          sex={sex}
          view="back"
          muscles={overlay}
        />
      </div>
    </div>
  );
}

/** "Músculos trabajados: Pecho, Tríceps, Hombro" — the same names the focus line prints. */
function muscleSummary(muscles: readonly MuscleWorked[]): string {
  const labels = groupMusclesForDisplay(muscles).map((muscle) => muscle.label);
  return `Músculos trabajados: ${labels.join(', ')}`;
}

/**
 * The muscle focus of a strength session, derived from its real FOR-136
 * muscle map rather than the fixed string this card used to print.
 *
 * <p>Renders nothing at all while loading, on failure, or when the session has
 * no muscle data: this is a one-line supporting detail on a card whose useful
 * parts (title, status, actions) do not depend on it, so a spinner or an error
 * row here would cost more attention than the line is worth. The full heatmap,
 * with its own loading and error states, lives in the session detail.
 */
function SessionFocus({ sessionId }: { readonly sessionId: string }) {
  // Shared with the silhouette overlay on this same card — see useSessionMuscles.
  const muscles = groupMusclesForDisplay(useSessionMuscles(sessionId));

  if (muscles.length === 0) return null;

  return (
    <p className={styles.sessionDetail}>
      Enfoque: {muscles.map((muscle) => muscle.label).join(', ')}
    </p>
  );
}

/*
 * The same words `StatusPill` prints, deliberately.
 *
 * <p>These label the compact days' status dots, and the expanded day beside
 * them carries a real `StatusPill`. While those two lived in different cards —
 * the dot in the calendar, the pill in the detail dialog — "Pendiente" here and
 * "Planificado" there went unnoticed. Side by side in one strip they read as
 * two different states, so this map follows the shared component rather than
 * keeping a second vocabulary for the same enum.
 */
const SESSION_STATUS_LABELS: Record<SessionStatus, string> = {
  PLANNED: 'Planificado',
  COMPLETED: 'Completado',
  SKIPPED: 'Saltado',
};

/**
 * The single status the day's dot shows.
 *
 * <p>A day usually holds one session, but it can hold two since sessions can be
 * moved onto the same day — so the dot has to say something about a set, not
 * just read one status off. Completed only when every session is: a day with
 * one done and one still to do is not a done day. Skipped only when nothing is
 * left pending, so the amber never hides outstanding work.
 */
function dayStatus(day: TrainingDay): SessionStatus {
  const sessions = day.sessions;
  if (sessions.length > 0 && sessions.every((session) => session.status === 'COMPLETED')) {
    return 'COMPLETED';
  }
  if (
    sessions.some((session) => session.status === 'SKIPPED') &&
    !sessions.some((session) => session.status === 'PLANNED')
  ) {
    return 'SKIPPED';
  }
  return 'PLANNED';
}

/**
 * The body drawn on a calendar card: one silhouette, always front for strength.
 *
 * <p>Only the front sheet, unlike the today card's front/back pair — at this
 * size two bodies would each get half the width and neither would read. The
 * cost is honest and worth naming: a pull day's lats and rear delts live on the
 * back sheet, so this card lights the biceps and leaves the rest dark. The
 * detail view is where the full pair is.
 *
 * <p>Running and rest carry their own whole-body art with the worked muscles
 * already drawn into the asset, so they need no overlay and no request.
 */
function DayFigure({
  session,
  sex,
}: {
  readonly session: TrainingSession;
  readonly sex: AnatomySex;
}) {
  const isStrength = session.kind === 'STRENGTH';
  const muscles = useSessionMuscles(isStrength ? session.id : undefined);

  if (!isStrength) {
    return <MuscleSilhouette className={styles.compactFigure} sex={sex} variant="running" />;
  }

  return (
    <MuscleSilhouette
      className={styles.compactFigure}
      sex={sex}
      view="front"
      muscles={overlayFromMuscleMap(muscles)}
    />
  );
}

/**
 * The week in numbers, as one strip under the days.
 *
 * <p>It replaces three blocks that all counted the same six sessions — "Resumen
 * semanal", the "Sesiones completadas" tile and "Distribución semanal" — plus
 * the streak card. The page used to print {@code 1 / 6} in three places at
 * three sizes, which is three chances to disagree and three rows of height.
 *
 * <p>Volumen, duración and calorías are gone rather than moved: they were fixed
 * strings in this file, and no training endpoint returns any of them.
 */
function WeekStats({ days }: { readonly days: readonly TrainingDay[] }) {
  const sessions = days.flatMap((day) => day.sessions);
  const total = tally(sessions);
  const running = tally(sessions.filter((session) => session.kind === 'RUNNING'));
  const strength = tally(sessions.filter((session) => session.kind === 'STRENGTH'));

  return (
    <section className={styles.weekStats} aria-label="Resumen de la semana">
      <StatTile label="Sesiones" icon="calendar" t={total} />
      <StatTile label="Carreras" icon="activity" t={running} />
      <StatTile label="Fuerza" icon="training" t={strength} tone="strength" />
      <StreakTile />
      <DistributionTile days={days} />
    </section>
  );
}

function StatTile({
  label,
  icon,
  t,
  tone,
}: {
  readonly label: string;
  readonly icon: IconName;
  readonly t: { completed: number; planned: number };
  readonly tone?: 'strength';
}) {
  return (
    <div className={styles.statTile}>
      <h2 className={styles.statLabel}>
        <Icon name={icon} size={16} className={styles.statIcon} data-tone={tone} />
        {label}
      </h2>
      <p className={styles.statValue}>{`${t.completed} / ${t.planned}`}</p>
    </div>
  );
}

/**
 * Split of the week by kind, as the donut the "Distribución semanal" card used
 * to hold. Display aggregation only (ADR-006), computed from the FOR-26 week.
 */
function DistributionTile({ days }: { readonly days: readonly TrainingDay[] }) {
  const sessions = days.flatMap((day) => day.sessions);
  const strength = sessions.filter((session) => session.kind === 'STRENGTH').length;
  const running = sessions.filter((session) => session.kind === 'RUNNING').length;
  const restDays = days.filter((day) => day.rest).length;
  const parts = strength + running + restDays;
  const pct = (n: number) => (parts > 0 ? Math.round((n / parts) * 100) : 0);
  const strengthDeg = parts > 0 ? (strength / parts) * 360 : 0;
  const runningDeg = parts > 0 ? (running / parts) * 360 : 0;

  const legend = [
    { key: 'strength', label: 'Fuerza', count: strength, className: styles.dotStrength },
    { key: 'running', label: 'Carreras', count: running, className: styles.dotRunning },
    { key: 'rest', label: 'Descanso', count: restDays, className: styles.dotRest },
  ];

  return (
    <div className={styles.distributionTile}>
      <div
        className={styles.distributionRing}
        style={{
          background: `conic-gradient(var(--color-warning-graphic) 0deg ${strengthDeg}deg, var(--color-accent) ${strengthDeg}deg ${
            strengthDeg + runningDeg
          }deg, var(--color-border) ${strengthDeg + runningDeg}deg 360deg)`,
        }}
        role="img"
        aria-label={`Distribución semanal: ${strength} de fuerza, ${running} de carrera, ${restDays} de descanso`}
      >
        <div className={styles.distributionHole} aria-hidden="true" />
      </div>
      <ul className={styles.distributionLegend}>
        {legend.map((item) => (
          <li key={item.key} className={styles.distributionItem}>
            <span className={`${styles.distributionDot} ${item.className}`} aria-hidden="true" />
            <span className={styles.distributionLabel}>{item.label}</span>
            <span className={styles.distributionPercent}>{pct(item.count)}%</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

type StreakState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly streak: Streak };

const STREAK_ERROR = 'No se pudo cargar tu racha. Inténtalo de nuevo.';

/**
 * "Racha actual" widget (FOR-143, mockup docs/3-entrenamiento.png), wired to
 * the FOR-139 {@code GET /api/v1/progress/streak} endpoint. Fetches
 * independently of the training week (FOR-60 pattern, mirroring
 * `ProgressPage`'s `InsightsSection`) so a streak failure never blocks the
 * rest of the page. The streak is a **nutrition meal-log** consistency
 * signal, not a training one (see this file's top doc comment) — rendered
 * exactly as the backend returns it, including a zero streak, which is a
 * normal state, not an error (ui-guidelines.md "no manipulative streaks": no
 * urgency copy, just the two numbers).
 */
/**
 * "Racha" as a tile in the week strip (FOR-143), wired to the FOR-139
 * {@code GET /api/v1/progress/streak} endpoint. Fetches independently of the
 * training week (FOR-60 pattern) so a streak failure never blocks the page.
 *
 * <p>The streak is a **nutrition meal-log** consistency signal, not a training
 * one (see this file's top doc comment). A zero streak is a normal state, not
 * an error (ui-guidelines.md "no manipulative streaks": no urgency copy). The
 * card this replaced also cheered — "¡Sigue así!" — which the same guideline
 * argues against and which a tile has no room for anyway; the record stays, as
 * the caption.
 */
function StreakTile() {
  const [state, setState] = useState<StreakState>({ status: 'loading' });
  const [reloadToken, setReloadToken] = useState(0);

  useEffect(() => {
    let active = true;
    setState({ status: 'loading' });
    getStreak()
      .then((streak) => {
        if (active) {
          setState({ status: 'ready', streak });
        }
      })
      .catch(() => {
        if (active) {
          setState({ status: 'error' });
        }
      });
    return () => {
      active = false;
    };
  }, [reloadToken]);

  return (
    <div className={styles.statTile}>
      <h2 className={styles.statLabel}>
        <Icon name="clock" size={16} className={styles.statIcon} data-tone="streak" />
        Racha
      </h2>
      {state.status === 'loading' && <WidgetLoading label="Cargando racha…" rows={1} />}
      {state.status === 'error' && (
        <p className={styles.statError} role="alert">
          {STREAK_ERROR}{' '}
          <button
            type="button"
            className={styles.statRetry}
            onClick={() => setReloadToken((n) => n + 1)}
          >
            Reintentar
          </button>
        </p>
      )}
      {state.status === 'ready' && (
        <>
          <p className={styles.statValue}>
            {state.streak.currentStreakDays}{' '}
            <span className={styles.statUnit}>
              {state.streak.currentStreakDays === 1 ? 'día' : 'días'}
            </span>
          </p>
          <p className={styles.statCaption}>
            Récord: {state.streak.longestStreakDays}{' '}
            {state.streak.longestStreakDays === 1 ? 'día' : 'días'}
          </p>
        </>
      )}
    </div>
  );
}

/**
 * The session detail dialog, redesigned in the "dos columnas" direction: the
 * muscle silhouettes and their legend on the left, the session's identity,
 * exercise breakdown and actions on the right. Design canvas:
 * `docs/design/modal-detalle-entrenamiento`.
 *
 * <p>Only a strength session earns the wide two-column panel — it is the one
 * that has a body to draw and a breakdown to list. A run keeps the 32rem
 * dialog every other modal uses, because widening a panel that holds three
 * lines of text just spreads them out.
 */
function SessionDetailModal({
  target,
  onClose,
  mark,
  move,
  pending,
  anatomySex,
}: {
  readonly target: DetailTarget;
  readonly onClose: () => void;
  readonly mark: (id: string, status: SessionStatus) => void;
  readonly move: (id: string, day: DayOfWeek) => void;
  readonly pending: boolean;
  readonly anatomySex: AnatomySex;
}) {
  const { dayOfWeek, session } = target;
  const strength = session.kind === 'STRENGTH';

  return (
    <Modal
      title={`${DAY_LABELS[dayOfWeek] ?? dayOfWeek} · ${KIND_LABELS[session.kind]}`}
      onClose={onClose}
      size={strength ? 'lg' : 'md'}
    >
      <div className={strength ? styles.detailLayout : styles.detailSingle}>
        {strength && <MuscleMapSection sessionId={session.id} sex={anatomySex} />}

        <div className={styles.detailMain}>
          {/* The dialog title already carries the day and the kind ("Martes ·
              Fuerza"), so the body prints only what it does not: which strength
              session this is, its status, and how many exercises it holds. */}
          <div className={styles.detailHeading}>
            <p className={styles.detailTitle}>{displayTitle(session.title)}</p>
            <div className={styles.detailMeta}>
              <StatusPill kind="training" value={session.status} />
              <span className={styles.sessionDetail}>{session.detail}</span>
            </div>
          </div>

          {session.notes && <p className={styles.notes}>{session.notes}</p>}

          {strength && <ExerciseBreakdown workoutType={session.workoutType} />}

          {/* Sticky: the panel caps at 90vh and a five-exercise breakdown
              scrolls past it on a phone, so the two actions the dialog exists
              for stay within a thumb's reach instead of below the fold. */}
          <div className={styles.detailFooter}>
            {/* Moving one session is the whole primitive: swapping two days is two
                moves and reordering the week is several, so there is no rule here
                for a session pushed past Sunday — the user decides where each one
                lands. The override lasts this week only. */}
            <label className={styles.detailMoveField}>
              <span className={styles.moveLabel}>Mover a otro día</span>
              <select
                className={styles.moveSelect}
                value={dayOfWeek}
                disabled={pending}
                onChange={(event) => move(session.id, event.target.value as DayOfWeek)}
              >
                {WEEK_ORDER.map((day) => (
                  <option key={day} value={day}>
                    {DAY_LABELS[day] ?? day}
                  </option>
                ))}
              </select>
            </label>
            {/* "Saltar" first, "Completar" last: the row is right-aligned, so
                DOM order puts the primary action nearest the corner the thumb
                and the eye both end on — and tab order reaches it last. */}
            <div className={styles.actions}>
              {session.status !== 'SKIPPED' && (
                <Button
                  type="button"
                  variant="secondary"
                  disabled={pending}
                  onClick={() => mark(session.id, 'SKIPPED')}
                >
                  Saltar
                </Button>
              )}
              {session.status !== 'COMPLETED' && (
                <Button
                  type="button"
                  disabled={pending}
                  loading={pending}
                  onClick={() => mark(session.id, 'COMPLETED')}
                >
                  Completar
                </Button>
              )}
            </div>
          </div>
        </div>
      </div>
    </Modal>
  );
}

/** A legend group set, split by the sheet that draws it. */
interface MusclesByView {
  readonly front: readonly MuscleGroupDisplay[];
  readonly back: readonly MuscleGroupDisplay[];
  readonly unplaced: readonly MuscleGroupDisplay[];
}

/**
 * Partitions the display groups into the two silhouettes' columns, keeping the
 * groups the pack cannot place in a third bucket rather than forcing them into
 * a column — a muscle listed under the wrong body is worse than one listed
 * under neither. Preserves the incoming order inside each bucket, which is the
 * catalog's own first-appearance order.
 */
function splitMusclesByView(muscles: readonly MuscleGroupDisplay[]): MusclesByView {
  const front: MuscleGroupDisplay[] = [];
  const back: MuscleGroupDisplay[] = [];
  const unplaced: MuscleGroupDisplay[] = [];

  for (const muscle of muscles) {
    const view = viewForMuscle(muscle.canonical);
    if (view === 'front') front.push(muscle);
    else if (view === 'back') back.push(muscle);
    else unplaced.push(muscle);
  }

  return { front, back, unplaced };
}

/**
 * One labelled column of the legend. Renders nothing when the session works
 * none of that sheet's muscles: a "Frente" heading over an empty column is
 * noise, and the asymmetry itself says something true (a pull day is mostly
 * back).
 */
function MuscleLegendColumn({
  title,
  muscles,
}: {
  readonly title: string;
  readonly muscles: readonly MuscleGroupDisplay[];
}) {
  if (muscles.length === 0) return null;

  return (
    <div className={styles.muscleLegendColumn}>
      {/* The columns are labelled on purpose: two unlabelled ones read as a
          list that happened to wrap, which teaches the reader nothing about
          why a muscle sits where it does. */}
      <h4 className={styles.muscleLegendTitle}>{title}</h4>
      <ul className={styles.muscleLegendList} aria-label={title}>
        {muscles.map((muscle) => (
          <MuscleLegendItem key={muscle.canonical} muscle={muscle} />
        ))}
      </ul>
    </div>
  );
}

/** One legend row: the swatch that keys the picture, the name and the load. */
function MuscleLegendItem({ muscle }: { readonly muscle: MuscleGroupDisplay }) {
  return (
    <li className={styles.muscleLegendItem}>
      <span className={styles.muscleDot} data-role={roleForLoad(muscle.load)} aria-hidden="true" />
      <span className={styles.muscleLegendText}>
        <span className={styles.muscleLegendName}>{muscle.label}</span>
        <span className={styles.muscleLegendLoad}>{statusLabel('muscleLoad', muscle.load)}</span>
      </span>
    </li>
  );
}

type WorkoutState =
  | { readonly status: 'unavailable' }
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly items: readonly WorkoutItem[] };

const WORKOUT_ERROR = 'No se pudieron cargar los ejercicios de esta sesión.';
const WORKOUT_UNAVAILABLE = 'Este entrenamiento no tiene desglose por ejercicio.';

/**
 * The real exercise prescription for a strength session, replacing the notice
 * that used to apologise for its absence.
 *
 * <p>That notice ("la API no expone las plantillas de fuerza por HTTP") had
 * been false since `GET /api/v1/training/workouts/{type}` shipped — the
 * "Entrenar" page has been calling it all along. This section is the same
 * endpoint, summarised: one line per exercise instead of an editable set table,
 * because this dialog is for deciding whether to do the session, not for
 * logging it.
 *
 * <p>Fetched independently of the muscle map so neither request can take the
 * other's section down, matching the page's existing "error, prior state
 * preserved" pattern.
 */
function ExerciseBreakdown({ workoutType }: { readonly workoutType?: string }) {
  const [state, setState] = useState<WorkoutState>({ status: 'loading' });

  useEffect(() => {
    if (!workoutType) {
      // Not an error: the read model marks some strength sessions as having no
      // template to look up, and a spinner that never resolves is worse than
      // saying so.
      setState({ status: 'unavailable' });
      return;
    }
    let cancelled = false;
    setState({ status: 'loading' });
    getWorkout(workoutType)
      .then((workout) => {
        if (!cancelled) setState({ status: 'ready', items: workout.items });
      })
      .catch(() => {
        if (!cancelled) setState({ status: 'error' });
      });
    return () => {
      cancelled = true;
    };
  }, [workoutType]);

  return (
    <div className={styles.exerciseSection}>
      {state.status === 'loading' && <WidgetLoading label="Cargando ejercicios…" rows={3} />}
      {state.status === 'error' && (
        <p className={styles.detailError} role="alert">
          {WORKOUT_ERROR}
        </p>
      )}
      {state.status === 'unavailable' && <p className={styles.message}>{WORKOUT_UNAVAILABLE}</p>}
      {state.status === 'ready' &&
        (state.items.length === 0 ? (
          <p className={styles.message}>{WORKOUT_UNAVAILABLE}</p>
        ) : (
          /* No visible heading: the session's "5 ejercicios" subtitle already
             names this list. The label keeps it announced all the same. */
          <ol className={styles.exerciseList} aria-label="Ejercicios">
            {state.items.map((item, index) => (
              <li key={item.exerciseId} className={styles.exerciseItem}>
                <span className={styles.exerciseOrder} aria-hidden="true">
                  {index + 1}
                </span>
                <span className={styles.exerciseLines}>
                  <span className={styles.exerciseName}>{item.exerciseName}</span>
                  <span className={styles.exercisePrescription}>{prescriptionSummary(item)}</span>
                </span>
              </li>
            ))}
          </ol>
        ))}
    </div>
  );
}

type MuscleMapState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | {
      readonly status: 'ready';
      readonly muscles: readonly MuscleGroupDisplay[];
      readonly raw: readonly MuscleWorked[];
    };

const MUSCLE_MAP_ERROR = 'No se pudieron cargar los músculos trabajados.';

/**
 * The FOR-136 worked-muscle heatmap for a strength session (spec FOR-53: "now
 * backed by FOR-136 — wire the heatmap to the muscle-map endpoint"), drawn as
 * the two anatomical sheets with the worked muscles lit rather than as a row of
 * chips. A push session works the triceps, which live on the back sheet, so
 * both views are shown: one sheet would silently drop muscles the legend names.
 *
 * <p>The legend is not decoration — it is the key to the picture, so each
 * swatch takes its emphasis from {@link roleForLoad}, the same rule that drives
 * the overlay's opacity. A legend that disagreed with the body beside it would
 * be worse than none.
 *
 * <p>Fetched on demand per session (the endpoint is per-session, not part of
 * the FOR-26 week payload) and normalized for display via
 * `trainingMuscleLabels` (frontend-owned grouping — the backend read model
 * itself stays untouched). A load failure here is scoped to this section only:
 * the rest of the session detail (status, breakdown, actions) keeps working,
 * matching the page's existing "error, prior state preserved" pattern.
 */
function MuscleMapSection({
  sessionId,
  sex,
}: {
  readonly sessionId: string;
  readonly sex: AnatomySex;
}) {
  const [state, setState] = useState<MuscleMapState>({ status: 'loading' });

  useEffect(() => {
    let cancelled = false;
    setState({ status: 'loading' });
    getMuscleMap(sessionId)
      .then((map) => {
        if (!cancelled) {
          setState({
            status: 'ready',
            muscles: groupMusclesForDisplay(map.muscles),
            raw: map.muscles,
          });
        }
      })
      .catch(() => {
        if (!cancelled) {
          setState({ status: 'error' });
        }
      });
    return () => {
      cancelled = true;
    };
  }, [sessionId]);

  const worked = state.status === 'ready' && state.muscles.length > 0;
  const byView = splitMusclesByView(state.status === 'ready' ? state.muscles : []);

  return (
    <aside className={styles.muscleColumn}>
      <h3 className={styles.detailSectionTitle}>Músculos trabajados</h3>
      {state.status === 'loading' && (
        <WidgetLoading label="Cargando músculos trabajados…" rows={2} />
      )}
      {state.status === 'error' && (
        <p className={styles.detailError} role="alert">
          {MUSCLE_MAP_ERROR}
        </p>
      )}
      {state.status === 'ready' && !worked && (
        <p className={styles.message}>Sin datos de músculos para esta sesión.</p>
      )}
      {state.status === 'ready' && worked && (
        <>
          <div className={styles.muscleFigures} role="img" aria-label={muscleSummary(state.raw)}>
            <MuscleSilhouette
              className={styles.muscleFigure}
              sex={sex}
              view="front"
              muscles={overlayFromMuscleMap(state.raw)}
            />
            <MuscleSilhouette
              className={styles.muscleFigure}
              sex={sex}
              view="back"
              muscles={overlayFromMuscleMap(state.raw)}
            />
          </div>
          <div className={styles.muscleLegend}>
            <MuscleLegendColumn title="Frente" muscles={byView.front} />
            <MuscleLegendColumn title="Espalda" muscles={byView.back} />
            {/* Worked, but on neither sheet — see `viewForMuscle`. Listed
                across the full width rather than dropped, because the muscle is
                still part of the session even when the pack cannot draw it. */}
            {byView.unplaced.length > 0 && (
              <ul className={styles.muscleLegendUnplaced}>
                {byView.unplaced.map((muscle) => (
                  <MuscleLegendItem key={muscle.canonical} muscle={muscle} />
                ))}
              </ul>
            )}
          </div>
        </>
      )}
    </aside>
  );
}
