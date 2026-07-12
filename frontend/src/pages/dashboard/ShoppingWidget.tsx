import { useEffect, useState } from 'react';
import { Card } from '../../components/Card';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { WidgetLoading } from '../../components/WidgetLoading';
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
    return <WidgetLoading label="Cargando tu presupuesto…" rows={2} />;
  }

  if (state.status === 'error') {
    return (
      <ErrorState message="No se pudo cargar tu lista de compra. Inténtalo de nuevo más tarde." />
    );
  }

  if (state.status === 'empty') {
    return (
      <EmptyState variant="filtered" title="Aún no hay una lista de compra generada esta semana." />
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
