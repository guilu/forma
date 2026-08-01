import { useCallback, useEffect, useState } from 'react';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { ErrorState } from '../components/ErrorState';
import { LoadingState } from '../components/LoadingState';
import { Modal } from '../components/Modal';
import { useNotify } from '../components/NotificationProvider';
import { useMediaQuery } from '../hooks/useMediaQuery';
import { useMountedRef } from '../hooks/useMountedRef';
import { ApiRequestError } from '../api/client';
import { deleteFood, listFoods, type CatalogFood } from '../api/foods';
import { FoodForm } from './admin/FoodForm';
import { FoodTable } from './admin/FoodTable';
import { Pagination } from './admin/Pagination';
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

/** Rows per page. Ten fills a phone screen without the card growing past it. */
const PAGE_SIZE = 10;

/** Below this the table drops to two columns — see `FoodTable`. */
const NARROW = '(max-width: 767px)';

export function AdminPage() {
  const notify = useNotify();
  const mountedRef = useMountedRef();
  const [state, setState] = useState<State>({ status: 'loading' });
  const [editing, setEditing] = useState<CatalogFood | undefined>(undefined);
  const [deleting, setDeleting] = useState<CatalogFood | undefined>(undefined);
  const [pending, setPending] = useState(false);
  const [actionError, setActionError] = useState<string | undefined>(undefined);
  const [page, setPage] = useState(0);
  const [expandedId, setExpandedId] = useState<string | undefined>(undefined);
  const narrow = useMediaQuery(NARROW);

  const foods = state.status === 'ready' ? state.foods : [];
  const pageCount = Math.max(1, Math.ceil(foods.length / PAGE_SIZE));
  // Clamped on read rather than corrected in an effect: deleting the last row of
  // the last page shrinks the catalog under the current page, and a render that
  // shows an empty table before an effect fixes it is a flicker users notice.
  const currentPage = Math.min(page, pageCount - 1);
  const visible = foods.slice(currentPage * PAGE_SIZE, currentPage * PAGE_SIZE + PAGE_SIZE);

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
        {/* The card carries no title: it sat directly above a column header
            saying the same word, and the table names itself for assistive
            tech. */}
        {state.status === 'ready' && (
          <Card>
            <FoodTable
              foods={visible}
              narrow={narrow}
              expandedId={expandedId}
              onToggle={(id) => setExpandedId((open) => (open === id ? undefined : id))}
              onEdit={(food) => {
                setActionError(undefined);
                setEditing(food);
              }}
              onDelete={(food) => {
                setActionError(undefined);
                setDeleting(food);
              }}
            />
            <Pagination
              page={currentPage}
              pageCount={pageCount}
              onChange={(next) => {
                setPage(next);
                // The open row belongs to the page that just left.
                setExpandedId(undefined);
              }}
            />
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
