import { useEffect, useState } from 'react';
import { Card } from '../../components/Card';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { WidgetLoading } from '../../components/WidgetLoading';
import { getTrainingWeek, type TrainingSession, type TrainingWeek } from '../../api/training';
import { WidgetSection } from './WidgetSection';
import { ProgressBar } from './ProgressBar';
import styles from './TrainingWidget.module.css';

/**
 * Weekly training status widget (FOR-51): the next planned session plus how many of
 * the week's sessions are completed, from the FOR-26 training-week read model. Renders
 * the API data as returned (ADR-006).
 *
 * <p>The "completed / total" count here is a plain tally over the sessions already
 * returned by `GET /training/week` (all kinds combined) — display aggregation, not the
 * FOR-28 `WeeklyTrainingSummary` domain calculation (which splits by session kind and
 * sums running distance, and is not exposed over HTTP). Documented simplification, see
 * FOR-51 PR "Known limitations".
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'empty' }
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

export function TrainingWidget() {
  const [state, setState] = useState<State>({ status: 'loading' });

  useEffect(() => {
    let active = true;
    getTrainingWeek()
      .then((week) => {
        if (!active) return;
        const hasAnySession = week.days.some((day) => day.sessions.length > 0);
        setState(hasAnySession ? { status: 'ready', week } : { status: 'empty' });
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
    <WidgetSection id="training-widget-title" title="Entrenamiento" linkTo="/entrenamiento">
      {renderContent(state)}
    </WidgetSection>
  );
}

function renderContent(state: State) {
  if (state.status === 'loading') {
    return <WidgetLoading label="Cargando tu semana de entrenamiento…" rows={2} />;
  }

  if (state.status === 'error') {
    return (
      <ErrorState message="No se pudo cargar tu entrenamiento. Inténtalo de nuevo más tarde." />
    );
  }

  if (state.status === 'empty') {
    return (
      <EmptyState variant="filtered" title="No hay entrenamientos planificados esta semana." />
    );
  }

  const allSessions: TrainingSession[] = state.week.days.flatMap((day) => day.sessions);
  const completed = allSessions.filter((s) => s.status === 'COMPLETED').length;
  const total = allSessions.length;

  const next = state.week.days
    .flatMap((day) => day.sessions.map((session) => ({ day: day.dayOfWeek, session })))
    .find(({ session }) => session.status === 'PLANNED');

  return (
    <div className={styles.content}>
      <Card title="Próximo entrenamiento">
        {next ? (
          <p className={styles.next}>
            <span className={styles.nextDay}>{DAY_LABELS[next.day] ?? next.day}</span>
            <span className={styles.nextTitle}>{next.session.title}</span>
            <span className={styles.nextDetail}>{next.session.detail}</span>
          </p>
        ) : (
          <p className={styles.message}>No tienes entrenamientos pendientes esta semana.</p>
        )}
      </Card>
      <div className={styles.completion}>
        <span className={styles.completionLabel}>
          {completed} de {total} sesiones completadas
        </span>
        <ProgressBar value={completed} max={total} label="Sesiones completadas esta semana" />
      </div>
    </div>
  );
}
