import { useEffect, useState } from 'react';
import { Card } from '../../components/Card';
import { Button } from '../../components/Button';
import { StatusPill } from '../../components/StatusPill';
import { getWeeklyInsights, type WeeklyCheckIn, type WeeklyInsights } from '../../api/insights';
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
 * <p>Reuses {@link StatusPill} (FOR-50) for severity so the badge copy/tone
 * matches the dashboard widget exactly (docs/api/weekly-insights.md,
 * ui-guidelines.md "always explain recommendations" / "no fake AI oracle
 * energy"). Reason/explanation text is always the backend's `reason` field —
 * never synthesized here (ADR-006: frontend renders read models, not domain
 * rules).
 *
 * <p>"Related signals" render the check-in's latest absolute values (weight,
 * body fat %, lean mass) and training completion counts — there is no
 * week-over-week "delta" field on `WeeklyCheckIn`
 * (docs/api/weekly-insights.md), so this shows the signals the recommendation
 * was computed from, not a trend. Mirrors the FOR-51 `BodyWidget` precedent of
 * rendering only what the API actually returns instead of recomputing a
 * comparison in the UI.
 *
 * <p>No persisted insight history exists — FOR-45 is computed on demand with
 * no history endpoint (spec `specs/FOR-56/spec.md` Data Model Notes) — so a
 * "historical insights list" is deferred entirely rather than built against
 * nothing. This section always shows the current week only.
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly insights: WeeklyInsights };

const DISCLAIMER =
  'Estas recomendaciones son orientativas y se generan a partir de tus datos. No sustituyen el diagnóstico ni el consejo de un profesional sanitario.';

function formatBody(value: number | undefined, unit: string): string | undefined {
  return value === undefined ? undefined : `${value.toFixed(1)} ${unit}`;
}

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
    return (
      <p className={styles.message} role="status">
        Cargando tus recomendaciones…
      </p>
    );
  }

  if (state.status === 'error') {
    return (
      <div className={styles.errorBox} role="alert">
        <p className={styles.message}>
          No se pudieron cargar tus recomendaciones. Inténtalo de nuevo.
        </p>
        <Button variant="secondary" onClick={onRetry}>
          Reintentar
        </Button>
      </div>
    );
  }

  const { checkIn, main, secondary } = state.insights;

  return (
    <div className={styles.content}>
      <div className={styles.topRow}>
        <Card title="Recomendación principal">
          <StatusPill kind="severity" value={main.severity} />
          <p className={styles.mainMessage}>{main.message}</p>
          <p className={styles.reason}>{main.reason}</p>
        </Card>

        <RelatedSignals checkIn={checkIn} />
      </div>

      {secondary.length > 0 && (
        <Card title="Otras recomendaciones">
          <ul className={styles.secondaryList}>
            {secondary.map((rec, index) => (
              <li key={`${rec.category}-${index}`} className={styles.secondaryItem}>
                <StatusPill kind="severity" value={rec.severity} />
                <div>
                  <p className={styles.secondaryMessage}>{rec.message}</p>
                  <p className={styles.reason}>{rec.reason}</p>
                </div>
              </li>
            ))}
          </ul>
        </Card>
      )}

      <p className={styles.disclaimer}>{DISCLAIMER}</p>
    </div>
  );
}

function RelatedSignals({ checkIn }: { readonly checkIn: WeeklyCheckIn }) {
  const bodySignals = [
    { label: 'Peso', value: formatBody(checkIn.latestWeightKg, 'kg') },
    { label: 'Grasa corporal', value: formatBody(checkIn.latestBodyFatPercentage, '%') },
    { label: 'Masa magra', value: formatBody(checkIn.latestLeanMassKg, 'kg') },
  ].filter((signal): signal is { label: string; value: string } => signal.value !== undefined);

  return (
    <Card title="Señales de esta semana">
      <ul className={styles.signalsList}>
        {bodySignals.map((signal) => (
          <li key={signal.label} className={styles.signalItem}>
            <span className={styles.signalLabel}>{signal.label}</span>
            <span className={styles.signalValue}>{signal.value}</span>
          </li>
        ))}
        <li className={styles.signalItem}>
          <span className={styles.signalLabel}>Entrenamiento running</span>
          <span className={styles.signalValue}>
            {checkIn.completedRunningSessions} de {checkIn.plannedRunningSessions} sesiones
          </span>
        </li>
        <li className={styles.signalItem}>
          <span className={styles.signalLabel}>Entrenamiento de fuerza</span>
          <span className={styles.signalValue}>
            {checkIn.completedStrengthSessions} de {checkIn.plannedStrengthSessions} sesiones
          </span>
        </li>
      </ul>
    </Card>
  );
}
