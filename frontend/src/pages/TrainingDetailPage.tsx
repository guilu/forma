import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  getMuscleMap,
  getTrainingWeek,
  getWorkout,
  updateSessionStatus,
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
import { LoadingState } from '../components/LoadingState';
import { ProgressRing } from '../components/ProgressRing';
import { StatusPill } from '../components/StatusPill';
import { groupMusclesForDisplay } from './trainingMuscleLabels';
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
    };

const ESTIMATED_DURATION_MIN = 55;
const LOAD_WEIGHT = { HIGH: 3, MEDIUM: 2, LOW: 1 } as const;

function displayTitle(title: string): string {
  return title.replace(/^Fuerza\s*·\s*/i, '');
}

function repTarget(item: WorkoutItem): string {
  if (item.repScheme === 'AMRAP') return 'AMRAP';
  if (item.repScheme === 'TIME_HOLD') {
    const min = item.durationSecondsMin ?? item.durationSecondsMax ?? 0;
    const max = item.durationSecondsMax ?? min;
    return min === max ? `${min} s` : `${min}–${max} s`;
  }
  const min = item.repsMin ?? item.repsMax ?? 0;
  const max = item.repsMax ?? min;
  return min === max ? String(min) : `${min}–${max}`;
}

function workoutDescription(title: string): string {
  return `Entrenamiento enfocado en desarrollar fuerza en ${title.toLocaleLowerCase('es-ES')}. Mantén una técnica controlada y aumenta las cargas de forma progresiva.`;
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
      const [workout, muscleMap] = await Promise.all([
        getWorkout(session.workoutType),
        getMuscleMap(session.id),
      ]);
      setState({ status: 'ready', session, workout, muscleMap });
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
  const muscles = groupMusclesForDisplay(state.muscleMap.muscles);
  const muscleTotal = muscles.reduce((total, muscle) => total + LOAD_WEIGHT[muscle.load], 0);
  const muscleSlices = muscles.map((muscle) => ({
    ...muscle,
    percentage: muscleTotal > 0 ? Math.round((LOAD_WEIGHT[muscle.load] / muscleTotal) * 100) : 0,
  }));

  return (
    <div className={styles.page}>
      <DetailTopbar />

      <div className={styles.layout}>
        <main className={styles.main}>
          <Card className={styles.hero}>
            <div className={styles.heroCopy}>
              <Badge tone="neutral">Fuerza</Badge>
              <div className={styles.titleRow}>
                <h1>{title}</h1>
                <StatusPill kind="training" value={state.session.status} />
              </div>
              <p className={styles.description}>{workoutDescription(title)}</p>
              <dl className={styles.metrics}>
                <Metric icon="activity" label="Duración" value={`${ESTIMATED_DURATION_MIN} min`} />
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
              <span className={styles.sectionStatus}>
                <Icon name={completed ? 'checkCircle' : 'activity'} size={17} />
                {completed ? 'Completado' : 'Pendiente'}
              </span>
            </header>
            <ol className={styles.exerciseList}>
              {state.workout.items.map((item) => (
                <ExerciseCard key={item.exerciseId} item={item} completed={completed} />
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
              value={completed ? 1 : 0}
              max={1}
              size={128}
              label={completed ? 'Entrenamiento completado' : 'Entrenamiento pendiente'}
            >
              <strong className={styles.progressPercent}>{completed ? 100 : 0}%</strong>
            </ProgressRing>
            <strong className={styles.progressMessage}>
              {completed ? '¡Entrenamiento completado!' : 'Entrenamiento pendiente'}
            </strong>
            <span>{state.workout.items.length} ejercicios</span>
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
    </div>
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

function muscleGradient(slices: readonly { percentage: number }[]): string {
  if (slices.length === 0) return 'var(--color-border)';
  const colors = [
    'var(--color-accent)',
    'var(--color-info)',
    'var(--color-warning-graphic)',
    'var(--color-secondary)',
  ];
  let cursor = 0;
  const segments = slices.map((slice, index) => {
    const start = cursor;
    cursor += slice.percentage;
    return `${colors[index % colors.length]} ${start}% ${Math.min(cursor, 100)}%`;
  });
  return `conic-gradient(${segments.join(', ')})`;
}

function Metric({
  icon,
  label,
  value,
}: {
  readonly icon: 'activity' | 'training' | 'progress';
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
  completed,
}: {
  readonly item: WorkoutItem;
  readonly completed: boolean;
}) {
  return (
    <li className={styles.exerciseCard}>
      <span className={styles.exerciseOrder}>{item.order}</span>
      <div className={styles.exerciseIdentity}>
        <Icon name="training" size={30} />
        <div>
          <h3>{item.exerciseName}</h3>
          <span>Fuerza · RIR {item.rir}</span>
          <small>Descanso: {item.restSeconds} s</small>
        </div>
      </div>
      <div className={styles.setTable} role="table" aria-label={`Series de ${item.exerciseName}`}>
        <div className={styles.setHeader} role="row">
          <span>Serie</span>
          <span>Peso</span>
          <span>Reps objetivo</span>
          <span>Estado</span>
        </div>
        {Array.from({ length: item.sets }, (_, index) => (
          <div key={index} className={styles.setRow} role="row">
            <span>{index + 1}</span>
            <span aria-label="Sin peso registrado">—</span>
            <span>{repTarget(item)}</span>
            <Icon name={completed ? 'checkCircle' : 'activity'} size={16} />
          </div>
        ))}
      </div>
    </li>
  );
}
