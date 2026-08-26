import { useEffect, useState } from 'react';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { Icon } from '../../components/Icon';
import { WidgetLoading } from '../../components/WidgetLoading';
import { getShoppingList, type ShoppingList } from '../../api/shopping';
import { shortUnitLabel, splitProductName } from '../shoppingDisplay';
import { WidgetSection } from './WidgetSection';
import styles from './ShoppingWidget.module.css';

/**
 * "Lista de compra" preview widget (FOR-51, rebuilt for the FOR-164 dashboard
 * mockup): the first few items of this week's FOR-39 shopping list — real
 * product name + quantity + unit — with a "Ver lista completa" link. Renders
 * API values as returned (ADR-006).
 *
 * <p>Typography and row shape are the menu card's, which sits beside it in the
 * same row: the product's head noun on top and its qualifiers under it, both
 * clipped with an ellipsis rather than wrapped, so five rows are always five
 * rows tall. The unit is abbreviated here and only here — "3 u" instead of "3
 * unidades" — because in this width the word was costing the product name its
 * last characters.
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
      linkTo="/app/shopping-list"
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

  const remaining = state.list.items.length - PREVIEW_COUNT;

  return (
    <>
      <ul className={styles.items}>
        {state.list.items.slice(0, PREVIEW_COUNT).map((item) => {
          const { head, rest } = splitProductName(item.productName);
          return (
            <li key={item.id} className={styles.item}>
              <span className={styles.itemIcon} aria-hidden="true">
                <Icon name="shopping" size={16} />
              </span>
              {/* Two lines like the menu card's meals, and for the same reason:
                  what the product IS reads first, and which one it is follows
                  underneath instead of pushing the quantity off the row. */}
              <span className={styles.itemText}>
                <span className={styles.itemName}>{head}</span>
                {rest && <span className={styles.itemDescription}>{rest}</span>}
              </span>
              <span className={styles.itemQty}>
                {item.quantity} {shortUnitLabel(item.unit)}
              </span>
            </li>
          );
        })}
      </ul>
      {remaining > 0 && <p className={styles.more}>+ {remaining} productos más</p>}
    </>
  );
}
