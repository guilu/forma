import { useEffect, useState } from 'react';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { Icon } from '../../components/Icon';
import { WidgetLoading } from '../../components/WidgetLoading';
import { getShoppingList, type ShoppingList } from '../../api/shopping';
import { unitLabel } from '../shoppingDisplay';
import { WidgetSection } from './WidgetSection';
import styles from './ShoppingWidget.module.css';

/**
 * "Lista de compra" preview widget (FOR-51, rebuilt for the FOR-164 dashboard
 * mockup): the first few items of this week's FOR-39 shopping list — real
 * product name + quantity + unit — with a "Ver lista completa" link. Renders
 * API values as returned (ADR-006).
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'empty' }
  | { readonly status: 'ready'; readonly list: ShoppingList };

const PREVIEW_COUNT = 5;

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
        if (active) setState({ status: 'error' });
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <WidgetSection
      id="shopping-widget-title"
      title="Lista de compra"
      linkTo="/lista-compra"
      linkLabel="Ver lista completa"
    >
      {renderContent(state)}
    </WidgetSection>
  );
}

function renderContent(state: State) {
  if (state.status === 'loading') {
    return <WidgetLoading label="Cargando tu lista de compra…" rows={2} />;
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

  return (
    <ul className={styles.items}>
      {state.list.items.slice(0, PREVIEW_COUNT).map((item) => (
        <li key={item.id} className={styles.item}>
          <span className={styles.itemIcon} aria-hidden="true">
            <Icon name="shopping" size={16} />
          </span>
          <span className={styles.itemName}>{item.productName}</span>
          <span className={styles.itemQty}>
            {item.quantity} {unitLabel(item.unit)}
          </span>
        </li>
      ))}
    </ul>
  );
}
