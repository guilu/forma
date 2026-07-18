import { useCallback, useEffect, useState } from 'react';
import { Badge } from '../components/Badge';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { EmptyState } from '../components/EmptyState';
import { ErrorState } from '../components/ErrorState';
import { LoadingState } from '../components/LoadingState';
import { MetricCard } from '../components/MetricCard';
import { Modal } from '../components/Modal';
import { useNotify } from '../components/NotificationProvider';
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
        <p className={styles.dateLabel}>{formatToday()}</p>
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
      </div>
      <div className={styles.side}>
        <WeeklySummary days={state.week.days} />
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
      <ul className={styles.todaySessions}>
        {day.sessions.map((session) => (
          <li key={session.id} className={styles.todaySession}>
            <div className={styles.todaySessionHeader}>
              <Badge tone={session.kind === 'RUNNING' ? 'accent' : 'neutral'}>
                {KIND_LABELS[session.kind]}
              </Badge>
              <StatusPill kind="training" value={session.status} />
            </div>
            <p className={styles.todaySessionTitle}>{session.title}</p>
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
      <div className={styles.progressWrap}>
        <span className={styles.progressText}>
          {completed} de {planned} sesiones completadas hoy
        </span>
        <div
          className={styles.progressTrack}
          role="progressbar"
          aria-label="Progreso de hoy"
          aria-valuenow={percent}
          aria-valuemin={0}
          aria-valuemax={100}
        >
          <div className={styles.progressFill} style={{ width: `${percent}%` }} />
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
  return (
    <Card title="Calendario semanal" headingLevel={2}>
      <ul className={styles.calendarGrid} aria-label="Calendario semanal de entrenamiento">
        {days.map((day) => (
          <li key={day.dayOfWeek} className={styles.calendarDay}>
            <h3 className={styles.calendarDayTitle}>
              {DAY_LABELS[day.dayOfWeek] ?? day.dayOfWeek}
            </h3>
            {day.rest ? (
              <p className={styles.rest}>Descanso</p>
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
                      <StatusPill kind="training" value={session.status} />
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </li>
        ))}
      </ul>
    </Card>
  );
}

function WeeklySummary({ days }: { readonly days: readonly TrainingDay[] }) {
  const sessions = days.flatMap((day) => day.sessions);
  const running = sessions.filter((s) => s.kind === 'RUNNING');
  const strength = sessions.filter((s) => s.kind === 'STRENGTH');
  const total = tally(sessions);
  const runningTally = tally(running);
  const strengthTally = tally(strength);

  return (
    <Card title="Resumen semanal" headingLevel={2} className={styles.summary}>
      <div className={styles.summaryGrid}>
        <MetricCard label="Sesiones totales" value={`${total.completed}/${total.planned}`} />
        <MetricCard label="Carrera" value={`${runningTally.completed}/${runningTally.planned}`} />
        <MetricCard label="Fuerza" value={`${strengthTally.completed}/${strengthTally.planned}`} />
      </div>
      <p className={styles.summaryNote}>
        Duración y volumen totales no están disponibles todavía en la API.
      </p>
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
          <p className={styles.streakValue}>{state.streak.currentStreakDays}</p>
          <p className={styles.streakUnit}>
            {state.streak.currentStreakDays === 1 ? 'día' : 'días'}
          </p>
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
