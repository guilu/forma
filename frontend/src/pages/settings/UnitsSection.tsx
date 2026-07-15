import { useCallback, useEffect, useRef, useState } from 'react';
import { Card } from '../../components/Card';
import { ErrorState } from '../../components/ErrorState';
import { LoadingState } from '../../components/LoadingState';
import { ApiRequestError } from '../../api/client';
import { getProfile, type UnitPreferences, type UserProfile } from '../../api/profile';
import { SettingsRow } from './SettingsRow';

const UNITS_LOAD_ERROR = 'No se pudieron cargar tus unidades. Inténtalo de nuevo.';

const WEIGHT_LABELS: Record<UnitPreferences['weightUnit'], string> = { KG: 'Kilogramos (kg)' };
const HEIGHT_LABELS: Record<UnitPreferences['heightUnit'], string> = { CM: 'Centímetros (cm)' };
const DISTANCE_LABELS: Record<UnitPreferences['distanceUnit'], string> = { KM: 'Kilómetros (km)' };
const ENERGY_LABELS: Record<UnitPreferences['energyUnit'], string> = {
  KCAL: 'Kilocalorías (kcal)',
};

type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error'; readonly detail?: string }
  | { readonly status: 'ready'; readonly profile: UserProfile };

/**
 * Units & locale (FOR-58 FR: "peso (kg), altura (cm), distancia (km), energía
 * (kcal)"). FOR-119: now reads the real persisted preference from
 * {@code GET /api/v1/profile} instead of the static {@code UNIT_PREFERENCES}
 * fixture.
 *
 * <p>Still a read-only display, not a multi-option selector -- verified
 * against the backend source (`WeightUnit`/`HeightUnit`/`DistanceUnit`/
 * `EnergyUnit`), each of FOR-107's unit enums defines exactly one supported
 * value for the MVP (metric-only). There is no second option to switch to,
 * so this resolves FOR-119 spec's Open Question in favor of "a single
 * documented default reflecting FOR-107's persisted value", not a real
 * selector, since FOR-107 exposes no other option. Rendering a dropdown with
 * a single, permanently-selected option would be a fake control (AGENTS.md /
 * FOR-58: "never shows unsupported options as active"), so rows stay plain,
 * non-interactive {@link SettingsRow}s -- unchanged markup from FOR-58, now
 * backed by a real API call.
 */
export function UnitsSection() {
  const [state, setState] = useState<State>({ status: 'loading' });
  // Guards against a late-resolving fetch calling setState after unmount
  // (mirrors ShoppingPage's ProductEditModal `active` flag guard, FOR-113).
  const mountedRef = useRef(true);
  useEffect(
    () => () => {
      mountedRef.current = false;
    },
    [],
  );

  const load = useCallback(() => {
    setState({ status: 'loading' });
    getProfile()
      .then((profile) => {
        if (mountedRef.current) {
          setState({ status: 'ready', profile });
        }
      })
      .catch((error: unknown) => {
        if (mountedRef.current) {
          setState({
            status: 'error',
            detail: error instanceof ApiRequestError ? error.message : undefined,
          });
        }
      });
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <Card title="Unidades" headingLevel={2}>
      {state.status === 'loading' && <LoadingState message="Cargando tus unidades…" />}
      {state.status === 'error' && (
        <ErrorState
          message={UNITS_LOAD_ERROR}
          onRetry={load}
          detail={state.detail}
          showDetail={import.meta.env.DEV}
        />
      )}
      {state.status === 'ready' && (
        <>
          <SettingsRow
            label="Peso"
            value={WEIGHT_LABELS[state.profile.unitPreferences.weightUnit]}
          />
          <SettingsRow
            label="Altura"
            value={HEIGHT_LABELS[state.profile.unitPreferences.heightUnit]}
          />
          <SettingsRow
            label="Distancia"
            value={DISTANCE_LABELS[state.profile.unitPreferences.distanceUnit]}
          />
          <SettingsRow
            label="Energía"
            value={ENERGY_LABELS[state.profile.unitPreferences.energyUnit]}
          />
        </>
      )}
    </Card>
  );
}
