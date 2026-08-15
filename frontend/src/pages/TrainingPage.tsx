import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Badge } from '../components/Badge';
import { BodyFigure } from '../components/BodyFigure';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { NoPlanEmptyState } from '../components/NoPlanEmptyState';
import { ErrorState } from '../components/ErrorState';
import { Icon, type IconName } from '../components/Icon';
import { LoadingState } from '../components/LoadingState';
import { MetricCard } from '../components/MetricCard';
import { Modal } from '../components/Modal';
import { useNotify } from '../components/NotificationProvider';
import { ProgressRing } from '../components/ProgressRing';
import { StatusPill } from '../components/StatusPill';
import { WidgetLoading } from '../components/WidgetLoading';
import { IconButton } from '../components/IconButton';
import { ApiRequestError } from '../api/client';
import { getStreak, type Streak } from '../api/progress';
import { getProfile } from '../api/profile';
import {
  getMuscleMap,
  getTrainingWeek,
  updateSessionStatus,
  type SessionStatus,
  type TrainingDay,
  type TrainingSession,
  type TrainingWeek,
} from '../api/training';
import { groupMusclesForDisplay, type MuscleGroupDisplay } from './trainingMuscleLabels';
import { formatShortDate, formatWeekday } from './dateLabel';
import styles from './TrainingPage.module.css';

/**
 * Training page (FOR-26/FOR-27, built out to the mockup by FOR-53):
 * `docs/3-entrenamiento.png` — today's session, a Monday-Sunday calendar, a
 * session detail view and a weekly summary, all read from the FOR-26 training
 * week API (`GET /api/v1/training/week`); completion is the FOR-27
 * `PATCH …/status` call. Renders the API read model directly (ADR-006); no
 * training rule (scheduling, progression) lives here.
 *
 * <p>Mockup elements not backed by any endpoint today (documented gap, not
 * invented — AGENTS.md "repository state has priority"):
 * <ul>
 *   <li>Per-exercise rows (series/reps/peso/descanso/estado) and per-exercise
 *       completion — the FOR-25 {@code WorkoutTemplateService} exists in the
 *       backend but is never wired to a controller, so the frontend only ever
 *       sees each session's plain {@code detail} summary string (e.g. "3
 *       ejercicios"). Shown as a labelled placeholder in the session detail
 *       view instead of a fabricated table.
 *   <li>"Calorías estimadas", "Volumen total" and "Duración total" tiles — no
 *       calories/volume/duration field exists anywhere in the training domain
 *       or API. The muscle-worked heatmap *is* backed (FOR-136, {@code GET
 *       …/sessions/{id}/muscle-map}) and is wired into the strength session
 *       detail below, normalized for display by {@code trainingMuscleLabels}
 *       (spec FOR-53: the frontend, not the backend, owns that
 *       normalization). "RACHA ACTUAL" is wired by FOR-143 to the FOR-139
 *       {@code GET …/progress/streak} endpoint. It is a real nutrition
 *       meal-log consistency signal, not a fabricated training streak.
 *   <li>Weekly summary counts (planned vs. completed sessions) are *not* the
 *       FOR-28 {@code WeeklyTrainingSummary} — that calculation is
 *       application-layer only and is not exposed over HTTP. This page tallies
 *       the sessions already returned by {@code GET /training/week}, exactly
 *       like the FOR-51 {@code TrainingWidget} does (see its doc comment).
 *   <li>Date navigation (prev/next day arrows) — `docs/api/training-week.md`
 *       states the composed week has "no dates, no week navigation"; only
 *       today's real calendar date is shown, read-only.
 *   <li>"Editar entrenamiento" — no endpoint mutates workout templates.
 * </ul>
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly week: TrainingWeek };

type AnatomySex = 'male' | 'female';

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

const KIND_LABELS: Record<TrainingSession['kind'], string> = {
  RUNNING: 'Carrera',
  STRENGTH: 'Fuerza',
};

const MARK_ERROR = 'No se pudo actualizar la sesión. Inténtalo de nuevo.';

/**
 * FOR-164 hybrid placeholders (`docs/3-entrenamiento-dash.png`). None of these
 * has a backing field in the training domain or API (verified: no volume /
 * duration / calories / per-exercise / estimated-duration / "focus" data
 * anywhere) — kept isolated here, clearly labelled, so they're obvious and
 * easy to rip out once endpoints exist. Real, backed data (session tallies,
 * distribution %, muscle map, streak, weekly history) is computed/fetched, not
 * taken from here.
 */
const PLACEHOLDER = {
  /*
   * `today` is gone: its fixed duration, muscle focus and "4 / 6 ejercicios"
   * were printed under every session regardless of kind, so a running day
   * announced a chest-and-triceps focus and an exercise count it does not
   * have. The card now shows the session's own `detail`, the real muscle map
   * for strength (see SessionFocus), and a session tally on the ring.
   */
  stats: {
    volume: '12.450',
    volumeDelta: '↑8% vs semana anterior',
    duration: '48:32',
    durationDelta: '↑5 min vs semana anterior',
    calories: '2.120',
    caloriesDelta: '↑12% vs semana anterior',
  },
} as const;

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
  const [anatomySex, setAnatomySex] = useState<AnatomySex>('male');
  const [actionError, setActionError] = useState<string | undefined>(undefined);
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

  useEffect(() => {
    let active = true;
    getProfile()
      .then((profile) => {
        if (active) setAnatomySex(profile.sex === 'FEMALE' ? 'female' : 'male');
      })
      .catch(() => {
        // The existing male presentation remains the safe fallback when the profile is unavailable.
      });
    return () => {
      active = false;
    };
  }, []);

  async function mark(sessionId: string, status: SessionStatus) {
    setActionError(undefined);
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
      setActionError(error instanceof ApiRequestError ? error.message : MARK_ERROR);
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

      {actionError && (
        <p className={styles.actionError} role="alert">
          {actionError}
        </p>
      )}

      {renderContent(
        state,
        mark,
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
          mark={mark}
          pending={pendingId === detailTarget.session.id}
        />
      )}
    </div>
  );
}

function renderContent(
  state: State,
  mark: (id: string, status: SessionStatus) => void,
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

  const selected = state.week.days.find((day) => day.dayOfWeek === selectedDay);

  return (
    <div className={styles.layout}>
      <div className={styles.todayArea}>
        <TodaySessionCard
          day={selected}
          dayOfWeek={selectedDay}
          mark={mark}
          pendingId={pendingId}
          openDetail={openDetail}
          openTraining={openTraining}
          anatomySex={anatomySex}
        />
      </div>
      <div className={styles.summaryArea}>
        <WeeklySummary days={state.week.days} />
      </div>
      <div className={styles.calendarArea}>
        <WeeklyCalendar days={state.week.days} openDetail={openDetail} anatomySex={anatomySex} />
      </div>
      <div className={styles.tabletPair}>
        <WeeklyDistribution days={state.week.days} />
        <StatsRow days={state.week.days} />
      </div>
      <div className={styles.streakArea}>
        <StreakCard />
      </div>
    </div>
  );
}

function TodaySessionCard({
  day,
  dayOfWeek,
  mark,
  pendingId,
  openDetail,
  openTraining,
  anatomySex,
}: {
  readonly day: TrainingDay | undefined;
  readonly dayOfWeek: string;
  readonly mark: (id: string, status: SessionStatus) => void;
  readonly pendingId: string | undefined;
  readonly openDetail: (target: DetailTarget) => void;
  readonly openTraining: (session: TrainingSession) => void;
  readonly anatomySex: AnatomySex;
}) {
  // "Entrenamiento de hoy" only while the card really is showing today; once
  // the arrows move it, the heading has to say which day is on screen.
  const isToday = dayOfWeek === todayDayOfWeek();
  const title = isToday
    ? 'Entrenamiento de hoy'
    : `Entrenamiento del ${(DAY_LABELS[dayOfWeek] ?? dayOfWeek).toLocaleLowerCase('es-ES')}`;
  const when = isToday ? 'hoy' : 'ese día';

  if (!day) {
    return (
      <Card title={title} headingLevel={2} className={styles.todayCard}>
        <p className={styles.message}>No hay datos de {when} en el plan de esta semana.</p>
      </Card>
    );
  }

  if (day.rest) {
    return (
      <Card title={title} headingLevel={2} className={styles.todayCard}>
        <p className={styles.rest}>
          {isToday ? 'Hoy es día de descanso.' : 'Ese día es de descanso.'}
        </p>
      </Card>
    );
  }

  const { completed, planned } = tally(day.sessions);
  const percent = planned > 0 ? Math.round((completed / planned) * 100) : 0;
  const visualSession =
    day.sessions.find((session) => session.kind === 'STRENGTH') ?? day.sessions[0];

  return (
    <Card title={title} headingLevel={2} className={styles.todayCard}>
      <div className={styles.todayLayout}>
        <ul className={styles.todaySessions}>
          {day.sessions.map((session) => (
            <li key={session.id} className={styles.todaySession}>
              <div className={styles.todaySessionHeader}>
                <p className={styles.todaySessionTitle}>{session.title}</p>
                <StatusPill kind="training" value={session.status} />
              </div>
              {/* Only what the session really carries. A fixed duration and a
                  fixed muscle focus used to print under every session, which
                  is how a *run* came to announce a chest-and-triceps focus —
                  neither field exists anywhere in the training API. The focus
                  of a strength session is derived below from its real
                  FOR-136 muscle map. */}
              <p className={styles.sessionDetail}>{session.detail}</p>
              {session.kind === 'STRENGTH' && <SessionFocus sessionId={session.id} />}
              <div className={styles.actions}>
                {session.kind === 'STRENGTH' ? (
                  <Button type="button" onClick={() => openTraining(session)}>
                    <Icon name="arrowRight" size={17} />
                    {session.status === 'COMPLETED' ? 'Ver entrenamiento' : 'Iniciar entrenamiento'}
                  </Button>
                ) : session.status === 'COMPLETED' ? (
                  /* A run has no per-exercise screen to open, so its action
                     marks completion in place — and undoes it, or a mistaken
                     tap would be permanent. Back to PLANNED, not SKIPPED:
                     undoing means "this did not happen yet", and SKIPPED is a
                     deliberate different statement. */
                  <Button
                    type="button"
                    variant="secondary"
                    aria-label="Desmarcar la carrera como completada"
                    disabled={pendingId === session.id}
                    loading={pendingId === session.id}
                    onClick={() => mark(session.id, 'PLANNED')}
                  >
                    <Icon name="checkCircle" size={17} />
                    Completada
                  </Button>
                ) : (
                  <Button
                    type="button"
                    disabled={pendingId === session.id}
                    loading={pendingId === session.id}
                    onClick={() => mark(session.id, 'COMPLETED')}
                  >
                    <Icon name="check" size={17} />
                    Completar carrera
                  </Button>
                )}
                {session.status === 'PLANNED' && (
                  <Button
                    type="button"
                    variant="secondary"
                    disabled={pendingId === session.id}
                    onClick={() => mark(session.id, 'SKIPPED')}
                  >
                    Saltar
                  </Button>
                )}
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => openDetail({ dayOfWeek: day.dayOfWeek, session })}
                >
                  <Icon name="menu" size={17} />
                  Ver detalle
                </Button>
              </div>
            </li>
          ))}
        </ul>

        <div className={styles.todayVisual}>
          <div className={styles.todayRing}>
            {/* Ring shows today's real session completion; the "N/M ejercicios"
                figure below it is placeholder (per-exercise data isn't backed). */}
            <ProgressRing
              value={completed}
              max={Math.max(planned, 1)}
              label={`${completed} de ${planned} sesiones completadas hoy`}
              size={128}
            >
              <span className={styles.ringPercent}>{percent}%</span>
            </ProgressRing>
            <p className={styles.ringStatus}>{percent === 100 ? 'Completado' : 'En progreso'}</p>
            {/* The ring counts sessions, which is what the week payload
                actually carries. It used to be captioned "4 / 6 ejercicios"
                from a constant — a figure that was wrong for every day and
                meaningless for a run. */}
            <p className={styles.ringCaption}>
              {completed} / {planned} {planned === 1 ? 'sesión' : 'sesiones'}
            </p>
          </div>
          <div className={styles.todayFigures}>
            <BodyFigure
              view={visualSession.bodyView.toLowerCase() as 'front' | 'back'}
              variant={visualSession.kind === 'RUNNING' ? 'running' : 'strength'}
              sex={anatomySex}
              active={visualSession.status === 'COMPLETED'}
              size={150}
            />
          </div>
        </div>
      </div>
    </Card>
  );
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
  const [muscles, setMuscles] = useState<readonly MuscleGroupDisplay[]>([]);

  useEffect(() => {
    let cancelled = false;
    setMuscles([]);
    getMuscleMap(sessionId)
      .then((map) => {
        if (!cancelled) setMuscles(groupMusclesForDisplay(map.muscles));
      })
      .catch(() => {
        if (!cancelled) setMuscles([]);
      });
    return () => {
      cancelled = true;
    };
  }, [sessionId]);

  if (muscles.length === 0) return null;

  return (
    <p className={styles.sessionDetail}>
      Enfoque: {muscles.map((muscle) => muscle.label).join(', ')}
    </p>
  );
}

function WeeklyCalendar({
  days,
  openDetail,
  anatomySex,
}: {
  readonly days: readonly TrainingDay[];
  readonly openDetail: (target: DetailTarget) => void;
  readonly anatomySex: AnatomySex;
}) {
  const todayEnum = todayDayOfWeek();
  const todayRef = useRef<HTMLLIElement>(null);

  useEffect(() => {
    const centerToday = () => {
      const today = todayRef.current;
      const calendar = today?.parentElement;

      if (!today || !calendar || calendar.scrollWidth <= calendar.clientWidth) return;

      today.scrollIntoView({ behavior: 'instant', inline: 'center', block: 'nearest' });
    };

    const frame = requestAnimationFrame(centerToday);
    const calendar = todayRef.current?.parentElement;
    const images = [...(calendar?.querySelectorAll('img') ?? [])];
    images.forEach((image) => image.addEventListener('load', centerToday, { once: true }));

    return () => {
      cancelAnimationFrame(frame);
      images.forEach((image) => image.removeEventListener('load', centerToday));
    };
  }, []);

  return (
    <Card title="Calendario semanal" headingLevel={2}>
      <ul className={styles.calendarGrid} aria-label="Calendario semanal de entrenamiento">
        {days.map((day) => (
          <li
            key={day.dayOfWeek}
            ref={day.dayOfWeek === todayEnum ? todayRef : undefined}
            aria-current={day.dayOfWeek === todayEnum ? 'date' : undefined}
            className={[styles.calendarDay, day.dayOfWeek === todayEnum ? styles.calendarToday : '']
              .filter(Boolean)
              .join(' ')}
          >
            <h3 className={styles.calendarDayTitle}>
              {DAY_LABELS[day.dayOfWeek] ?? day.dayOfWeek}
            </h3>
            {day.rest ? (
              <div className={styles.calendarRest}>
                <Badge tone="neutral">Descanso</Badge>
                <BodyFigure variant="rest" size={72} />
              </div>
            ) : (
              <ul className={styles.calendarSessions}>
                {day.sessions.map((session) => (
                  <li key={session.id}>
                    <button
                      type="button"
                      className={styles.calendarSessionButton}
                      onClick={() => openDetail({ dayOfWeek: day.dayOfWeek, session })}
                    >
                      <Badge tone={session.kind === 'RUNNING' ? 'accent' : 'violet'}>
                        {KIND_LABELS[session.kind]}
                      </Badge>
                      <span className={styles.calendarSessionTitle}>
                        {stripKindPrefix(session.title)}
                      </span>
                      <BodyFigure
                        view={session.bodyView.toLowerCase() as 'front' | 'back'}
                        sex={anatomySex}
                        variant={session.kind === 'RUNNING' ? 'running' : 'strength'}
                        active={session.status === 'COMPLETED'}
                        size={64}
                      />
                      <StatusPill kind="training" value={session.status} />
                      <span
                        className={styles.calendarSessionProgress}
                        role="progressbar"
                        aria-label={`Progreso de ${session.title}`}
                        aria-valuemin={0}
                        aria-valuemax={100}
                        aria-valuenow={session.status === 'COMPLETED' ? 100 : 0}
                      >
                        <span style={{ width: session.status === 'COMPLETED' ? '100%' : '0%' }} />
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </li>
        ))}
      </ul>
      <div className={styles.calendarTimeline} role="img" aria-label="Progreso semanal">
        {days.map((day) => {
          const completed =
            !day.rest && day.sessions.some((session) => session.status === 'COMPLETED');
          return (
            <span
              key={day.dayOfWeek}
              className={[
                styles.calendarTimelineDot,
                completed ? styles.calendarTimelineDone : '',
                day.dayOfWeek === todayEnum ? styles.calendarTimelineToday : '',
              ]
                .filter(Boolean)
                .join(' ')}
            />
          );
        })}
      </div>
      {/* Two axes in one legend, as the mockup has it: the timeline's statuses
          and the badge/figure colour that tells the two session kinds apart.
          "Pendiente" stays even though the mockup's fully-completed week never
          shows it — the grid does render that state. */}
      <ul className={styles.calendarLegend} aria-label="Leyenda del calendario">
        <li>
          <span className={`${styles.legendDot} ${styles.legendDone}`} aria-hidden="true" />{' '}
          Completado
        </li>
        <li>
          <span className={`${styles.legendDot} ${styles.legendToday}`} aria-hidden="true" /> Hoy
        </li>
        <li>
          <span className={`${styles.legendDot} ${styles.legendPending}`} aria-hidden="true" />{' '}
          Pendiente
        </li>
        <li>
          <span className={`${styles.legendDot} ${styles.legendStrength}`} aria-hidden="true" />{' '}
          Fuerza
        </li>
        <li>
          <span className={`${styles.legendDot} ${styles.legendRunning}`} aria-hidden="true" />{' '}
          Carrera
        </li>
        <li>
          <span className={`${styles.legendDot} ${styles.legendRest}`} aria-hidden="true" />{' '}
          Descanso
        </li>
      </ul>
    </Card>
  );
}

function summaryPercent({ completed, planned }: { completed: number; planned: number }): number {
  return planned > 0 ? Math.round((completed / planned) * 100) : 0;
}

function WeeklySummary({ days }: { readonly days: readonly TrainingDay[] }) {
  const sessions = days.flatMap((day) => day.sessions);
  const total = tally(sessions);
  const runningTally = tally(sessions.filter((s) => s.kind === 'RUNNING'));
  const strengthTally = tally(sessions.filter((s) => s.kind === 'STRENGTH'));

  const rows: {
    label: string;
    caption: string;
    icon: IconName;
    t: { completed: number; planned: number };
  }[] = [
    { label: 'Sesiones totales', caption: 'Sesiones completadas', icon: 'calendar', t: total },
    // The row's own heading already names the kind, so the caption only says
    // what is being counted rather than repeating it.
    { label: 'Carreras', caption: 'Completadas', icon: 'activity', t: runningTally },
    { label: 'Fuerza', caption: 'Completadas', icon: 'training', t: strengthTally },
  ];

  return (
    <Card
      title="Resumen semanal"
      headingLevel={2}
      action={<Icon name="activity" className={styles.summaryTitleIcon} size={24} />}
      className={styles.summary}
    >
      <ul className={styles.summaryList}>
        {rows.map((row) => (
          <li key={row.label} className={styles.summaryRow} aria-label={row.label}>
            {/* Violet marks the strength row here the same way it marks the
                strength badge in the calendar and the muscle tags on the
                detail screen. */}
            <span
              className={styles.summaryIcon}
              data-kind={row.icon === 'training' ? 'strength' : 'default'}
              aria-hidden="true"
            >
              <Icon name={row.icon} size={28} />
            </span>
            <div className={styles.summaryContent}>
              <h3 className={styles.summaryLabel}>{row.label}</h3>
              <p className={styles.summaryValue}>{`${row.t.completed} / ${row.t.planned}`}</p>
              <p className={styles.summaryCaption}>{row.caption}</p>
            </div>
            <div className={styles.summaryProgress}>
              <ProgressRing
                value={row.t.completed}
                max={Math.max(row.t.planned, 1)}
                label={`${row.label}: ${row.t.completed} de ${row.t.planned}`}
                size={72}
              >
                <span className={styles.summaryRingText}>{summaryPercent(row.t)}%</span>
              </ProgressRing>
            </div>
          </li>
        ))}
      </ul>
      <Link className={styles.summaryLink} to="/app/progress">
        <Icon name="progress" size={18} />
        <span>Ver estadísticas completas</span>
        <Icon name="arrowRight" size={17} />
      </Link>
    </Card>
  );
}

/**
 * "Distribución semanal" donut (FOR-164 mockup). Real split of this week's
 * sessions by kind (strength / running) plus rest days, computed from the
 * FOR-26 week — display aggregation only (ADR-006), not the FOR-28
 * `WeeklyTrainingSummary`. The "balance" note is a small static heuristic on
 * the real ratio, not a backend signal.
 */
function WeeklyDistribution({ days }: { readonly days: readonly TrainingDay[] }) {
  const sessions = days.flatMap((day) => day.sessions);
  const strength = sessions.filter((s) => s.kind === 'STRENGTH').length;
  const running = sessions.filter((s) => s.kind === 'RUNNING').length;
  const restDays = days.filter((day) => day.rest).length;
  const totalParts = strength + running + restDays;
  const pct = (n: number) => (totalParts > 0 ? Math.round((n / totalParts) * 100) : 0);
  const strengthDeg = totalParts > 0 ? (strength / totalParts) * 360 : 0;
  const runningDeg = totalParts > 0 ? (running / totalParts) * 360 : 0;

  const ringStyle = {
    background: `conic-gradient(var(--color-warning-graphic) 0deg ${strengthDeg}deg, var(--color-accent) ${strengthDeg}deg ${
      strengthDeg + runningDeg
    }deg, var(--color-border) ${strengthDeg + runningDeg}deg 360deg)`,
  };

  const legend = [
    {
      key: 'strength',
      label: 'Fuerza',
      count: strength,
      unit: 'sesiones',
      className: styles.dotStrength,
    },
    {
      key: 'running',
      label: 'Carreras',
      count: running,
      unit: 'sesiones',
      className: styles.dotRunning,
    },
    { key: 'rest', label: 'Descanso', count: restDays, unit: 'días', className: styles.dotRest },
  ];
  const balanced = strength > 0 && running > 0;

  return (
    <Card title="Distribución semanal" headingLevel={2}>
      <div className={styles.distribution}>
        <div
          className={styles.distributionRing}
          style={ringStyle}
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
              <span className={styles.distributionCount}>
                {item.count}{' '}
                {item.unit === 'días'
                  ? item.count === 1
                    ? 'día'
                    : 'días'
                  : item.count === 1
                    ? 'sesión'
                    : 'sesiones'}
              </span>
            </li>
          ))}
        </ul>
      </div>
      <p className={styles.distributionNote}>
        <Icon name="check" size={16} className={styles.distributionNoteIcon} />
        {balanced
          ? 'Equilibrio adecuado. Buen balance entre fuerza y cardio.'
          : 'Añade variedad para equilibrar fuerza y cardio.'}
      </p>
    </Card>
  );
}

/**
 * "Estadísticas de la semana" tiles (FOR-164 mockup): SESIONES COMPLETADAS is
 * the real week tally; VOLUMEN / DURACIÓN / CALORÍAS are isolated placeholders
 * (no such fields exist in the training domain or API — see {@link PLACEHOLDER}).
 */
function StatsRow({ days }: { readonly days: readonly TrainingDay[] }) {
  const total = tally(days.flatMap((day) => day.sessions));
  return (
    <div className={styles.statsRow}>
      <MetricCard
        label="Volumen total"
        icon="training"
        value={PLACEHOLDER.stats.volume}
        unit="kg"
        caption={PLACEHOLDER.stats.volumeDelta}
      />
      <MetricCard
        label="Duración total"
        icon="activity"
        value={PLACEHOLDER.stats.duration}
        unit="min"
        caption={PLACEHOLDER.stats.durationDelta}
      />
      <MetricCard
        label="Sesiones completadas"
        icon="check"
        value={`${total.completed} / ${total.planned}`}
        caption="Esta semana"
      />
      <MetricCard
        label="Calorías estimadas"
        icon="heart"
        value={PLACEHOLDER.stats.calories}
        unit="kcal"
        caption={PLACEHOLDER.stats.caloriesDelta}
      />
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
function StreakCard() {
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
    <Card title="Racha actual" headingLevel={2}>
      <p className={styles.widgetCaption}>Días consecutivos con registro de nutrición.</p>
      {state.status === 'loading' && <WidgetLoading label="Cargando racha…" rows={2} />}
      {state.status === 'error' && (
        <ErrorState message={STREAK_ERROR} onRetry={() => setReloadToken((n) => n + 1)} />
      )}
      {state.status === 'ready' && (
        <div className={styles.streak}>
          <div className={styles.streakHeadline}>
            <span className={styles.streakFire} aria-hidden="true">
              🔥
            </span>
            <p className={styles.streakValue}>{state.streak.currentStreakDays}</p>
            <span className={styles.streakUnit}>
              {state.streak.currentStreakDays === 1 ? 'día' : 'días'}
            </span>
          </div>
          <p className={styles.streakCheer}>¡Sigue así!</p>
          <p className={styles.streakNote}>
            Récord: {state.streak.longestStreakDays}{' '}
            {state.streak.longestStreakDays === 1 ? 'día' : 'días'}
          </p>
        </div>
      )}
    </Card>
  );
}

function SessionDetailModal({
  target,
  onClose,
  mark,
  pending,
}: {
  readonly target: DetailTarget;
  readonly onClose: () => void;
  readonly mark: (id: string, status: SessionStatus) => void;
  readonly pending: boolean;
}) {
  const { dayOfWeek, session } = target;
  return (
    <Modal
      title={`${DAY_LABELS[dayOfWeek] ?? dayOfWeek} · ${KIND_LABELS[session.kind]}`}
      onClose={onClose}
    >
      <div className={styles.detail}>
        <p className={styles.detailTitle}>{session.title}</p>
        <p className={styles.sessionDetail}>{session.detail}</p>
        <StatusPill kind="training" value={session.status} />
        {session.notes && <p className={styles.notes}>{session.notes}</p>}
        {session.kind === 'STRENGTH' && (
          <>
            <p className={styles.placeholder}>
              El desglose por ejercicio (series, reps, peso, descanso) no está disponible todavía:
              la API no expone las plantillas de fuerza por HTTP.
            </p>
            <MuscleMapSection sessionId={session.id} />
          </>
        )}
        <div className={styles.actions}>
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
        </div>
      </div>
    </Modal>
  );
}

type MuscleMapState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly muscles: readonly MuscleGroupDisplay[] };

const MUSCLE_MAP_ERROR = 'No se pudieron cargar los músculos trabajados.';

/**
 * The FOR-136 worked-muscle heatmap for a strength session (spec FOR-53: "now
 * backed by FOR-136 — wire the heatmap to the muscle-map endpoint"). Fetched
 * on demand per session (the endpoint is per-session, not part of the FOR-26
 * week payload) and normalized for display via `trainingMuscleLabels`
 * (frontend-owned grouping — the backend read model itself stays untouched).
 * A load failure here is scoped to this section only: the rest of the
 * session detail (status, actions) keeps working, matching the page's
 * existing "error, prior state preserved" pattern.
 */
function MuscleMapSection({ sessionId }: { readonly sessionId: string }) {
  const [state, setState] = useState<MuscleMapState>({ status: 'loading' });

  useEffect(() => {
    let cancelled = false;
    setState({ status: 'loading' });
    getMuscleMap(sessionId)
      .then((map) => {
        if (!cancelled) {
          setState({ status: 'ready', muscles: groupMusclesForDisplay(map.muscles) });
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

  return (
    <div className={styles.muscleMap}>
      <h3 className={styles.muscleMapTitle}>Músculos trabajados</h3>
      {state.status === 'loading' && (
        <WidgetLoading label="Cargando músculos trabajados…" rows={2} />
      )}
      {state.status === 'error' && (
        <p className={styles.muscleMapError} role="alert">
          {MUSCLE_MAP_ERROR}
        </p>
      )}
      {state.status === 'ready' &&
        (state.muscles.length === 0 ? (
          <p className={styles.message}>Sin datos de músculos para esta sesión.</p>
        ) : (
          <ul className={styles.muscleList} aria-label="Músculos trabajados">
            {state.muscles.map((muscle) => (
              <li key={muscle.label} className={styles.muscleItem}>
                <Badge tone="neutral">{muscle.label}</Badge>
                <StatusPill kind="muscleLoad" value={muscle.load} />
              </li>
            ))}
          </ul>
        ))}
    </div>
  );
}
