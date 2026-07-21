import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Badge } from '../components/Badge';
import { BodyFigure } from '../components/BodyFigure';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { EmptyState } from '../components/EmptyState';
import { ErrorState } from '../components/ErrorState';
import { Icon, type IconName } from '../components/Icon';
import { LoadingState } from '../components/LoadingState';
import { MetricCard } from '../components/MetricCard';
import { Modal } from '../components/Modal';
import { useNotify } from '../components/NotificationProvider';
import { ProgressRing } from '../components/ProgressRing';
import { StatusPill } from '../components/StatusPill';
import { WidgetLoading } from '../components/WidgetLoading';
import { ApiRequestError } from '../api/client';
import { getStreak, getWeeklyHistory, type Streak, type WeeklyHistory } from '../api/progress';
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
 *       normalization). The weekly-history bars and "RACHA ACTUAL" (this
 *       comment's own prior gap note) are now wired by FOR-143 to the FOR-139
 *       {@code GET …/progress/streak} / {@code GET …/progress/weekly-history}
 *       endpoints ({@link StreakCard}, {@link WeeklyHistoryCard} below) — both
 *       are a real **nutrition meal-log** consistency signal, not a training
 *       one (no per-date training-completion history exists to back a
 *       training streak or per-week training bar; spec FOR-139: "do NOT
 *       fabricate per-date training completion" — surfaced here exactly as
 *       the backend documents it, per ADR-001).
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
  today: { durationMin: 55, focus: 'Pecho, Hombros, Tríceps', exercisesDone: 4, exercisesTotal: 6 },
  stats: {
    volume: '12.450',
    volumeDelta: '↑8% vs semana anterior',
    duration: '48:32',
    durationDelta: '↑5 min vs semana anterior',
    calories: '2.120',
    caloriesDelta: '↑12% vs semana anterior',
  },
  muscleGroups: [
    { label: 'Pecho', quality: 'Excelente' },
    { label: 'Espalda', quality: 'Bueno' },
    { label: 'Hombros', quality: 'Excelente' },
    { label: 'Brazos', quality: 'Bueno' },
    { label: 'Piernas', quality: 'Bueno' },
    { label: 'Core', quality: 'Excelente' },
  ],
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

function formatToday(): string {
  return new Date().toLocaleDateString('es-ES', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  });
}

function tally(sessions: readonly TrainingSession[]): { completed: number; planned: number } {
  return {
    completed: sessions.filter((s) => s.status === 'COMPLETED').length,
    planned: sessions.length,
  };
}

export function TrainingPage() {
  const notify = useNotify();
  const [state, setState] = useState<State>({ status: 'loading' });
  const [actionError, setActionError] = useState<string | undefined>(undefined);
  const [pendingId, setPendingId] = useState<string | undefined>(undefined);
  const [detailTarget, setDetailTarget] = useState<DetailTarget | undefined>(undefined);

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

  return (
    <div className={styles.wrapper}>
      <header className={styles.header}>
        <div className={styles.titles}>
          <h1 className={styles.title}>Entrenamiento</h1>
          <p className={styles.subtitle}>Sigue tu plan y mejora cada día.</p>
        </div>
        {/* Date navigator — visual only: the composed week has no dates / week
            navigation (docs/api/training-week.md), so the arrows are inert
            decorative affordances and the label is today's real date. */}
        <div className={styles.dateNav}>
          <span className={styles.dateArrow} aria-hidden="true">
            <Icon name="chevron" size={16} className={styles.dateArrowPrev} />
          </span>
          <span className={styles.dateLabel}>{formatToday()}</span>
          <span className={styles.dateArrow} aria-hidden="true">
            <Icon name="chevron" size={16} />
          </span>
        </div>
      </header>

      {actionError && (
        <p className={styles.actionError} role="alert">
          {actionError}
        </p>
      )}

      {renderContent(state, mark, pendingId, setDetailTarget, load)}

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
  reload: () => void,
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
    return <EmptyState title="No hay entrenamientos planificados esta semana." />;
  }

  const today = state.week.days.find((day) => day.dayOfWeek === todayDayOfWeek());

  return (
    <div className={styles.layout}>
      <div className={styles.main}>
        <TodaySessionCard day={today} mark={mark} pendingId={pendingId} openDetail={openDetail} />
        <WeeklyCalendar days={state.week.days} openDetail={openDetail} />
        <StatsRow days={state.week.days} />
        <MuscleGroupsSection />
      </div>
      <div className={styles.side}>
        <WeeklySummary days={state.week.days} />
        <WeeklyDistribution days={state.week.days} />
        <StreakCard />
        <WeeklyHistoryCard />
      </div>
    </div>
  );
}

function TodaySessionCard({
  day,
  mark,
  pendingId,
  openDetail,
}: {
  readonly day: TrainingDay | undefined;
  readonly mark: (id: string, status: SessionStatus) => void;
  readonly pendingId: string | undefined;
  readonly openDetail: (target: DetailTarget) => void;
}) {
  if (!day) {
    return (
      <Card title="Entrenamiento de hoy" headingLevel={2}>
        <p className={styles.message}>No hay datos de hoy en el plan de esta semana.</p>
      </Card>
    );
  }

  if (day.rest) {
    return (
      <Card title="Entrenamiento de hoy" headingLevel={2}>
        <p className={styles.rest}>Hoy es día de descanso.</p>
      </Card>
    );
  }

  const { completed, planned } = tally(day.sessions);
  const percent = planned > 0 ? Math.round((completed / planned) * 100) : 0;

  return (
    <Card title="Entrenamiento de hoy" headingLevel={2}>
      <div className={styles.todayLayout}>
        <ul className={styles.todaySessions}>
          {day.sessions.map((session) => (
            <li key={session.id} className={styles.todaySession}>
              <div className={styles.todaySessionHeader}>
                <p className={styles.todaySessionTitle}>{session.title}</p>
                <StatusPill kind="training" value={session.status} />
              </div>
              {/* Placeholder estimated duration + focus (see PLACEHOLDER). */}
              <p className={styles.sessionDetail}>
                Duración estimada: {PLACEHOLDER.today.durationMin} min
              </p>
              <p className={styles.sessionDetail}>Enfoque: {PLACEHOLDER.today.focus}</p>
              <p className={styles.sessionDetail}>{session.detail}</p>
              <div className={styles.actions}>
                {session.status !== 'COMPLETED' && (
                  <Button
                    type="button"
                    disabled={pendingId === session.id}
                    loading={pendingId === session.id}
                    onClick={() => mark(session.id, 'COMPLETED')}
                  >
                    Iniciar entrenamiento
                  </Button>
                )}
                {session.status !== 'SKIPPED' && (
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
                  variant="ghost"
                  onClick={() => openDetail({ dayOfWeek: day.dayOfWeek, session })}
                >
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
              size={110}
            >
              <span className={styles.ringPercent}>{percent}%</span>
            </ProgressRing>
            <p className={styles.ringCaption}>
              {PLACEHOLDER.today.exercisesDone} / {PLACEHOLDER.today.exercisesTotal} ejercicios
              completados
            </p>
          </div>
          <div className={styles.todayFigures}>
            <BodyFigure view="front" variant="strength" active size={132} />
            <BodyFigure view="back" variant="strength" size={132} />
          </div>
        </div>
      </div>
    </Card>
  );
}

function WeeklyCalendar({
  days,
  openDetail,
}: {
  readonly days: readonly TrainingDay[];
  readonly openDetail: (target: DetailTarget) => void;
}) {
  const todayEnum = todayDayOfWeek();

  return (
    <Card title="Calendario semanal" headingLevel={2}>
      <ul className={styles.calendarGrid} aria-label="Calendario semanal de entrenamiento">
        {days.map((day) => (
          <li
            key={day.dayOfWeek}
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
                      <Badge tone={session.kind === 'RUNNING' ? 'accent' : 'neutral'}>
                        {KIND_LABELS[session.kind]}
                      </Badge>
                      <span className={styles.calendarSessionTitle}>{session.title}</span>
                      <BodyFigure
                        variant={session.kind === 'RUNNING' ? 'running' : 'strength'}
                        active={session.status === 'COMPLETED'}
                        size={72}
                      />
                      <StatusPill kind="training" value={session.status} />
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </li>
        ))}
      </ul>
      <ul className={styles.calendarLegend} aria-hidden="true">
        <li>
          <span className={`${styles.legendDot} ${styles.legendDone}`} /> Completado
        </li>
        <li>
          <span className={`${styles.legendDot} ${styles.legendToday}`} /> Hoy
        </li>
        <li>
          <span className={`${styles.legendDot} ${styles.legendPending}`} /> Pendiente
        </li>
        <li>
          <span className={`${styles.legendDot} ${styles.legendRest}`} /> Descanso
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

  const rows: { label: string; icon: IconName; t: { completed: number; planned: number } }[] = [
    { label: 'Sesiones totales', icon: 'measurements', t: total },
    { label: 'Carrera', icon: 'activity', t: runningTally },
    { label: 'Fuerza', icon: 'training', t: strengthTally },
  ];

  return (
    <Card title="Resumen semanal" headingLevel={2} className={styles.summary}>
      <ul className={styles.summaryList}>
        {rows.map((row) => (
          <li key={row.label} className={styles.summaryRow}>
            <MetricCard
              label={row.label}
              icon={row.icon}
              value={`${row.t.completed}/${row.t.planned}`}
            />
            <ProgressRing
              value={row.t.completed}
              max={Math.max(row.t.planned, 1)}
              label={`${row.label}: ${row.t.completed} de ${row.t.planned}`}
              size={60}
            >
              <span className={styles.summaryRingText}>{summaryPercent(row.t)}%</span>
            </ProgressRing>
          </li>
        ))}
      </ul>
      <Link className={styles.summaryLink} to="/progreso">
        Ver estadísticas completas
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
    background: `conic-gradient(var(--color-warning) 0deg ${strengthDeg}deg, var(--color-accent) ${strengthDeg}deg ${
      strengthDeg + runningDeg
    }deg, var(--color-border) ${strengthDeg + runningDeg}deg 360deg)`,
  };

  const legend = [
    { key: 'strength', label: 'Fuerza', count: strength, unit: 'sesiones', className: styles.dotStrength },
    { key: 'running', label: 'Carreras', count: running, unit: 'sesiones', className: styles.dotRunning },
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
                {item.count} {item.unit === 'días' ? (item.count === 1 ? 'día' : 'días') : item.count === 1 ? 'sesión' : 'sesiones'}
              </span>
            </li>
          ))}
        </ul>
      </div>
      <p className={styles.distributionNote}>
        <Icon name="check" size={16} className={styles.distributionNoteIcon} />
        {balanced ? 'Equilibrio adecuado. Buen balance entre fuerza y cardio.' : 'Añade variedad para equilibrar fuerza y cardio.'}
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

/**
 * "Grupos musculares trabajados esta semana" (FOR-164 mockup). Placeholder
 * quality labels + figures for now (the FOR-136 muscle map is per-session; a
 * real weekly aggregate would need a fetch per strength session — deferred).
 * Swap {@link BodyFigure} for the real asset pack later.
 */
function MuscleGroupsSection() {
  return (
    <Card title="Grupos musculares trabajados esta semana" headingLevel={2}>
      <div className={styles.muscleGroups}>
        <ul className={styles.muscleGroupGrid}>
          {PLACEHOLDER.muscleGroups.map((group) => (
            <li key={group.label} className={styles.muscleGroup}>
              <span className={styles.muscleGroupName}>{group.label}</span>
              <BodyFigure variant="strength" active size={84} />
              <span className={styles.muscleGroupQuality}>{group.quality}</span>
            </li>
          ))}
        </ul>
        <div className={styles.encourage}>
          <span className={styles.encourageIcon} aria-hidden="true">
            🏆
          </span>
          <p className={styles.encourageTitle}>¡Sigue así!</p>
          <p className={styles.encourageText}>Vas por buen camino para alcanzar tu objetivo.</p>
          <Link className={styles.encourageLink} to="/progreso">
            Ver progreso
          </Link>
        </div>
      </div>
    </Card>
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

type WeeklyHistoryState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly history: WeeklyHistory };

const WEEKLY_HISTORY_ERROR = 'No se pudo cargar el historial semanal. Inténtalo de nuevo.';

function formatWeekLabel(weekStart: string): string {
  return new Date(`${weekStart}T00:00:00`).toLocaleDateString('es-ES', {
    day: 'numeric',
    month: 'short',
  });
}

/**
 * Weekly-history bars widget (FOR-143, mockup docs/3-entrenamiento.png),
 * wired to the FOR-139 {@code GET /api/v1/progress/weekly-history} endpoint.
 * Fetches independently, same rationale as {@link StreakCard}. Renders one
 * bar per week exactly as returned — including all-zero weeks, which stay
 * visible bars, never hidden (spec FOR-139: "still present in the series,
 * never omitted-as-error"). Only a genuinely empty series (defensive; the
 * backend documents it never happens) falls back to {@link EmptyState}.
 */
function WeeklyHistoryCard() {
  const [state, setState] = useState<WeeklyHistoryState>({ status: 'loading' });
  const [reloadToken, setReloadToken] = useState(0);

  useEffect(() => {
    let active = true;
    setState({ status: 'loading' });
    getWeeklyHistory()
      .then((history) => {
        if (active) {
          setState({ status: 'ready', history });
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
    <Card title="Historial semanal" headingLevel={2}>
      <p className={styles.widgetCaption}>Días con registro de nutrición por semana.</p>
      {state.status === 'loading' && <WidgetLoading label="Cargando historial semanal…" rows={3} />}
      {state.status === 'error' && (
        <ErrorState message={WEEKLY_HISTORY_ERROR} onRetry={() => setReloadToken((n) => n + 1)} />
      )}
      {state.status === 'ready' &&
        (state.history.weeks.length === 0 ? (
          <EmptyState variant="filtered" title="Todavía no hay historial semanal." />
        ) : (
          <ul className={styles.historyBars} aria-label="Historial semanal de constancia">
            {state.history.weeks.map((week) => {
              const ratio = week.planned > 0 ? week.completed / week.planned : 0;
              return (
                <li key={week.weekStart} className={styles.historyBarItem}>
                  <span className={styles.historyBarTrack}>
                    <span
                      className={styles.historyBarFill}
                      style={{ height: `${Math.round(ratio * 100)}%` }}
                    />
                  </span>
                  <span className={styles.historyBarLabel}>{formatWeekLabel(week.weekStart)}</span>
                  <span className={styles.srOnly}>
                    {week.completed} de {week.planned} días
                  </span>
                </li>
              );
            })}
          </ul>
        ))}
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
