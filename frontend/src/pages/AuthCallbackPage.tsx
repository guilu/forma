import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Button } from '../components/Button';
import { ErrorState } from '../components/ErrorState';
import { LoadingState } from '../components/LoadingState';
import { useNotify } from '../components/NotificationProvider';
import { ApiRequestError } from '../api/client';
import {
  completeIntegrationCallback,
  connectIntegration,
  isAuthorizationRequired,
} from '../api/integrations';
import styles from './AuthCallbackPage.module.css';

/**
 * OAuth callback landing page (FOR-133), mounted at `/auth` — the SPA route
 * registered as Withings' OAuth2 redirect URL (spec FOR-131:
 * `https://forma.diegobarrioh.dev/auth`). Withings redirects the browser
 * here with `?code&state` after the user authorizes; this page relays them
 * to the backend callback ({@link completeIntegrationCallback}) to complete
 * the token exchange, then lands the user back on Integraciones.
 *
 * <p><b>Withings-specific by default</b> (spec FOR-133 Open Questions,
 * resolved conservatively): Withings is the only provider with a registered
 * OAuth gateway this slice (FOR-131), and its redirect carries no `provider`
 * query param to key a provider-aware route off — so this page targets
 * Withings directly rather than inventing a generic multi-provider `/auth`.
 * Revisit if a second provider's OAuth gateway ships and needs this route
 * too.
 *
 * <p>The SPA only ever handles `code`/`state` in transit — never a token,
 * never persisted (no `localStorage`/`sessionStorage` write) — matching
 * {@link completeIntegrationCallback}'s own contract.
 *
 * <p>Effect double-invocation guard: React 19 StrictMode (see `main.tsx`)
 * runs mount effects twice in development. `ranRef` ensures the callback is
 * only ever POSTed once per successful landing (spec ui.md: "runs the
 * callback once on mount").
 */
const INTEGRATIONS_PATH = '/ajustes/integraciones';
const PROVIDER = 'WITHINGS';
const GENERIC_ERROR_MESSAGE = 'No se pudo completar la conexión con Withings. Inténtalo de nuevo.';
const MISSING_PARAMS_MESSAGE =
  'Falta información necesaria para completar la conexión con Withings.';

type CallbackState =
  | { readonly status: 'loading' }
  | { readonly status: 'nothing-to-complete' }
  | { readonly status: 'error'; readonly message: string };

function messageFromError(error: unknown): string {
  return error instanceof ApiRequestError ? error.message : GENERIC_ERROR_MESSAGE;
}

function initialState(
  hasCodeParam: boolean,
  hasStateParam: boolean,
  code: string,
  state: string,
): CallbackState {
  // Direct visit — no OAuth redirect landed here at all. Nothing to relay,
  // and calling the backend with junk would be wrong (spec Edge Cases).
  if (!hasCodeParam && !hasStateParam) {
    return { status: 'nothing-to-complete' };
  }
  const hasCode = code.trim().length > 0;
  const hasState = state.trim().length > 0;
  if (!hasCode || !hasState) {
    return { status: 'error', message: MISSING_PARAMS_MESSAGE };
  }
  return { status: 'loading' };
}

export function AuthCallbackPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const notify = useNotify();
  const ranRef = useRef(false);

  const code = searchParams.get('code') ?? '';
  const state = searchParams.get('state') ?? '';
  const [callbackState, setCallbackState] = useState<CallbackState>(() =>
    initialState(searchParams.has('code'), searchParams.has('state'), code, state),
  );

  useEffect(() => {
    if (callbackState.status !== 'loading' || ranRef.current) {
      return;
    }
    ranRef.current = true;

    completeIntegrationCallback(PROVIDER, code, state)
      .then(() => {
        notify.success('Conectado con Withings.');
        navigate(INTEGRATIONS_PATH, { replace: true });
      })
      .catch((error: unknown) => {
        setCallbackState({ status: 'error', message: messageFromError(error) });
      });
    // `code`/`state` are stable for the lifetime of this landing (from the
    // URL at mount); re-running on navigate/notify identity changes is not
    // desired — `ranRef` is the actual guard.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [callbackState.status]);

  useEffect(() => {
    if (callbackState.status !== 'nothing-to-complete') {
      return;
    }
    navigate(INTEGRATIONS_PATH, { replace: true });
  }, [callbackState.status, navigate]);

  /**
   * "Volver a intentar" re-initiates the OAuth flow from scratch rather than
   * re-POSTing the same `code`/`state` — the backend's OAuth state challenge
   * is single-use and already consumed by the failed attempt (`IntegrationService.callback`
   * doc comment: "a failed exchange cannot be retried with the same
   * code/state — the caller must restart the connect flow"), so retrying the
   * same values would just fail again with the same error.
   */
  async function handleRetryConnect() {
    setCallbackState({ status: 'loading' });
    ranRef.current = true; // this retry IS the one call — a re-render must not re-trigger the effect above
    try {
      const result = await connectIntegration(PROVIDER);
      if (isAuthorizationRequired(result)) {
        window.location.assign(result.authorizationUrl);
        return;
      }
      navigate(INTEGRATIONS_PATH, { replace: true });
    } catch (error) {
      setCallbackState({ status: 'error', message: messageFromError(error) });
    }
  }

  function handleBackToIntegrations() {
    navigate(INTEGRATIONS_PATH);
  }

  return (
    <div className={styles.wrapper}>
      <h1 className={styles.title}>Conexión con Withings</h1>
      {callbackState.status === 'loading' && (
        <LoadingState message="Completando conexión con Withings…" />
      )}
      {callbackState.status === 'nothing-to-complete' && (
        <p role="status" className={styles.message}>
          No hay nada que completar. Volviendo a Integraciones…
        </p>
      )}
      {callbackState.status === 'error' && (
        <div className={styles.errorWrapper}>
          <ErrorState
            message={callbackState.message}
            onRetry={handleRetryConnect}
            retryLabel="Volver a intentar"
          />
          <Button variant="ghost" type="button" onClick={handleBackToIntegrations}>
            Volver a Integraciones
          </Button>
        </div>
      )}
    </div>
  );
}
