import { useEffect, useState } from 'react';
import { ErrorState } from '../../components/ErrorState';
import { LoadingState } from '../../components/LoadingState';
import { getWeeklyInsights, type WeeklyInsights } from '../../api/insights';
import { WeeklyInsightsDetail } from './WeeklyInsightsDetail';
import styles from './InsightsSection.module.css';

/**
 * Full insights & recommendations surface (FOR-56): the FOR-45 weekly `main`
 * recommendation (message + severity + reason), the check-in signals that fed
 * it, any `secondary` recommendations and a non-medical disclaimer. Lives
 * inside the Progreso page — the nav has no dedicated "Insights" section
 * (`frontend/src/app/navigation.ts` only has Progreso/Objetivos) — alongside
 * the dashboard's compact `InsightWidget` (FOR-51), which only renders the
 * main message/reason. This documents the FOR-56 spec's Open Question: no new
 * nav item/route is added for the MVP.
 *
 * <p>The actual "main/secondary/signals/disclaimer" rendering lives in the
 * shared {@link WeeklyInsightsDetail} (FOR-124), reused as-is by
 * `InsightsHistorySection` for a selected historical period. Reuses {@link
 * StatusPill} (FOR-50) for severity via that shared component so the badge
 * copy/tone matches the dashboard widget exactly (docs/api/weekly-insights.md,
 * ui-guidelines.md "always explain recommendations" / "no fake AI oracle
 * energy"). Reason/explanation text is always the backend's `reason` field —
 * never synthesized here (ADR-006: frontend renders read models, not domain
 * rules).
 *
 * <p>"Related signals" render the check-in's latest absolute values (weight,
 * body fat %, lean mass) and training completion counts, plus FOR-110's
 * week-over-week deltas alongside each when the backend provides one — a
 * gap this section's original doc comment explicitly named and FOR-124
 * closes. Mirrors the FOR-51 `BodyWidget` precedent of rendering only what
 * the API actually returns instead of recomputing a comparison in the UI.
 *
 * <p>The historical insights list this section's original doc comment
 * deferred ("FOR-45 is computed on demand with no history endpoint") is now
 * `InsightsHistorySection` (FOR-124), rendered as a sibling below this one on
 * the Progreso page — this section itself still shows the current week only.
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly insights: WeeklyInsights };

export function InsightsSection() {
  const [state, setState] = useState<State>({ status: 'loading' });
  const [reloadToken, setReloadToken] = useState(0);

  useEffect(() => {
    let active = true;
    setState({ status: 'loading' });
    getWeeklyInsights()
      .then((insights) => {
        if (active) {
          setState({ status: 'ready', insights });
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
    <section className={styles.wrapper} aria-labelledby="insights-section-title">
      <h2 id="insights-section-title" className={styles.title}>
        Recomendaciones de la semana
      </h2>
      {renderContent(state, () => setReloadToken((n) => n + 1))}
    </section>
  );
}

function renderContent(state: State, onRetry: () => void) {
  if (state.status === 'loading') {
    return <LoadingState message="Cargando tus recomendaciones…" />;
  }

  if (state.status === 'error') {
    return (
      <ErrorState
        message="No se pudieron cargar tus recomendaciones. Inténtalo de nuevo."
        onRetry={onRetry}
      />
    );
  }

  return (
    <div className={styles.content}>
      <WeeklyInsightsDetail insights={state.insights} />
    </div>
  );
}
