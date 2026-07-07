import { useEffect, useState } from 'react';
import { Card } from '../components/Card';
import { getTrainingWeek, type TrainingDay, type TrainingWeek } from '../api/training';
import styles from './TrainingPage.module.css';

/**
 * Training page (FOR-26). Shows the current week's training calendar — running and strength
 * sessions on their days, with rest days — from the FOR-26 API (`/api/v1/training/week`). Read-only;
 * marking sessions complete is FOR-27. Renders the API read model directly (ADR-006). Handles
 * loading, empty and error states.
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

export function TrainingPage() {
  const [state, setState] = useState<State>({ status: 'loading' });

  useEffect(() => {
    let active = true;
    getTrainingWeek()
      .then((week) => {
        if (active) {
          setState({ status: 'ready', week });
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
  }, []);

  return (
    <div className={styles.wrapper}>
      <header className={styles.header}>
        <h1 className={styles.title}>Entrenamiento</h1>
        <p className={styles.subtitle}>Tu semana de entrenamiento.</p>
      </header>
      {renderContent(state)}
    </div>
  );
}

function renderContent(state: State) {
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
        <DayCard key={day.dayOfWeek} day={day} />
      ))}
    </section>
  );
}

function DayCard({ day }: { readonly day: TrainingDay }) {
  return (
    <Card title={DAY_LABELS[day.dayOfWeek] ?? day.dayOfWeek}>
      {day.rest ? (
        <p className={styles.rest}>Descanso</p>
      ) : (
        <ul className={styles.sessions}>
          {day.sessions.map((session, index) => (
            <li key={index} className={styles.session}>
              <span className={styles.kind} data-kind={session.kind}>
                {session.kind === 'RUNNING' ? 'Carrera' : 'Fuerza'}
              </span>
              <span className={styles.sessionTitle}>{session.title}</span>
              <span className={styles.sessionDetail}>{session.detail}</span>
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}
