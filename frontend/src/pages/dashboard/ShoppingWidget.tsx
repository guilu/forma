import { useEffect, useState } from 'react';
import { Card } from '../../components/Card';
import { getShoppingList, type ShoppingList } from '../../api/shopping';
import { WidgetSection } from './WidgetSection';
import styles from './ShoppingWidget.module.css';

/**
 * Shopping budget summary widget (FOR-51): weekly total + monthly estimate from the
 * FOR-39 shopping list read model. Renders the API values as returned (ADR-006) — the
 * budget numbers are backend-computed, never recomputed here.
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'empty' }
  | { readonly status: 'ready'; readonly list: ShoppingList };

const EUR = new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' });

export function ShoppingWidget() {
  const [state, setState] = useState<State>({ status: 'loading' });

  useEffect(() => {
    let active = true;
    getShoppingList()
      .then((list) => {
        if (!active) return;
        setState(list.items.length === 0 ? { status: 'empty' } : { status: 'ready', list });
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
    <WidgetSection
      id="shopping-widget-title"
      title="Presupuesto de la compra"
      linkTo="/lista-compra"
    >
      {renderContent(state)}
    </WidgetSection>
  );
}

function renderContent(state: State) {
  if (state.status === 'loading') {
    return (
      <p className={styles.message} role="status">
        Cargando tu presupuesto…
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

  if (state.status === 'empty') {
    return (
      <p className={styles.message} role="status">
        Aún no hay una lista de compra generada esta semana.
      </p>
    );
  }

  const { budget } = state.list;

  return (
    <div className={styles.tiles}>
      <Card title="Presupuesto semanal">
        <p className={styles.value}>{EUR.format(budget.weeklyEur)}</p>
      </Card>
      <Card title="Estimado mensual">
        <p className={styles.value}>{EUR.format(budget.monthlyEur)}</p>
      </Card>
    </div>
  );
}
