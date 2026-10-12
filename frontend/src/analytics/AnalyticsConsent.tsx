import { useEffect, useRef, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Button } from '../components/Button';
import {
  getAnalyticsConsent,
  grantAnalyticsConsent,
  initializeAnalyticsFromConsent,
  revokeAnalyticsConsent,
  trackPageView,
} from './analytics';
import styles from './AnalyticsConsent.module.css';

interface AnalyticsConsentProps {
  readonly measurementId?: string;
}

export function AnalyticsConsent({
  measurementId = import.meta.env.VITE_GA_MEASUREMENT_ID,
}: AnalyticsConsentProps) {
  const location = useLocation();
  const [decision, setDecision] = useState(getAnalyticsConsent);
  const [preferencesOpen, setPreferencesOpen] = useState(decision === null);
  const lastTrackedPath = useRef<string | null>(null);

  useEffect(() => {
    if (!measurementId || decision !== 'granted') {
      return;
    }
    initializeAnalyticsFromConsent(measurementId);
    if (lastTrackedPath.current !== location.pathname && trackPageView(location.pathname)) {
      lastTrackedPath.current = location.pathname;
    }
  }, [decision, location.pathname, measurementId]);

  function accept() {
    if (!measurementId) {
      return;
    }
    grantAnalyticsConsent(measurementId);
    setDecision('granted');
    setPreferencesOpen(false);
  }

  function reject() {
    revokeAnalyticsConsent();
    setDecision('denied');
    setPreferencesOpen(false);
    lastTrackedPath.current = null;
  }

  return (
    <>
      {preferencesOpen && (
        <section
          className={styles.dialog}
          role="dialog"
          aria-modal="true"
          aria-labelledby="analytics-consent-title"
        >
          <div className={styles.copy}>
            <h2 id="analytics-consent-title">Preferencias de analítica</h2>
            <p>
              Con tu permiso, usamos Google Analytics para saber qué páginas se visitan y mejorar
              Forma. No se carga Google Analytics hasta que aceptas. Puedes cambiar de opinión en
              cualquier momento. <Link to="/privacidad">Más información</Link>.
            </p>
          </div>
          <div className={styles.actions}>
            <Button variant="secondary" onClick={reject}>
              Rechazar analítica
            </Button>
            <Button variant="accent" onClick={accept} disabled={!measurementId}>
              Aceptar analítica
            </Button>
          </div>
        </section>
      )}
      {!preferencesOpen && (
        <Button
          className={styles.preferences}
          variant="ghost"
          onClick={() => setPreferencesOpen(true)}
        >
          Configurar analítica
        </Button>
      )}
    </>
  );
}
