import { useEffect, useState } from 'react';
import { StatusPill } from '../../components/StatusPill';
import { getWeeklyInsights, type WeeklyInsights } from '../../api/insights';
import { WidgetSection } from './WidgetSection';
import styles from './InsightWidget.module.css';

/**
 * Latest insight/recommendation widget (FOR-51): the FOR-45 main recommendation
 * (message + reason), calm copy per docs/ui-guidelines.md. The backend always returns
 * `200` with a main recommendation — an insufficient-data `INFO` one when there isn't
 * enough underlying data yet (`docs/api/weekly-insights.md`) — so there is no distinct
 * "empty" widget state to build; that case renders through the normal `ready` path.
 *
 * <p>No dedicated insights feature page exists yet in the route table
 * (`frontend/src/app/routes.tsx` has no `/insights`/`/progreso`-linked insights view —
 * FOR-45 is API-only so far), so this widget has no "Ver más" link. Documented gap, see
 * FOR-51 PR "Known limitations".
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly insights: WeeklyInsights };

export function InsightWidget() {
  const [state, setState] = useState<State>({ status: 'loading' });

  useEffect(() => {
    let active = true;
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
  }, []);

  return (
    <WidgetSection id="insight-widget-title" title="Recomendación de la semana">
      {renderContent(state)}
    </WidgetSection>
  );
}

function renderContent(state: State) {
  if (state.status === 'loading') {
    return (
      <p className={styles.message} role="status">
        Cargando tu recomendación…
      </p>
    );
  }

  if (state.status === 'error') {
    return (
      <p className={styles.message} role="alert">
        No se pudo cargar tu recomendación. Inténtalo de nuevo más tarde.
      </p>
    );
  }

  const { main } = state.insights;

  return (
    <div className={styles.card}>
      <StatusPill kind="severity" value={main.severity} />
      <p className={styles.recommendationMessage}>{main.message}</p>
      <p className={styles.reason}>{main.reason}</p>
    </div>
  );
}
