import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { MetricCard } from '../components/MetricCard';
import { listBodyMeasurements, type BodyMeasurement } from '../api/bodyMeasurements';
import styles from './DashboardPage.module.css';

/**
 * Dashboard page (FOR-19). Shows body-composition metric cards for the latest
 * measurement (weight, body fat %, fat mass, lean mass, BMI) from the FOR-17 API,
 * following the card style in docs/1-dashboard.png. Derived values are read from
 * the API, never recomputed (ADR-006). Handles loading, empty and error states.
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly latest: BodyMeasurement | undefined };

/** One decimal, avoiding fake precision; a missing value shows a clear marker. */
function format(value: number | undefined): string {
  return value === undefined ? '—' : value.toFixed(1);
}

export function DashboardPage() {
  const [state, setState] = useState<State>({ status: 'loading' });

  useEffect(() => {
    let active = true;
    listBodyMeasurements()
      .then((measurements) => {
        if (active) {
          // The list is newest-first (FOR-16/FOR-17), so the first item is latest.
          setState({ status: 'ready', latest: measurements[0] });
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
        <h1 className={styles.title}>Dashboard</h1>
        <p className={styles.subtitle}>Este es tu resumen de hoy.</p>
      </header>
      {renderContent(state)}
    </div>
  );
}

function renderContent(state: State) {
  if (state.status === 'loading') {
    return (
      <p className={styles.message} role="status">
        Cargando tus mediciones…
      </p>
    );
  }

  if (state.status === 'error') {
    return (
      <p className={styles.message} role="alert">
        No se pudieron cargar tus mediciones. Inténtalo de nuevo más tarde.
      </p>
    );
  }

  if (!state.latest) {
    return (
      <p className={styles.message} role="status">
        Aún no hay mediciones. <Link to="/mediciones">Registra tu primera medición</Link> para ver
        tu resumen.
      </p>
    );
  }

  const latest = state.latest;
  return (
    <section className={styles.grid} aria-label="Composición corporal (última medición)">
      <MetricCard label="Peso" value={format(latest.weightKg)} unit="kg" />
      <MetricCard label="Grasa corporal" value={format(latest.bodyFatPercentage)} unit="%" />
      <MetricCard label="Masa grasa" value={format(latest.fatMassKg)} unit="kg" />
      <MetricCard label="Masa magra" value={format(latest.leanMassKg)} unit="kg" />
      <MetricCard label="IMC" value={format(latest.bmi)} />
    </section>
  );
}
