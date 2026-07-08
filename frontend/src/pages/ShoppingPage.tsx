import { useEffect, useState } from 'react';
import { Card } from '../components/Card';
import { ApiRequestError } from '../api/client';
import {
  getShoppingList,
  setItemChecked,
  type ShoppingItem,
  type ShoppingList,
} from '../api/shopping';
import styles from './ShoppingPage.module.css';

/**
 * Shopping page (FOR-39). Shows the weekly shopping checklist and budget from the FOR-39 API:
 * per-item name, quantity, estimated cost and checked state, plus the weekly total and monthly
 * estimate. The user can check/uncheck items (persisted). Renders the API read model directly
 * (ADR-006); handles loading, empty and error states. Editing prices/quantities is a separate story.
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly list: ShoppingList };

const EUR = new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' });
const MARK_ERROR = 'No se pudo actualizar el artículo. Inténtalo de nuevo.';

export function ShoppingPage() {
  const [state, setState] = useState<State>({ status: 'loading' });
  const [actionError, setActionError] = useState<string | undefined>(undefined);
  const [pendingId, setPendingId] = useState<string | undefined>(undefined);

  useEffect(() => {
    let active = true;
    getShoppingList()
      .then((list) => {
        if (active) {
          setState({ status: 'ready', list });
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

  async function toggle(item: ShoppingItem) {
    setActionError(undefined);
    setPendingId(item.id);
    try {
      const result = await setItemChecked(item.id, !item.checked);
      setState((current) =>
        current.status === 'ready'
          ? {
              status: 'ready',
              list: {
                ...current.list,
                items: current.list.items.map((it) =>
                  it.id === result.id ? { ...it, checked: result.checked } : it,
                ),
              },
            }
          : current,
      );
    } catch (error) {
      setActionError(error instanceof ApiRequestError ? error.message : MARK_ERROR);
    } finally {
      setPendingId(undefined);
    }
  }

  return (
    <div className={styles.wrapper}>
      <header className={styles.header}>
        <h1 className={styles.title}>Lista de compra</h1>
        <p className={styles.subtitle}>Generada para tu plan nutricional semanal.</p>
      </header>
      {actionError && (
        <p className={styles.actionError} role="alert">
          {actionError}
        </p>
      )}
      {renderContent(state, toggle, pendingId)}
    </div>
  );
}

function renderContent(
  state: State,
  toggle: (item: ShoppingItem) => void,
  pendingId: string | undefined,
) {
  if (state.status === 'loading') {
    return (
      <p className={styles.message} role="status">
        Cargando tu lista de compra…
      </p>
    );
  }

  if (state.status === 'error') {
    return (
      <p className={styles.message} role="alert">
        No se pudo cargar tu lista de compra. Inténtalo de nuevo más tarde.
      </p>
    );
  }

  const { items, budget } = state.list;

  return (
    <>
      <section className={styles.tiles} aria-label="Presupuesto">
        <Card title="Total estimado (semana)">
          <p className={styles.tileValue}>{EUR.format(budget.weeklyEur)}</p>
        </Card>
        <Card title="Estimado mensual">
          <p className={styles.tileValue}>{EUR.format(budget.monthlyEur)}</p>
        </Card>
      </section>

      {items.length === 0 ? (
        <p className={styles.message} role="status">
          No hay artículos en la lista de esta semana.
        </p>
      ) : (
        <Card title="Artículos">
          <ul className={styles.items}>
            {items.map((item) => (
              <li key={item.id} className={styles.item}>
                <label className={styles.itemLabel}>
                  <input
                    type="checkbox"
                    checked={item.checked}
                    disabled={pendingId === item.id}
                    onChange={() => toggle(item)}
                  />
                  <span className={item.checked ? styles.checkedName : undefined}>
                    {item.productName}
                  </span>
                </label>
                <span className={styles.quantity}>{item.quantity}</span>
                <span className={styles.cost}>{EUR.format(item.estimatedCostEur)}</span>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </>
  );
}
