import { useEffect, useState } from 'react';
import { Card } from '../../components/Card';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { LoadingState } from '../../components/LoadingState';
import { StatusPill } from '../../components/StatusPill';
import { getInsightsHistory, type WeeklyInsights } from '../../api/insights';
import { WeeklyInsightsDetail } from './WeeklyInsightsDetail';
import styles from './InsightsHistorySection.module.css';

/**
 * Past-weeks insights history view (FOR-124), closing the gap
 * `InsightsSection`'s original doc comment named: "a 'historical insights
 * list' is deferred entirely rather than built against nothing." Consumes
 * FOR-110's {@code GET /api/v1/insights/history}, which returns every
 * persisted period, most recent first — rendered in that order as-is,
 * without any client-side re-sorting (ADR-006).
 *
 * <p>Lives inside Progreso as a sibling section below the current-week
 * `InsightsSection`, preserving FOR-56's "no new nav item" decision (the nav
 * has no dedicated "Insights" route). Fetches independently from
 * `InsightsSection`, so a history failure never blocks the current week's
 * recommendations or vice versa (FOR-60 loading/empty/error states, scoped
 * per section).
 *
 * <p>Selecting a period renders its full insights via the shared {@link
 * WeeklyInsightsDetail} (FOR-124) — the same main/secondary/signals/
 * disclaimer rendering `InsightsSection` uses for the current week, reused
 * verbatim for a historical `WeeklyInsights` payload (spec FOR-124: "mirrors
 * the current week's existing rendering").
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly history: WeeklyInsights[] };

const HISTORY_DETAIL_ID = 'insights-history-detail';

function formatPeriodLabel(weekStartDate: string): string {
  const date = new Date(`${weekStartDate}T00:00:00`);
  return `Semana del ${date.toLocaleDateString('es-ES', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  })}`;
}

export function InsightsHistorySection() {
  const [state, setState] = useState<State>({ status: 'loading' });
  const [reloadToken, setReloadToken] = useState(0);
  const [selectedIndex, setSelectedIndex] = useState<number | null>(null);

  useEffect(() => {
    let active = true;
    setState({ status: 'loading' });
    setSelectedIndex(null);
    getInsightsHistory()
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
    <section className={styles.wrapper} aria-labelledby="insights-history-title">
      <h2 id="insights-history-title" className={styles.title}>
        Semanas anteriores
      </h2>
      {renderContent(state, () => setReloadToken((n) => n + 1), selectedIndex, setSelectedIndex)}
    </section>
  );
}

function renderContent(
  state: State,
  onRetry: () => void,
  selectedIndex: number | null,
  onSelect: (index: number | null) => void,
) {
  if (state.status === 'loading') {
    return <LoadingState message="Cargando semanas anteriores…" />;
  }

  if (state.status === 'error') {
    return (
      <ErrorState
        message="No se pudo cargar el historial de recomendaciones. Inténtalo de nuevo."
        onRetry={onRetry}
      />
    );
  }

  if (state.history.length === 0) {
    return <EmptyState title="Aún no hay semanas anteriores registradas." />;
  }

  const selected = selectedIndex !== null ? state.history[selectedIndex] : undefined;

  return (
    <div className={styles.content}>
      <Card title="Historial de recomendaciones">
        <ul className={styles.list}>
          {state.history.map((period, index) => {
            const isSelected = selectedIndex === index;
            return (
              <li key={period.checkIn.weekStartDate}>
                <button
                  type="button"
                  className={styles.listItem}
                  aria-expanded={isSelected}
                  aria-controls={HISTORY_DETAIL_ID}
                  onClick={() => onSelect(isSelected ? null : index)}
                >
                  <span className={styles.period}>
                    {formatPeriodLabel(period.checkIn.weekStartDate)}
                  </span>
                  <span className={styles.summary}>
                    <StatusPill kind="severity" value={period.main.severity} />
                    <span className={styles.summaryMessage}>{period.main.message}</span>
                  </span>
                </button>
              </li>
            );
          })}
        </ul>
      </Card>

      {selected && (
        <div id={HISTORY_DETAIL_ID} className={styles.detail}>
          <WeeklyInsightsDetail insights={selected} />
        </div>
      )}
    </div>
  );
}
