import { useEffect, useState } from 'react';
import { getWeeklyInsights, type WeeklyInsights } from '../../api/insights';
import { ErrorState } from '../../components/ErrorState';
import { StatusPill } from '../../components/StatusPill';
import { WidgetLoading } from '../../components/WidgetLoading';
import { WidgetSection } from './WidgetSection';
import styles from './TipWidget.module.css';

/**
 * Dashboard's highlighted recommendation. It renders the weekly insight read
 * model verbatim: recommendation priority and the insufficient-data fallback
 * both belong to the backend, never to this presentation component.
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly insights: WeeklyInsights };

export function TipWidget() {
  const [state, setState] = useState<State>({ status: 'loading' });

  useEffect(() => {
    let active = true;
    getWeeklyInsights()
      .then((insights) => {
        if (active) setState({ status: 'ready', insights });
      })
      .catch(() => {
        if (active) setState({ status: 'error' });
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <WidgetSection id="tip-widget-title" title="Recomendación destacada">
      {renderContent(state)}
    </WidgetSection>
  );
}

function renderContent(state: State) {
  if (state.status === 'loading') {
    return <WidgetLoading label="Cargando tu recomendación…" rows={2} />;
  }

  if (state.status === 'error') {
    return (
      <ErrorState message="No se pudo cargar tu recomendación. Inténtalo de nuevo más tarde." />
    );
  }

  const { main } = state.insights;

  return (
    <div className={styles.card}>
      <StatusPill kind="severity" value={main.severity} />
      <p className={styles.recommendation}>{main.message}</p>
      <p className={styles.reason}>{main.reason}</p>
    </div>
  );
}
