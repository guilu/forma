import { useEffect, useState } from 'react';
import { ErrorState } from '../../components/ErrorState';
import { ProgressRing } from '../../components/ProgressRing';
import { WidgetLoading } from '../../components/WidgetLoading';
import { listBodyMeasurements } from '../../api/bodyMeasurements';
import { WidgetSection } from './WidgetSection';
import styles from './FirstSummaryWidget.module.css';

/**
 * "Tu primer resumen" onboarding celebration (FOR-164 dashboard mockup): a
 * warm confirmation that the user has recorded their first measurement, with a
 * ring showing how many measurements are logged. The count is real (FOR-17
 * measurement history length); the encouraging copy is static.
 *
 * <p>When there are no measurements yet, it nudges the user to record their
 * first one instead of celebrating a zero (honest empty framing).
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly count: number };

export function FirstSummaryWidget() {
  const [state, setState] = useState<State>({ status: 'loading' });

  useEffect(() => {
    let active = true;
    listBodyMeasurements()
      .then((measurements) => {
        if (active) setState({ status: 'ready', count: measurements.length });
      })
      .catch(() => {
        if (active) setState({ status: 'error' });
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <WidgetSection id="first-summary-widget-title" title="Tu primer resumen">
      {renderContent(state)}
    </WidgetSection>
  );
}

function renderContent(state: State) {
  if (state.status === 'loading') {
    return <WidgetLoading label="Cargando tu resumen…" rows={2} />;
  }
  if (state.status === 'error') {
    return <ErrorState message="No se pudo cargar tu resumen. Inténtalo de nuevo más tarde." />;
  }

  const { count } = state;
  const registered = count > 0;
  const noun = count === 1 ? 'medición registrada' : 'mediciones registradas';

  return (
    <div className={styles.card}>
      <div className={styles.text}>
        <p className={styles.heading}>{registered ? '¡Buen trabajo!' : '¡Empecemos!'}</p>
        <p className={styles.body}>
          {registered
            ? 'Has completado tu configuración y registrado tu primera medición.'
            : 'Registra tu primera medición para empezar a ver tu progreso.'}
        </p>
        <p className={styles.body}>Sigue así, la constancia es la clave del progreso.</p>
      </div>
      <ProgressRing
        value={count}
        max={Math.max(count, 1)}
        label={`${count} ${noun}`}
        size={104}
      >
        <span className={styles.ringValue}>{count}</span>
        <span className={styles.ringUnit}>{count === 1 ? 'de 1' : `de ${count}`}</span>
      </ProgressRing>
    </div>
  );
}
