import { useEffect, useState, type FormEvent } from 'react';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { EmptyState } from '../components/EmptyState';
import { ErrorState } from '../components/ErrorState';
import { Icon } from '../components/Icon';
import { LoadingState } from '../components/LoadingState';
import { MetricCard } from '../components/MetricCard';
import { Modal } from '../components/Modal';
import { useNotify } from '../components/NotificationProvider';
import { SavedIndicator } from '../components/SavedIndicator';
import { TextField } from '../components/FormField';
import { ApiRequestError } from '../api/client';
import {
  getShoppingList,
  listShoppingProducts,
  setItemChecked,
  updateShoppingProduct,
  type ShoppingItem,
  type ShoppingList,
  type ShoppingProduct,
} from '../api/shopping';
import {
  ALL_CATEGORIES,
  buildCategoryTabs,
  categoryLabel,
  filterItemsByCategory,
} from './shoppingCategories';
import styles from './ShoppingPage.module.css';

/**
 * Shopping page (FOR-39 checklist + budget, built out to the mockup by FOR-55:
 * `docs/5-lista-compra.png`). Reads the weekly list + budget from FOR-39/FOR-38
 * and resolves product edits through FOR-36; renders the API read models
 * directly (ADR-006/ADR-001 — no pricing/budget math here).
 *
 * <p>Mockup elements not backed by the API today (documented gap, repository
 * priority per AGENTS.md — never invented):
 * <ul>
 *   <li><b>Quantity unit</b> ("unidades"/"kg"/"g") — {@code quantity} is a
 *       plain integer with no unit field; shown as-is.
 *   <li><b>"PORCIONES" and "GENERADA"</b> budget tiles — the list/budget read
 *       models carry no servings count or generation timestamp
 *       ({@code weekStartDate} is not "generated at"), so these tiles are
 *       omitted rather than shown with invented data (same precedent as the
 *       FOR-54 Nutrition page).
 *   <li><b>"Generar nueva lista"</b>, per-item Mercadona link-out/add-to-cart
 *       icons and +/- quantity editing — no regenerate, link-out or item-
 *       quantity-update endpoint exists; omitted entirely rather than shown
 *       inactive (same precedent as FOR-54).
 * </ul>
 *
 * <p><b>Category filter tabs and id-based product edit (FOR-111)</b>:
 * {@code ShoppingListResponse.Item} (FOR-106) now carries {@code productId}
 * and {@code category}, so the list is grouped/filtered by
 * {@link ShoppingItem.category} exactly as returned (no UI-side category
 * inference, ADR-006) and the edit entry point resolves the product by
 * {@link ShoppingItem.productId} instead of matching {@code productName}.
 * Editing a product's price does not retroactively change this week's
 * already-generated line cost (the list stores its own
 * {@code estimatedCostEur} — see {@code ShoppingListItem} javadoc); this is a
 * documented limitation, not a bug.
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly list: ShoppingList };

const EUR = new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' });
const MARK_ERROR = 'No se pudo actualizar el artículo. Inténtalo de nuevo.';

export function ShoppingPage() {
  const notify = useNotify();
  const [state, setState] = useState<State>({ status: 'loading' });
  const [retryToken, setRetryToken] = useState(0);
  const [actionError, setActionError] = useState<string | undefined>(undefined);
  const [pendingId, setPendingId] = useState<string | undefined>(undefined);
  const [editingItem, setEditingItem] = useState<ShoppingItem | undefined>(undefined);
  const [selectedCategory, setSelectedCategory] = useState<string>(ALL_CATEGORIES);

  useEffect(() => {
    let active = true;
    setState({ status: 'loading' });
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
  }, [retryToken]);

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
      // Success feedback for the "toggle shopping item" key action (FOR-63).
      // Rapid repeated toggles collapse into a single toast (NotificationProvider
      // dedupe), matching the spec edge case "avoid stacking excessive notifications".
      notify.success('Artículo actualizado.');
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
      {renderContent(
        state,
        toggle,
        pendingId,
        () => setRetryToken((t) => t + 1),
        setEditingItem,
        selectedCategory,
        setSelectedCategory,
      )}
      {editingItem && (
        <ProductEditModal item={editingItem} onClose={() => setEditingItem(undefined)} />
      )}
    </div>
  );
}

function renderContent(
  state: State,
  toggle: (item: ShoppingItem) => void,
  pendingId: string | undefined,
  retry: () => void,
  onEdit: (item: ShoppingItem) => void,
  selectedCategory: string,
  onSelectCategory: (category: string) => void,
) {
  if (state.status === 'loading') {
    return <LoadingState message="Cargando tu lista de compra…" />;
  }

  if (state.status === 'error') {
    return (
      <ErrorState
        message="No se pudo cargar tu lista de compra. Inténtalo de nuevo más tarde."
        onRetry={retry}
      />
    );
  }

  const { items, budget } = state.list;
  const tabs = buildCategoryTabs(items);
  const filteredItems = filterItemsByCategory(items, selectedCategory);

  return (
    <>
      <section className={styles.tiles} aria-label="Presupuesto">
        <MetricCard label="Productos" value={String(items.length)} unit="productos únicos" />
        <MetricCard
          label="Total estimado"
          value={EUR.format(budget.weeklyEur)}
          unit="precio aproximado"
        />
        <MetricCard label="Estimado mensual" value={EUR.format(budget.monthlyEur)} />
      </section>

      {/* Category filter tabs (FOR-111): one tab per distinct category present
          in the list (FOR-106), plus "Todas" first. */}
      <div className={styles.tabs} role="tablist" aria-label="Categorías">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            type="button"
            role="tab"
            aria-selected={tab.key === selectedCategory}
            className={styles.tab}
            onClick={() => onSelectCategory(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {items.length === 0 ? (
        <EmptyState title="No hay artículos en la lista de esta semana." />
      ) : filteredItems.length === 0 ? (
        <EmptyState variant="filtered" title="No hay artículos en esta categoría." />
      ) : (
        <Card title={categoryLabel(selectedCategory)}>
          <ul className={styles.items}>
            {filteredItems.map((item) => (
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
                <button
                  type="button"
                  className={styles.editButton}
                  onClick={() => onEdit(item)}
                  aria-label={`Editar producto ${item.productName}`}
                >
                  <Icon name="edit" size={16} />
                </button>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </>
  );
}

type ProductState =
  | { readonly status: 'loading' }
  | { readonly status: 'not-found' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly product: ShoppingProduct };

const PRODUCT_NOT_FOUND =
  'No se pudo encontrar el producto asociado a este artículo para editarlo.';
const PRODUCT_LOAD_ERROR = 'No se pudo cargar el producto. Inténtalo de nuevo.';
const PRODUCT_SAVE_ERROR = 'No se pudo guardar el producto. Inténtalo de nuevo.';

/**
 * Entry point to edit a product's price/URL (FOR-36), reached from a list item.
 *
 * <p>FOR-60 note: this modal's own loading/error/not-found states are
 * deliberately left on their pre-existing inline markup rather than migrated
 * to the shared state components. It is a secondary, nested flow with a
 * three-way state (`loading`/`not-found`/`error`) that doesn't map 1:1 onto
 * the shared components without inventing new behavior, and the FOR-60
 * migration discipline favors a documented deferral here over risking a
 * regression in an already-tested modal for low additional value. Follow-up:
 * a future story can fold this into {@link EmptyState}/{@link ErrorState} if
 * the product-not-found case gets its own shared "not found" treatment.
 */
function ProductEditModal({
  item,
  onClose,
}: {
  readonly item: ShoppingItem;
  readonly onClose: () => void;
}) {
  const [state, setState] = useState<ProductState>({ status: 'loading' });
  const [url, setUrl] = useState('');
  const [price, setPrice] = useState('');
  const [priceError, setPriceError] = useState<string | undefined>(undefined);
  const [saveError, setSaveError] = useState<string | undefined>(undefined);
  const [saved, setSaved] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let active = true;
    listShoppingProducts()
      .then((products) => {
        if (!active) return;
        // Match by id (FOR-106/FOR-111) — not by name, which broke when two
        // products shared a display name.
        const match = products.find((product) => product.id === item.productId);
        if (!match) {
          setState({ status: 'not-found' });
          return;
        }
        setState({ status: 'ready', product: match });
        setUrl(match.url ?? '');
        setPrice(String(match.estimatedPriceEur));
      })
      .catch(() => {
        if (active) {
          setState({ status: 'error' });
        }
      });
    return () => {
      active = false;
    };
  }, [item.productId]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (state.status !== 'ready') {
      return;
    }
    const priceValue = Number(price);
    if (!price || !Number.isFinite(priceValue) || priceValue <= 0) {
      setPriceError('Introduce un precio válido.');
      return;
    }
    setPriceError(undefined);
    setSaveError(undefined);
    setSaving(true);
    try {
      const updated = await updateShoppingProduct(state.product.id, {
        name: state.product.name,
        url: url.trim() ? url.trim() : undefined,
        packageSize: state.product.packageSize,
        estimatedPriceEur: priceValue,
        pricePerUnitEur: state.product.pricePerUnitEur,
        linkedFoodItemId: state.product.linkedFoodItemId,
        notes: state.product.notes,
      });
      setState({ status: 'ready', product: updated });
      setSaved(true);
    } catch (error) {
      setSaveError(error instanceof ApiRequestError ? error.message : PRODUCT_SAVE_ERROR);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title="Editar producto" onClose={onClose}>
      {state.status === 'loading' && (
        <p className={styles.message} role="status">
          Cargando producto…
        </p>
      )}
      {state.status === 'error' && (
        <p className={styles.message} role="alert">
          {PRODUCT_LOAD_ERROR}
        </p>
      )}
      {state.status === 'not-found' && (
        <p className={styles.message} role="alert">
          {PRODUCT_NOT_FOUND}
        </p>
      )}
      {state.status === 'ready' && (
        <form className={styles.editForm} onSubmit={handleSubmit} noValidate>
          <p className={styles.editProductName}>{state.product.name}</p>
          <TextField
            id="product-url"
            label="URL (opcional)"
            type="text"
            value={url}
            disabled={saving}
            onChange={(event) => setUrl(event.target.value)}
          />
          <TextField
            id="product-price"
            label="Precio estimado (€)"
            type="number"
            step="0.01"
            value={price}
            error={priceError}
            disabled={saving}
            onChange={(event) => setPrice(event.target.value)}
          />
          {saveError && (
            <p className={styles.actionError} role="alert">
              {saveError}
            </p>
          )}
          {saved && !saveError && <SavedIndicator message="Producto actualizado." />}
          <div className={styles.editActions}>
            <Button variant="secondary" type="button" onClick={onClose}>
              Cerrar
            </Button>
            <Button type="submit" loading={saving}>
              Guardar
            </Button>
          </div>
        </form>
      )}
    </Modal>
  );
}
