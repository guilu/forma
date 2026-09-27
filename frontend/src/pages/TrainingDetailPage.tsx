import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  getMuscleMap,
  getSessionSets,
  getTrainingWeek,
  getWorkout,
  putSessionSet,
  updateSessionStatus,
  type LoggedSet,
  type MuscleWorkedMap,
  type SessionStatus,
  type TrainingSession,
  type Workout,
  type WorkoutItem,
} from '../api/training';
import { Badge } from '../components/Badge';
import { BodyFigure } from '../components/BodyFigure';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { ErrorState } from '../components/ErrorState';
import { Icon } from '../components/Icon';
import { IconButton } from '../components/IconButton';
import { LoadingState } from '../components/LoadingState';
import { ProgressRing } from '../components/ProgressRing';
import { StatusPill } from '../components/StatusPill';
import { groupMusclesForDisplay } from './trainingMuscleLabels';
import { displayTitle, repTarget } from './trainingPrescription';
import { formatShortDate } from './dateLabel';
import styles from './TrainingDetailPage.module.css';

type DetailState =
  | { readonly status: 'loading' }
  | { readonly status: 'error'; readonly message: string }
  | {
      readonly status: 'ready';
      readonly session: TrainingSession;
      readonly workout: Workout;
      readonly muscleMap: MuscleWorkedMap;
      readonly sets: readonly LoggedSet[];
    };

const ESTIMATED_DURATION_MIN = 55;
const REST_SECONDS = 90;
const LOAD_WEIGHT = { HIGH: 3, MEDIUM: 2, LOW: 1 } as const;

/**
 * The muscle-focus palette, in slice order. One list feeds both the donut's
 * conic-gradient and the legend swatches — they are the same chart drawn twice,
 * and two lists would drift until the legend named the wrong slice.
 *
 * <p>Violet sits second because the slot used to fall to `--color-secondary`, a
 * lime that reads as a second green next to the accent; the mockups paint it
 * violet so adjacent slices stay tellable apart. All four are the graphic-role
 * tokens (3:1 bar), never the text-safe ones.
 */
const MUSCLE_SLICE_COLORS = [
  'var(--color-accent)',
  'var(--color-violet)',
  'var(--color-warning-graphic)',
  'var(--color-info)',
] as const;

type SetEntry = {
  readonly weight: string;
  readonly reps: string;
  readonly done: boolean;
};

type SetEntries = Readonly<Record<string, SetEntry>>;

function workoutDescription(title: string): string {
  return `Entrenamiento enfocado en desarrollar fuerza en ${title.toLocaleLowerCase('es-ES')}. Mantén una técnica controlada y aumenta las cargas de forma progresiva.`;
}

function formatTimer(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
}

function setKey(exerciseId: string, setNumber: number): string {
  return `${exerciseId}:${setNumber}`;
}

/**
 * Builds the set table's local editing state from the server's per-set log
 * (training-set-log, design D6), not from an ephemeral in-memory default: a
 * reload or a return visit later in the same week must show what was
 * actually persisted. A set the log has nothing for yet (fresh week, spec
 * "arranca vacía") falls back to an empty weight and the prescription's own
 * target reps, mirroring the pre-persistence placeholder behaviour.
 */
function buildSetEntries(workout: Workout, sets: readonly LoggedSet[]): SetEntries {
  const logged = new Map(sets.map((set) => [setKey(set.exerciseId, set.setNumber), set]));
  return Object.fromEntries(
    workout.items.flatMap((item) =>
      Array.from({ length: item.sets }, (_, index) => {
        const setNumber = index + 1;
        const key = setKey(item.exerciseId, setNumber);
        const loggedSet = logged.get(key);
        return [
          key,
          {
            weight: loggedSet?.weightKg != null ? String(loggedSet.weightKg) : '',
            reps:
              loggedSet?.reps != null
                ? String(loggedSet.reps)
                : String(item.repsMin ?? item.repsMax ?? ''),
            done: loggedSet?.done ?? false,
          },
        ];
      }),
    ),
  );
}

/** Parses a set-table input's raw string into what the log endpoint expects: `null` for
 * an empty/unparseable value, never `NaN` (design D7 "registro parcial"). */
function parseOptionalNumber(value: string): number | null {
  const trimmed = value.trim();
  if (trimmed === '') return null;
  const parsed = Number(trimmed);
  return Number.isNaN(parsed) ? null : parsed;
}

export function TrainingDetailPage() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const [state, setState] = useState<DetailState>({ status: 'loading' });
  const [pendingStatus, setPendingStatus] = useState<SessionStatus>();
  const [actionError, setActionError] = useState<string>();

  const load = useCallback(async () => {
    if (!sessionId) {
      setState({ status: 'error', message: 'No se ha indicado ningún entrenamiento.' });
      return;
    }
    setState({ status: 'loading' });
    try {
      const week = await getTrainingWeek();
      const session = week.days
        .flatMap((day) => day.sessions)
        .find((item) => item.id === sessionId);
      if (!session) {
        setState({
          status: 'error',
          message: 'Este entrenamiento no pertenece a la semana actual.',
        });
        return;
      }
      if (session.kind !== 'STRENGTH' || !session.workoutType) {
        setState({
          status: 'error',
          message: 'El detalle completo está disponible para los entrenamientos de fuerza.',
        });
        return;
      }
      const [workout, muscleMap, sessionSets] = await Promise.all([
        getWorkout(session.workoutType),
        getMuscleMap(session.id),
        getSessionSets(session.id),
      ]);
      setState({ status: 'ready', session, workout, muscleMap, sets: sessionSets.sets });
    } catch {
      setState({ status: 'error', message: 'No se pudo cargar el detalle del entrenamiento.' });
    }
  }, [sessionId]);

  useEffect(() => {
    void load();
  }, [load]);

  async function changeStatus(status: SessionStatus) {
    if (state.status !== 'ready') return;
    setPendingStatus(status);
    setActionError(undefined);
    try {
      await updateSessionStatus(state.session.id, status);
      setState({ ...state, session: { ...state.session, status } });
    } catch {
      setActionError('No se pudo actualizar el entrenamiento. Inténtalo de nuevo.');
    } finally {
      setPendingStatus(undefined);
    }
  }

  if (state.status === 'loading') {
    return (
      <div className={styles.page}>
        <DetailTopbar />
        <LoadingState message="Cargando entrenamiento…" />
      </div>
    );
  }
  if (state.status === 'error') {
    return (
      <div className={styles.page}>
        <DetailTopbar />
        <ErrorState message={state.message} onRetry={load} />
      </div>
    );
  }

  return (
    <TrainingDetailContent
      state={state}
      pendingStatus={pendingStatus}
      actionError={actionError}
      onStatusChange={changeStatus}
    />
  );
}

function TrainingDetailContent({
  state,
  pendingStatus,
  actionError,
  onStatusChange,
}: {
  readonly state: Extract<DetailState, { status: 'ready' }>;
  readonly pendingStatus: SessionStatus | undefined;
  readonly actionError: string | undefined;
  readonly onStatusChange: (status: SessionStatus) => void;
}) {
  const title = displayTitle(state.session.title);
  const completed = state.session.status === 'COMPLETED';
  const [startedAt] = useState(() => Date.now());
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const [restSeconds, setRestSeconds] = useState(0);
  const [restPaused, setRestPaused] = useState(false);
  const [setEntries, setSetEntries] = useState<SetEntries>(() =>
    buildSetEntries(state.workout, state.sets),
  );
  const [setLogError, setSetLogError] = useState<string>();
  const muscles = groupMusclesForDisplay(state.muscleMap.muscles);
  const muscleTotal = muscles.reduce((total, muscle) => total + LOAD_WEIGHT[muscle.load], 0);
  const muscleSlices = muscles.map((muscle, index) => ({
    ...muscle,
    color: MUSCLE_SLICE_COLORS[index % MUSCLE_SLICE_COLORS.length],
    percentage: muscleTotal > 0 ? Math.round((LOAD_WEIGHT[muscle.load] / muscleTotal) * 100) : 0,
  }));
  /*
   * The chips above the donut. The mockup shows only the muscles the session
   * really leans on, not the full legend, and `HIGH` is the backend's own word
   * for that — so the chips are derived, never a second hand-kept list.
   */
  const primaryMuscles = muscles.filter((muscle) => muscle.load === 'HIGH');
  const totalVolume = useMemo(
    () =>
      Object.values(setEntries).reduce(
        (total, entry) =>
          entry.done ? total + (Number(entry.weight) || 0) * (Number(entry.reps) || 0) : total,
        0,
      ),
    [setEntries],
  );

  /*
   * The ring counts sets, not the session's status. `setEntries` is built from the
   * current template only, so orphaned logged sets (design D6) never reach this
   * count — the same rule that keeps them off the table. A session marked
   * COMPLETED with sets still pending shows its real share: completing early is
   * legitimate, inventing the sets that were never logged is not.
   */
  const setProgress = useMemo(() => {
    const entries = Object.values(setEntries);
    return { done: entries.filter((entry) => entry.done).length, total: entries.length };
  }, [setEntries]);
  const progressPercent =
    setProgress.total > 0 ? Math.round((setProgress.done / setProgress.total) * 100) : 0;

  useEffect(() => {
    const interval = window.setInterval(
      () => setElapsedSeconds(Math.floor((Date.now() - startedAt) / 1000)),
      1000,
    );
    return () => window.clearInterval(interval);
  }, [startedAt]);

  useEffect(() => {
    if (restSeconds === 0 || restPaused) return;
    const interval = window.setInterval(
      () => setRestSeconds((seconds) => Math.max(0, seconds - 1)),
      1000,
    );
    return () => window.clearInterval(interval);
  }, [restPaused, restSeconds]);

  function updateSet(
    item: WorkoutItem,
    setNumber: number,
    field: 'weight' | 'reps',
    value: string,
  ) {
    const key = setKey(item.exerciseId, setNumber);
    setSetEntries((entries) => ({
      ...entries,
      [key]: { ...entries[key], [field]: value },
    }));
  }

  /** Persists one set (design D7). Never throws — a failed write shows a visible error
   * instead of an unhandled rejection, and never blocks the local, already-applied edit. */
  async function persistSet(item: WorkoutItem, setNumber: number, entry: SetEntry) {
    setSetLogError(undefined);
    try {
      await putSessionSet(state.session.id, item.exerciseId, setNumber, {
        weightKg: parseOptionalNumber(entry.weight),
        reps: parseOptionalNumber(entry.reps),
        done: entry.done,
      });
    } catch {
      setSetLogError('No se pudo guardar la serie. Inténtalo de nuevo.');
    }
  }

  function commitSet(item: WorkoutItem, setNumber: number) {
    const key = setKey(item.exerciseId, setNumber);
    void persistSet(item, setNumber, setEntries[key]);
  }

  function toggleSet(item: WorkoutItem, setNumber: number) {
    const key = setKey(item.exerciseId, setNumber);
    const willBeDone = !setEntries[key].done;
    setSetEntries((entries) => ({
      ...entries,
      [key]: { ...entries[key], done: willBeDone },
    }));
    if (willBeDone) {
      setRestSeconds(REST_SECONDS);
      setRestPaused(false);
    }
    void persistSet(item, setNumber, { ...setEntries[key], done: willBeDone });
  }

  return (
    <div className={styles.page}>
      <DetailTopbar />

      <SessionMetrics
        elapsedSeconds={elapsedSeconds}
        restSeconds={restSeconds}
        restPaused={restPaused}
        totalVolume={totalVolume}
        onToggleRest={() => setRestPaused((paused) => !paused)}
      />

      <div className={styles.layout}>
        <main className={styles.main}>
          <Card className={styles.hero}>
            <div className={styles.heroCopy}>
              <Badge tone="violet" className={styles.kindBadge}>
                Fuerza
              </Badge>
              <div className={styles.titleRow}>
                <h1>{title}</h1>
                <StatusPill kind="training" value={state.session.status} />
              </div>
              <p className={styles.description}>{workoutDescription(title)}</p>
              <dl className={styles.metrics}>
                <Metric icon="clock" label="Duración" value={`${ESTIMATED_DURATION_MIN} min`} />
                <Metric
                  icon="training"
                  label="Enfoque"
                  value={
                    muscles
                      .slice(0, 3)
                      .map((muscle) => muscle.label)
                      .join(', ') || 'Fuerza'
                  }
                />
                <Metric
                  icon="progress"
                  label="Ejercicios"
                  value={`${state.workout.items.length}`}
                />
              </dl>
            </div>
            <BodyFigure view="front" variant="strength" active size={190} />
          </Card>

          <section className={styles.exercises} aria-label="Ejercicios del entrenamiento">
            <header className={styles.sectionHeader}>
              <h2>Ejercicios ({state.workout.items.length})</h2>
              {/* Legend for the Estado column, which is icon-only in the
                  mockup: the ticks need something that names them. */}
              <span className={styles.sectionStatus}>
                <span>
                  <Icon name="checkCircle" size={15} /> Completado
                </span>
                <span className={styles.sectionStatusPending}>
                  <Icon name="checkCircle" size={15} /> Pendiente
                </span>
              </span>
            </header>
            <ol className={styles.exerciseList}>
              {state.workout.items.map((item) => (
                <ExerciseCard
                  key={item.exerciseId}
                  item={item}
                  entries={setEntries}
                  disabled={completed}
                  onChange={updateSet}
                  onBlur={commitSet}
                  onToggle={toggleSet}
                />
              ))}
            </ol>
            <div className={styles.exerciseActions}>
              <Button
                type="button"
                variant="secondary"
                disabled={pendingStatus !== undefined}
                onClick={() => onStatusChange('SKIPPED')}
              >
                <Icon name="trash" size={17} /> Descartar sesión
              </Button>
              <Button
                type="button"
                disabled={completed || pendingStatus !== undefined}
                loading={pendingStatus === 'COMPLETED'}
                onClick={() => onStatusChange('COMPLETED')}
              >
                <Icon name="checkCircle" size={17} />
                {completed ? 'Entrenamiento completado' : 'Finalizar y guardar entrenamiento'}
              </Button>
            </div>
          </section>
        </main>

        <aside className={styles.side}>
          <Card title="Progreso del entrenamiento" className={styles.progressCard}>
            <ProgressRing
              value={setProgress.done}
              max={setProgress.total}
              size={128}
              label={`${setProgress.done} de ${setProgress.total} series completadas`}
            >
              <strong className={styles.progressPercent}>{progressPercent}%</strong>
            </ProgressRing>
            <strong className={styles.progressMessage}>
              {completed ? '¡Entrenamiento completado!' : 'Entrenamiento pendiente'}
            </strong>
            <span>
              {setProgress.done} de {setProgress.total} series
            </span>
            <Button
              type="button"
              disabled={completed || pendingStatus !== undefined}
              loading={pendingStatus === 'COMPLETED'}
              onClick={() => onStatusChange('COMPLETED')}
            >
              <Icon name="checkCircle" size={17} />
              {completed ? 'Completado' : 'Marcar como completado'}
            </Button>
          </Card>

          <Card title="Enfoque muscular">
            {primaryMuscles.length > 0 && (
              <ul className={styles.muscleTags}>
                {primaryMuscles.map((muscle) => (
                  <li key={muscle.label}>
                    <Badge tone="violet">{muscle.label}</Badge>
                  </li>
                ))}
              </ul>
            )}
            <div
              className={styles.muscleChart}
              aria-label="Distribución relativa del enfoque muscular"
            >
              <div
                className={styles.muscleDonut}
                style={{ background: muscleGradient(muscleSlices) }}
                aria-hidden="true"
              />
              <ul>
                {muscleSlices.map((muscle) => (
                  <li key={muscle.label}>
                    <span
                      className={styles.muscleDot}
                      style={{ backgroundColor: muscle.color }}
                      aria-hidden="true"
                    />
                    <span>{muscle.label}</span>
                    <strong>{muscle.percentage}%</strong>
                  </li>
                ))}
              </ul>
            </div>
          </Card>

          <Card className={styles.tip}>
            <h2>
              <Icon name="goals" size={19} /> Consejo del día
            </h2>
            <p>
              Mantén una técnica controlada en cada repetición. La calidad siempre supera a la
              cantidad.
            </p>
          </Card>
        </aside>
      </div>

      {actionError && (
        <p className={styles.actionError} role="alert">
          {actionError}
        </p>
      )}
      {setLogError && (
        <p className={styles.actionError} role="alert">
          {setLogError}
        </p>
      )}
    </div>
  );
}

function SessionMetrics({
  elapsedSeconds,
  restSeconds,
  restPaused,
  totalVolume,
  onToggleRest,
}: {
  readonly elapsedSeconds: number;
  readonly restSeconds: number;
  readonly restPaused: boolean;
  readonly totalVolume: number;
  readonly onToggleRest: () => void;
}) {
  return (
    <section className={styles.sessionMetrics} aria-label="Métricas de la sesión">
      <div>
        <span>Tiempo transcurrido</span>
        <strong>{formatTimer(elapsedSeconds)}</strong>
      </div>
      <div>
        <span>Descanso</span>
        <strong className={styles.restTime}>{restSeconds}s</strong>
        <button type="button" disabled={restSeconds === 0} onClick={onToggleRest}>
          {restPaused ? 'Continuar' : 'Pausa'}
        </button>
      </div>
      <div>
        <span>Volumen total</span>
        <strong>{totalVolume.toLocaleString('es-ES')} kg</strong>
      </div>
    </section>
  );
}

function DetailTopbar() {
  return (
    <header className={styles.topbar}>
      <Link className={styles.backLink} to="/app/training">
        <Icon name="chevron" size={17} className={styles.backIcon} />
        Volver a Entrenamiento
      </Link>
      <div className={styles.date}>
        <Icon name="calendar" size={17} />
        <span>{formatShortDate(new Date())}</span>
      </div>
    </header>
  );
}

function muscleGradient(slices: readonly { percentage: number; color: string }[]): string {
  if (slices.length === 0) return 'var(--color-border)';
  let cursor = 0;
  const segments = slices.map((slice) => {
    const start = cursor;
    cursor += slice.percentage;
    return `${slice.color} ${start}% ${Math.min(cursor, 100)}%`;
  });
  return `conic-gradient(${segments.join(', ')})`;
}

function Metric({
  icon,
  label,
  value,
}: {
  readonly icon: 'clock' | 'training' | 'progress';
  readonly label: string;
  readonly value: string;
}) {
  return (
    <div>
      <dt>
        <Icon name={icon} size={16} /> {label}
      </dt>
      <dd>{value}</dd>
    </div>
  );
}

function ExerciseCard({
  item,
  entries,
  disabled,
  onChange,
  onBlur,
  onToggle,
}: {
  readonly item: WorkoutItem;
  readonly entries: SetEntries;
  readonly disabled: boolean;
  readonly onChange: (
    item: WorkoutItem,
    setNumber: number,
    field: 'weight' | 'reps',
    value: string,
  ) => void;
  readonly onBlur: (item: WorkoutItem, setNumber: number) => void;
  readonly onToggle: (item: WorkoutItem, setNumber: number) => void;
}) {
  return (
    <li className={styles.exerciseCard}>
      <span className={styles.exerciseOrder}>{item.order}</span>
      <div className={styles.exerciseIdentity}>
        <Icon name="training" size={30} />
        <div>
          <h3>{item.exerciseName}</h3>
          <span className={styles.exerciseTag}>Fuerza · RIR {item.rir}</span>
          <small>Descanso entre series</small>
          <small className={styles.exerciseRest}>
            <Icon name="clock" size={13} /> {item.restSeconds} s
          </small>
        </div>
      </div>
      <div className={styles.setTable} role="table" aria-label={`Series de ${item.exerciseName}`}>
        <div className={styles.setHeader} role="row">
          <span>Serie</span>
          <span>Peso (kg)</span>
          <span>Reps</span>
          <span>Estado</span>
        </div>
        {Array.from({ length: item.sets }, (_, index) => {
          const setNumber = index + 1;
          const key = setKey(item.exerciseId, setNumber);
          const entry = entries[key];
          return (
            <div
              key={key}
              className={`${styles.setRow} ${entry.done ? styles.setRowDone : ''}`}
              role="row"
            >
              <span>{setNumber}</span>
              <input
                type="number"
                min="0"
                step="0.5"
                value={entry.weight}
                placeholder="0"
                disabled={disabled || entry.done}
                aria-label={`Peso, ${item.exerciseName}, serie ${setNumber}`}
                onChange={(event) => onChange(item, setNumber, 'weight', event.target.value)}
                onBlur={() => onBlur(item, setNumber)}
              />
              <input
                type="number"
                min="0"
                step="1"
                value={entry.reps}
                placeholder={repTarget(item)}
                disabled={disabled || entry.done}
                aria-label={`Repeticiones, ${item.exerciseName}, serie ${setNumber}`}
                onChange={(event) => onChange(item, setNumber, 'reps', event.target.value)}
                onBlur={() => onBlur(item, setNumber)}
              />
              <IconButton
                variant="ghost"
                size="sm"
                className={styles.doneButton}
                data-done={entry.done}
                disabled={disabled}
                label={`${entry.done ? 'Reabrir' : 'Completar'} ${item.exerciseName}, serie ${setNumber}`}
                onClick={() => onToggle(item, setNumber)}
              >
                <Icon name="checkCircle" size={17} />
              </IconButton>
            </div>
          );
        })}
      </div>
    </li>
  );
}
