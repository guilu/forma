import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Badge } from '../components/Badge';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { EmptyState } from '../components/EmptyState';
import { ErrorState } from '../components/ErrorState';
import { Icon } from '../components/Icon';
import { LoadingState } from '../components/LoadingState';
import { Modal } from '../components/Modal';
import { useNotify } from '../components/NotificationProvider';
import { ApiRequestError } from '../api/client';
import {
  activatePlan,
  changePlanStatus,
  deletePlan,
  getPlan,
  listPlans,
  type NutritionPlan,
  type PlanStatus,
} from '../api/plans';
import { PlanEditor } from './plan/PlanEditor';
import styles from './PlansPage.module.css';

/**
 * The user's nutrition plans (V53/V54).
 *
 * <p>Deliberately NOT a tab on the Administrar page, though every other editing
 * surface in the app lives there. That page says of itself "catálogos
 * compartidos por toda la aplicación", and a plan is the opposite: one
 * account's own diet, which the server scopes to the caller and shows to nobody
 * else. Putting it there would have been convenient and would have made the
 * page's own subtitle false.
 *
 * <p>Every macro figure on this screen is computed by the server against
 * today's food catalog. Nothing here recalculates and nothing here sends totals
 * back (ADR-006, ADR-001).
 */
const STATUS_LABELS: Record<PlanStatus, string> = {
  DRAFT: 'Borrador',
  ACTIVE: 'En curso',
  COMPLETED: 'Terminado',
  ARCHIVED: 'Archivado',
};

type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly plans: NutritionPlan[] };

export function PlansPage() {
  const notify = useNotify();
  const [state, setState] = useState<State>({ status: 'loading' });
  const [editing, setEditing] = useState<NutritionPlan | undefined>(undefined);
  const [creating, setCreating] = useState(false);
  const [deleting, setDeleting] = useState<NutritionPlan | undefined>(undefined);
  const [actionError, setActionError] = useState<string | undefined>(undefined);

  const reload = useCallback(() => {
    setState({ status: 'loading' });
    listPlans()
      .then((plans) => setState({ status: 'ready', plans }))
      .catch(() => setState({ status: 'error' }));
  }, []);

  useEffect(reload, [reload]);

  const openEditor = async (plan: NutritionPlan) => {
    setActionError(undefined);
    try {
      // The list carries headers only, so the days are fetched when one is opened. A list that
      // resolved four twelve-week plans against the catalog would work out some three hundred
      // lines to render four names.
      setEditing(await getPlan(plan.id));
    } catch {
      setActionError('No se pudo abrir el plan. Inténtalo de nuevo.');
    }
  };

  const act = async (what: () => Promise<unknown>, failure: string) => {
    setActionError(undefined);
    try {
      await what();
      reload();
    } catch (caught) {
      setActionError(caught instanceof ApiRequestError ? caught.message : failure);
    }
  };

  if (state.status === 'loading') {
    return <LoadingState message="Cargando planes…" />;
  }
  if (state.status === 'error') {
    return <ErrorState message="No se pudieron cargar los planes." onRetry={reload} />;
  }

  return (
    <div className={styles.wrapper}>
      <header className={styles.header}>
        <div>
          <h1 className={styles.title}>Mis planes</h1>
          <p className={styles.subtitle}>
            Qué comer cada día de la semana. Los macros salen del catálogo de alimentos, así que se
            mueven solos cuando corriges uno.
          </p>
        </div>
        <Button variant="accent" type="button" onClick={() => setCreating(true)}>
          + Plan
        </Button>
      </header>

      <Link className={styles.back} to="/app/nutrition">
        ← Volver a nutrición
      </Link>

      {actionError && (
        <p className={styles.error} role="alert">
          {actionError}
        </p>
      )}

      {state.plans.length === 0 ? (
        <EmptyState
          title="Todavía no hay ningún plan"
          description="Un plan dice qué comer cada día. Mientras no haya ninguno en curso, la pantalla de nutrición no tiene nada que mostrar."
        />
      ) : (
        <ul className={styles.list}>
          {state.plans.map((plan) => (
            <li key={plan.id}>
              <Card>
                <div className={styles.row}>
                  <div className={styles.identity}>
                    <h2 className={styles.name}>{plan.name}</h2>
                    <div className={styles.meta}>
                      <Badge tone={plan.active ? 'accent' : 'neutral'}>
                        {STATUS_LABELS[plan.status]}
                      </Badge>
                      {plan.startDate && (
                        <span className={styles.dates}>desde {plan.startDate}</span>
                      )}
                    </div>
                    {plan.description && <p className={styles.description}>{plan.description}</p>}
                  </div>

                  <div className={styles.actions}>
                    {!plan.active && (
                      <Button
                        type="button"
                        variant="ghost"
                        onClick={() =>
                          act(
                            () =>
                              activatePlan(plan.id).then(() => notify.success('Plan en curso.')),
                            'No se pudo activar el plan.',
                          )
                        }
                      >
                        Seguir este
                      </Button>
                    )}
                    <Button type="button" variant="ghost" onClick={() => openEditor(plan)}>
                      Editar
                    </Button>
                    {plan.status !== 'ARCHIVED' && (
                      <Button
                        type="button"
                        variant="ghost"
                        onClick={() =>
                          act(
                            () => changePlanStatus(plan.id, 'ARCHIVED'),
                            'No se pudo archivar el plan.',
                          )
                        }
                      >
                        Archivar
                      </Button>
                    )}
                    <button
                      type="button"
                      className={styles.remove}
                      aria-label={`Eliminar ${plan.name}`}
                      onClick={() => setDeleting(plan)}
                    >
                      <Icon name="trash" size={16} />
                    </button>
                  </div>
                </div>
              </Card>
            </li>
          ))}
        </ul>
      )}

      {(creating || editing) && (
        <Modal
          title={editing ? `Editar ${editing.name}` : 'Nuevo plan'}
          onClose={() => {
            setCreating(false);
            setEditing(undefined);
          }}
        >
          <PlanEditor
            plan={editing}
            onCancel={() => {
              setCreating(false);
              setEditing(undefined);
            }}
            onSaved={() => {
              setCreating(false);
              setEditing(undefined);
              reload();
            }}
          />
        </Modal>
      )}

      {deleting && (
        <ConfirmDialog
          title={`Eliminar ${deleting.name}`}
          message="El plan y todos sus días desaparecen. Los alimentos y las recetas no se tocan."
          confirmLabel="Eliminar"
          onConfirm={() => {
            const target = deleting;
            setDeleting(undefined);
            act(() => deletePlan(target.id), 'No se pudo eliminar el plan.');
          }}
          onCancel={() => setDeleting(undefined)}
        />
      )}
    </div>
  );
}
