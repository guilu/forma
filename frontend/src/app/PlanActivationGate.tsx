import { useEffect, useState } from 'react';
import { PlanActivationModal } from '../components/PlanActivationModal';
import { acceptPlan, getPlanAcceptance } from '../api/planAcceptance';

/**
 * Asks, once per session, whether to start the plan the account was given (V58).
 *
 * <p>Mounted in the shell rather than on a page, because the answer changes three screens at once —
 * training, nutrition and the shopping list all read the plan it switches on.
 *
 * <p><b>A failed check asks nothing.</b> If the request errors the app carries on silently: a modal
 * is a door in front of everything else, and one raised by a network blip would be a door with no
 * handle. The screens' own empty states already cover "no plan", which is what the user would see.
 *
 * <p>Declining stores nothing, so the question returns next session. That is deliberate: an account
 * that never starts its plan should keep being asked rather than be left with three blank screens
 * and no way back to them.
 */
interface PlanActivationGateProps {
  /** Called after a successful activation so the shell can re-read the screens behind the modal. */
  readonly onActivated: () => void;
}

type State =
  | { readonly status: 'checking' }
  | { readonly status: 'closed' }
  | { readonly status: 'offering'; readonly planName?: string }
  | { readonly status: 'activating'; readonly planName?: string }
  | { readonly status: 'failed'; readonly planName?: string };

export function PlanActivationGate({ onActivated }: PlanActivationGateProps) {
  const [state, setState] = useState<State>({ status: 'checking' });

  useEffect(() => {
    let active = true;
    getPlanAcceptance()
      .then((acceptance) => {
        if (active) {
          setState(
            acceptance.pending
              ? { status: 'offering', planName: acceptance.planName }
              : { status: 'closed' },
          );
        }
      })
      .catch(() => {
        if (active) {
          setState({ status: 'closed' });
        }
      });
    return () => {
      active = false;
    };
  }, []);

  if (state.status === 'checking' || state.status === 'closed') {
    return null;
  }

  const accept = () => {
    setState({ status: 'activating', planName: state.planName });
    acceptPlan()
      .then(() => {
        setState({ status: 'closed' });
        onActivated();
      })
      .catch(() => setState({ status: 'failed', planName: state.planName }));
  };

  return (
    <PlanActivationModal
      planName={state.planName}
      pending={state.status === 'activating'}
      error={
        state.status === 'failed' ? 'No se pudo activar tu plan. Inténtalo de nuevo.' : undefined
      }
      onAccept={accept}
      onDismiss={() => setState({ status: 'closed' })}
    />
  );
}
