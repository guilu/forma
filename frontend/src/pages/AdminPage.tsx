import { useCallback, useEffect, useState } from 'react';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { ErrorState } from '../components/ErrorState';
import { Icon } from '../components/Icon';
import { LoadingState } from '../components/LoadingState';
import { Modal } from '../components/Modal';
import { useNotify } from '../components/NotificationProvider';
import { useMountedRef } from '../hooks/useMountedRef';
import { ApiRequestError } from '../api/client';
import { deleteFood, listFoods, type CatalogFood } from '../api/foods';
import { FoodForm } from './admin/FoodForm';
import { CATEGORY_LABELS } from './admin/foodDisplay';
import styles from './AdminPage.module.css';

/**
 * Catalog maintenance (FOR-190), reachable from the account menu by an admin.
 *
 * <p>One tab today — the food catalog, the global reference data every plan is
 * built from. The store catalogs (Mercadona, later Carrefour) get their own tabs
 * in the next slice, which is why this is a tab bar rather than a page with a
 * single table on it.
 *
 * <p>`RequireAdmin` keeps non-admins off the route and the account menu hides
 * the link, but neither is what protects the catalog: every write here is
 * refused server-side by `@PreAuthorize` without the admin authority. The
 * client-side checks only spare ordinary accounts a screen full of 403s.
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly foods: CatalogFood[] };

const TABS = [{ key: 'macros', label: 'Macros' }] as const;

export function AdminPage() {
  const notify = useNotify();
  const mountedRef = useMountedRef();
  const [state, setState] = useState<State>({ status: 'loading' });
  const [editing, setEditing] = useState<CatalogFood | undefined>(undefined);
  const [deleting, setDeleting] = useState<CatalogFood | undefined>(undefined);
  const [pending, setPending] = useState(false);
  const [actionError, setActionError] = useState<string | undefined>(undefined);

  const load = useCallback(() => {
    listFoods()
      .then((foods) => {
        if (mountedRef.current) setState({ status: 'ready', foods });
      })
      .catch(() => {
        if (mountedRef.current) setState({ status: 'error' });
      });
  }, [mountedRef]);

  useEffect(() => {
    load();
  }, [load]);

  async function confirmDelete() {
    if (!deleting) return;
    setPending(true);
    setActionError(undefined);
    try {
      await deleteFood(deleting.id);
      setDeleting(undefined);
      notify.success('Alimento eliminado.');
      load();
    } catch (caught) {
      setActionError(
        caught instanceof ApiRequestError
          ? caught.message
          : 'No se pudo eliminar el alimento. Puede estar en uso por un producto.',
      );
    } finally {
      setPending(false);
    }
  }

  return (
    <div className={styles.wrapper}>
      <header className={styles.header}>
        <div className={styles.titles}>
          <h1 className={styles.title}>Administrar</h1>
          <p className={styles.subtitle}>Catálogos compartidos por toda la aplicación.</p>
        </div>
        <Button variant="accent" type="button" onClick={() => setEditing(NEW_FOOD)}>
          + Alimento
        </Button>
      </header>

      <div className={styles.tabs} role="tablist" aria-label="Catálogos">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            type="button"
            role="tab"
            id={`tab-${tab.key}`}
            aria-selected="true"
            aria-controls={`panel-${tab.key}`}
            className={styles.tabActive}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <section id="panel-macros" role="tabpanel" aria-labelledby="tab-macros">
        {actionError && (
          <p className={styles.actionError} role="alert">
            {actionError}
          </p>
        )}
        {state.status === 'loading' && <LoadingState message="Cargando el catálogo…" />}
        {state.status === 'error' && (
          <ErrorState message="No se pudo cargar el catálogo." onRetry={load} />
        )}
        {state.status === 'ready' && (
          <Card title="Alimentos" headingLevel={2}>
            <div className={styles.tableScroll}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th scope="col">Alimento</th>
                    <th scope="col">Categoría</th>
                    <th scope="col">kcal</th>
                    <th scope="col">Prot.</th>
                    <th scope="col">HC</th>
                    <th scope="col">Grasa</th>
                    <th scope="col">Ración</th>
                    <th scope="col">
                      <span className={styles.srOnly}>Acciones</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {state.foods.map((food) => (
                    <tr key={food.id}>
                      <td>{food.name}</td>
                      <td>{food.category ? CATEGORY_LABELS[food.category] : '—'}</td>
                      <td>{food.kcal}</td>
                      <td>{food.proteinG}</td>
                      <td>{food.carbsG}</td>
                      <td>{food.fatG}</td>
                      <td>{food.servingSizeG ? `${food.servingSizeG} g` : '—'}</td>
                      <td>
                        <div className={styles.rowActions}>
                          <button
                            type="button"
                            className={styles.rowAction}
                            aria-label={`Editar ${food.name}`}
                            onClick={() => {
                              setActionError(undefined);
                              setEditing(food);
                            }}
                          >
                            <Icon name="edit" size={18} />
                          </button>
                          <button
                            type="button"
                            className={styles.rowAction}
                            aria-label={`Eliminar ${food.name}`}
                            onClick={() => {
                              setActionError(undefined);
                              setDeleting(food);
                            }}
                          >
                            <Icon name="trash" size={18} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Card>
        )}
      </section>

      {editing && (
        <Modal
          title={editing === NEW_FOOD ? 'Nuevo alimento' : `Editar ${editing.name}`}
          onClose={() => setEditing(undefined)}
        >
          <FoodForm
            food={editing === NEW_FOOD ? undefined : editing}
            onCancel={() => setEditing(undefined)}
            onSaved={() => {
              setEditing(undefined);
              load();
            }}
          />
        </Modal>
      )}

      {deleting && (
        <ConfirmDialog
          title="Eliminar alimento"
          message={`¿Seguro que quieres eliminar "${deleting.name}" del catálogo? Es referencia compartida: si algún producto lo enlaza, el borrado se rechazará.`}
          confirmLabel="Eliminar"
          pending={pending}
          onConfirm={confirmDelete}
          onCancel={() => setDeleting(undefined)}
        />
      )}
    </div>
  );
}

/** Sentinel for "the form is open on a food that does not exist yet". */
const NEW_FOOD = {
  id: '',
  name: '',
  kcal: 0,
  proteinG: 0,
  carbsG: 0,
  fatG: 0,
} as CatalogFood;
