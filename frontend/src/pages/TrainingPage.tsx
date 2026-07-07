import { useCallback, useEffect, useState } from 'react';
import { Card } from '../components/Card';
import { ApiRequestError } from '../api/client';
import {
  getTrainingWeek,
  updateSessionStatus,
  type SessionStatus,
  type TrainingDay,
  type TrainingSession,
  type TrainingWeek,
} from '../api/training';
import styles from './TrainingPage.module.css';

/**
 * Training page (FOR-26/FOR-27). Shows the current week's calendar — running and strength sessions
 * on their days, with rest days — from the training API, and lets the user mark each session
 * completed or skipped (FOR-27). Renders the API read model directly (ADR-006); handles loading,
 * empty and error states.
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly week: TrainingWeek };

const DAY_LABELS: Record<string, string> = {
  MONDAY: 'Lunes',
  TUESDAY: 'Martes',
  WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves',
  FRIDAY: 'Viernes',
  SATURDAY: 'Sábado',
  SUNDAY: 'Domingo',
};

const STATUS_LABELS: Record<SessionStatus, string> = {
  PLANNED: 'Planificado',
  COMPLETED: 'Completado',
  SKIPPED: 'Saltado',
};

const MARK_ERROR = 'No se pudo actualizar la sesión. Inténtalo de nuevo.';

export function TrainingPage() {
  const [state, setState] = useState<State>({ status: 'loading' });
  const [actionError, setActionError] = useState<string | undefined>(undefined);
  const [pendingId, setPendingId] = useState<string | undefined>(undefined);

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
    } catch (error) {
      setActionError(error instanceof ApiRequestError ? error.message : MARK_ERROR);
    } finally {
      setPendingId(undefined);
    }
  }

  return (
    <div className={styles.wrapper}>
      <header className={styles.header}>
        <h1 className={styles.title}>Entrenamiento</h1>
        <p className={styles.subtitle}>Tu semana de entrenamiento.</p>
      </header>
      {actionError && (
        <p className={styles.actionError} role="alert">
          {actionError}
        </p>
      )}
      {renderContent(state, mark, pendingId)}
    </div>
  );
}

function renderContent(
  state: State,
  mark: (id: string, status: SessionStatus) => void,
  pendingId: string | undefined,
) {
  if (state.status === 'loading') {
    return (
      <p className={styles.message} role="status">
        Cargando tu semana…
      </p>
    );
  }

  if (state.status === 'error') {
    return (
      <p className={styles.message} role="alert">
        No se pudo cargar tu semana de entrenamiento. Inténtalo de nuevo más tarde.
      </p>
    );
  }

  const hasAnySession = state.week.days.some((day) => day.sessions.length > 0);
  if (!hasAnySession) {
    return (
      <p className={styles.message} role="status">
        No hay entrenamientos planificados esta semana.
      </p>
    );
  }

  return (
    <section className={styles.grid} aria-label="Calendario semanal de entrenamiento">
      {state.week.days.map((day) => (
        <DayCard key={day.dayOfWeek} day={day} mark={mark} pendingId={pendingId} />
      ))}
    </section>
  );
}

function DayCard({
  day,
  mark,
  pendingId,
}: {
  readonly day: TrainingDay;
  readonly mark: (id: string, status: SessionStatus) => void;
  readonly pendingId: string | undefined;
}) {
  return (
    <Card title={DAY_LABELS[day.dayOfWeek] ?? day.dayOfWeek}>
      {day.rest ? (
        <p className={styles.rest}>Descanso</p>
      ) : (
        <ul className={styles.sessions}>
          {day.sessions.map((session) => (
            <SessionItem
              key={session.id}
              session={session}
              mark={mark}
              pending={pendingId === session.id}
            />
          ))}
        </ul>
      )}
    </Card>
  );
}

function SessionItem({
  session,
  mark,
  pending,
}: {
  readonly session: TrainingSession;
  readonly mark: (id: string, status: SessionStatus) => void;
  readonly pending: boolean;
}) {
  return (
    <li className={styles.session}>
      <span className={styles.kind} data-kind={session.kind}>
        {session.kind === 'RUNNING' ? 'Carrera' : 'Fuerza'}
      </span>
      <span className={styles.sessionTitle}>{session.title}</span>
      <span className={styles.sessionDetail}>{session.detail}</span>
      <span className={styles.status} data-status={session.status}>
        {STATUS_LABELS[session.status]}
      </span>
      <div className={styles.actions}>
        {session.status !== 'COMPLETED' && (
          <button
            type="button"
            className={styles.mark}
            disabled={pending}
            onClick={() => mark(session.id, 'COMPLETED')}
          >
            Completar
          </button>
        )}
        {session.status !== 'SKIPPED' && (
          <button
            type="button"
            className={styles.markSecondary}
            disabled={pending}
            onClick={() => mark(session.id, 'SKIPPED')}
          >
            Saltar
          </button>
        )}
      </div>
    </li>
  );
}
