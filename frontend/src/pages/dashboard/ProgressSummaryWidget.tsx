import { useEffect, useState } from 'react';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { ProgressRing } from '../../components/ProgressRing';
import { WidgetLoading } from '../../components/WidgetLoading';
import { listGoals, type Goal, type GoalMetric } from '../../api/goals';
import { WidgetSection } from './WidgetSection';
import styles from './ProgressSummaryWidget.module.css';

/**
 * "Tu progreso" widget (FOR-164 dashboard 7-measurement variant): the user's
 * primary goal and how close they are, from the FOR-125 goals API
 * (`GET /api/v1/goals`).
 *
 * <p>Progress is NEVER computed here (spec FOR-122 / architecture-overview.md:
 * "No goal-progress math client-side"): `progress.ratio` is the backend-derived
 * fraction, and it is explicitly `null` when the metric has no linked data yet
 * — rendered as a neutral "sin datos" state, never a fabricated 0%. With no
 * goals at all, it nudges the user to create one (the onboarding path this card
 * shows in the sparse "1-measurement" variant).
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly goals: readonly Goal[] };

const METRIC_UNIT: Record<GoalMetric, string> = {
  WEIGHT_KG: 'kg',
  LEAN_MASS_KG: 'kg',
  BODY_FAT_PCT: '%',
};

/** Picks the goal to feature: first ACTIVE (weight preferred), else the first. */
function primaryGoal(goals: readonly Goal[]): Goal | undefined {
  const active = goals.filter((g) => g.status === 'ACTIVE');
  return active.find((g) => g.metric === 'WEIGHT_KG') ?? active[0] ?? goals[0] ?? undefined;
}

export function ProgressSummaryWidget() {
  const [state, setState] = useState<State>({ status: 'loading' });

  useEffect(() => {
    let active = true;
    listGoals()
      .then((goals) => {
        if (active) setState({ status: 'ready', goals });
      })
      .catch(() => {
        if (active) setState({ status: 'error' });
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <WidgetSection id="progress-summary-widget-title" title="Tu progreso" linkTo="/objetivos">
      {renderContent(state)}
    </WidgetSection>
  );
}

function renderContent(state: State) {
  if (state.status === 'loading') {
    return <WidgetLoading label="Cargando tu progreso…" rows={2} />;
  }
  if (state.status === 'error') {
    return <ErrorState message="No se pudo cargar tu progreso. Inténtalo de nuevo más tarde." />;
  }

  const goal = primaryGoal(state.goals);
  if (!goal) {
    return (
      <EmptyState
        variant="filtered"
        title="Define un objetivo para seguir tu progreso semana a semana."
      />
    );
  }

  const unit = METRIC_UNIT[goal.metric];
  const hasProgress = goal.progress.ratio != null;
  const percent = hasProgress ? Math.round((goal.progress.ratio as number) * 100) : 0;
  const doneMilestones = goal.milestones.filter((m) => m.completed).length;

  return (
    <div className={styles.card}>
      <div className={styles.text}>
        <p className={styles.heading}>¡Vas por muy buen camino!</p>
        <p className={styles.body}>{goal.title}</p>
        <p className={styles.body}>
          Objetivo: {goal.target} {unit}
        </p>
        {goal.milestones.length > 0 && (
          <p className={styles.body}>
            {doneMilestones} de {goal.milestones.length} hitos completados
          </p>
        )}
      </div>
      <ProgressRing
        value={percent}
        max={100}
        label={
          hasProgress
            ? `Progreso del objetivo: ${percent}%`
            : 'Progreso del objetivo: sin datos todavía'
        }
        size={104}
      >
        {hasProgress ? (
          <span className={styles.ringValue}>{percent}%</span>
        ) : (
          <span className={styles.ringHint}>Sin datos</span>
        )}
      </ProgressRing>
    </div>
  );
}
