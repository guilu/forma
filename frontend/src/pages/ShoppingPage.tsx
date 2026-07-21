import { useEffect, useState, type FormEvent } from 'react';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { ConfirmDialog } from '../components/ConfirmDialog';
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
  regenerateShoppingList,
  setItemChecked,
  updateItemQuantity,
  updateShoppingProduct,
  type ShoppingItem,
  type ShoppingList,
  type ShoppingProduct,
} from '../api/shopping';
import {
  ALL_CATEGORIES,
  buildCategoryTabs,
  categoryIcon,
  categoryLabel,
  filterItemsByCategory,
  groupItemsByCategory,
} from './shoppingCategories';
import { formatGeneratedAt, totalServings, unitLabel } from './shoppingDisplay';
import styles from './ShoppingPage.module.css';

/**
 * Shopping page (FOR-39 checklist + budget, built out to the mockup by FOR-55:
 * `docs/5-lista-compra.png`). Reads the weekly list + budget from FOR-39/FOR-38
 * and resolves product edits through FOR-36; renders the API read models
 * directly (ADR-006/ADR-001 — no pricing/budget math here).
 *
 * <p><b>Quantity unit and "Generada" tile (FOR-117)</b>: {@code quantity} now
 * renders together with its {@link ShoppingItem.unit} (FOR-108, e.g. "2 kg"),
 * and the list's {@link ShoppingList.generatedAt} is shown as a restored
 * "Generada" tile — closing the two gaps this doc comment used to document
 * as omitted. The mockup's "PORCIONES" aggregate tile stays omitted, though:
 * summing per-item {@link ShoppingItem.servings} across unrelated products
 * (e.g. chicken servings + milk servings) would not be a meaningful number,
 * so servings render per item only (see the item row below), never as a
 * list-level aggregate (spec.md Open Questions).
 *
 * <p><b>Regenerate, quantity +/- and link-out (FOR-118)</b>: this doc comment
 * used to document these as an omitted gap (no backing endpoint). FOR-109
 * shipped the three commands and this story wires them up: "Generar nueva
 * lista" rebuilds the list behind a {@link ConfirmDialog} (FOR-63 destructive-
 * confirmation pattern, since it discards checked state); per-item +/-
 * controls call the quantity-edit command and render the backend's
 * recalculated {@code estimatedCostEur} (never computed client-side, ADR-006);
 * the per-item link-out control renders {@link ShoppingItem.productUrl} when
 * present, omitted (not disabled) otherwise — same "omit, don't show
 * inactive" precedent as FOR-54. Regenerate and any single item's pending
 * action mutually exclude each other (regenerate disables while any item
 * action is in flight) to avoid a race between a full-list rebuild and a
 * per-item update targeting a since-replaced item id.
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
const QUANTITY_ERROR = 'No se pudo actualizar la cantidad. Inténtalo de nuevo.';
const REGENERATE_ERROR = 'No se pudo regenerar la lista. Inténtalo de nuevo.';
const REGENERATE_CONFIRM_MESSAGE =
  'Se generará una nueva lista de la compra a partir de tu plan actual. Se perderá el estado marcado de los artículos.';

export function ShoppingPage() {
  const notify = useNotify();
  const [state, setState] = useState<State>({ status: 'loading' });
  const [retryToken, setRetryToken] = useState(0);
  const [actionError, setActionError] = useState<string | undefined>(undefined);
  const [pendingId, setPendingId] = useState<string | undefined>(undefined);
  const [editingItem, setEditingItem] = useState<ShoppingItem | undefined>(undefined);
  const [selectedCategory, setSelectedCategory] = useState<string>(ALL_CATEGORIES);
  const [confirmRegenerate, setConfirmRegenerate] = useState(false);
  const [regenerating, setRegenerating] = useState(false);

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

  /**
   * Quantity +/- handler (FOR-118): mirrors {@link toggle}'s `pendingId`/
   * try-catch/`ApiRequestError` pattern. Waits for the response rather than
   * updating optimistically (spec.md Open Questions: MVP simplicity, matches
   * `toggle`); on failure nothing was changed yet, so the displayed quantity
   * is already "reverted" — no separate rollback state needed.
   */
  async function changeQuantity(item: ShoppingItem, delta: 1 | -1) {
    const nextQuantity = item.quantity + delta;
    if (nextQuantity < 1) {
      return;
    }
    setActionError(undefined);
    setPendingId(item.id);
    try {
      const result = await updateItemQuantity(item.id, nextQuantity);
      setState((current) =>
        current.status === 'ready'
          ? {
              status: 'ready',
              list: {
                ...current.list,
                items: current.list.items.map((it) =>
                  it.id === result.id
                    ? {
                        ...it,
                        quantity: result.quantity,
                        estimatedCostEur: result.estimatedCostEur,
                      }
                    : it,
                ),
              },
            }
          : current,
      );
      notify.success('Cantidad actualizada.');
    } catch (error) {
      setActionError(error instanceof ApiRequestError ? error.message : QUANTITY_ERROR);
    } finally {
      setPendingId(undefined);
    }
  }

  /**
   * Regenerate confirm handler (FOR-118/FOR-109): mirrors
   * `IntegrationsSection`'s disconnect-confirm flow (FOR-63) — the dialog
   * closes on both success and failure (a failed regenerate is not
   * retry-recoverable from a stuck-open dialog; the page-level `actionError`
   * carries the message instead).
   */
  async function handleRegenerateConfirm() {
    setActionError(undefined);
    setRegenerating(true);
    try {
      const regenerated = await regenerateShoppingList();
      setState({ status: 'ready', list: regenerated });
      notify.success('Lista regenerada correctamente.');
    } catch (error) {
      setActionError(error instanceof ApiRequestError ? error.message : REGENERATE_ERROR);
    } finally {
      setRegenerating(false);
      setConfirmRegenerate(false);
    }
  }

  return (
    <div className={styles.wrapper}>
      <header className={styles.header}>
        <div className={styles.headerText}>
          <h1 className={styles.title}>Lista de compra</h1>
          <p className={styles.subtitle}>Generada para tu plan nutricional semanal.</p>
        </div>
        {state.status === 'ready' && (
          <div className={styles.headerActions}>
            {/* Week selector — visual only (FOR-164 mockup). There is no
                per-week navigation endpoint yet, so the arrows are inert
                decorative affordances (aria-hidden, not focusable) and the
                label is static; wiring real week navigation is a separate,
                backend-dependent story. */}
            <div className={styles.weekSelector} aria-hidden="true">
              <span className={styles.weekArrow}>
                <Icon name="chevron" size={16} className={styles.weekArrowPrev} />
              </span>
              <span className={styles.weekLabel}>Semana del 8 - 14 Junio 2025</span>
              <span className={styles.weekArrow}>
                <Icon name="chevron" size={16} />
              </span>
            </div>
            <Button
              variant="secondary"
              onClick={() => setConfirmRegenerate(true)}
              disabled={pendingId !== undefined || regenerating}
            >
              Generar nueva lista
            </Button>
          </div>
        )}
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
        regenerating,
        () => setRetryToken((t) => t + 1),
        setEditingItem,
        selectedCategory,
        setSelectedCategory,
        changeQuantity,
      )}
      {editingItem && (
        <ProductEditModal item={editingItem} onClose={() => setEditingItem(undefined)} />
      )}
      {confirmRegenerate && (
        // Destructive-confirmation pattern (FOR-63): regenerating discards
        // the current checked state, so it requires explicit confirmation;
        // cancel has no side effect (Modal's close/Escape/backdrop all route
        // through onCancel only).
        <ConfirmDialog
          title="Generar nueva lista"
          message={REGENERATE_CONFIRM_MESSAGE}
          confirmLabel="Generar nueva lista"
          pending={regenerating}
          onConfirm={handleRegenerateConfirm}
          onCancel={() => setConfirmRegenerate(false)}
        />
      )}
    </div>
  );
}

function renderContent(
  state: State,
  toggle: (item: ShoppingItem) => void,
  pendingId: string | undefined,
  regenerating: boolean,
  retry: () => void,
  onEdit: (item: ShoppingItem) => void,
  selectedCategory: string,
  onSelectCategory: (category: string) => void,
  onChangeQuantity: (item: ShoppingItem, delta: 1 | -1) => void,
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

  const { items, budget, generatedAt } = state.list;
  const tabs = buildCategoryTabs(items);
  const filteredItems = filterItemsByCategory(items, selectedCategory);
  const groups = groupItemsByCategory(filteredItems);

  return (
    <>
      <section className={styles.tiles} aria-label="Presupuesto">
        <MetricCard
          label="Productos"
          headingLevel={2}
          icon="shopping"
          value={String(items.length)}
          unit="productos únicos"
        />
        <MetricCard
          label="Total estimado"
          headingLevel={2}
          icon="goals"
          value={EUR.format(budget.weeklyEur)}
          unit="precio aproximado"
        />
        {/* "Porciones" aggregate tile (FOR-164 mockup): sums the per-item
            servings the backend returns, skipping non-food items
            (`servings: null`) rather than fabricating a figure. */}
        <MetricCard
          label="Porciones"
          headingLevel={2}
          icon="nutrition"
          value={String(totalServings(items))}
          unit="para 7 días"
        />
        {/* List-level generation timestamp (FOR-117). */}
        <MetricCard
          label="Generada"
          headingLevel={2}
          icon="progress"
          value={formatGeneratedAt(generatedAt)}
        />
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
        <Card className={styles.tableCard}>
          {/* Sort control — visual only (FOR-164 mockup). The list is already
              grouped by category; a real re-sort has no backing option yet, so
              this is a static, inert affordance. */}
          <div className={styles.sortRow} aria-hidden="true">
            <span className={styles.sortLabel}>Ordenar por:</span>
            <span className={styles.sortValue}>
              Categoría
              <Icon name="chevron" size={14} className={styles.sortChevron} />
            </span>
          </div>

          {/* Column headers mirror the item-row grid below. Decorative
              (aria-hidden): each item row already carries accessible labels on
              its own controls, so the header text isn't announced twice. */}
          <div className={styles.columns} aria-hidden="true">
            <span>Producto</span>
            <span className={styles.colCenter}>Cantidad</span>
            <span className={styles.colCenter}>Unidad</span>
            <span className={styles.colRight}>Precio</span>
            <span className={styles.colRight}>Acciones</span>
          </div>

          {/* Section heading for the visible slice (a11y/FOR-112): the table
              region needs an <h2> in the heading order; the visible structure
              is the column headers + category groups, so this label is
              sr-only. */}
          <h2 className={styles.srOnly}>{categoryLabel(selectedCategory)}</h2>

          {groups.map((group) => (
            <div key={group.key} className={styles.group}>
              <div className={styles.groupHeader}>
                <Icon name={categoryIcon(group.key)} size={18} className={styles.groupIcon} />
                <span className={styles.groupLabel}>{group.label}</span>
              </div>
              <ul className={styles.items}>
                {group.items.map((item) => (
                  <li key={item.id} className={styles.item}>
                    <span className={styles.product}>
                      <span className={item.checked ? styles.checkedName : styles.productName}>
                        {item.productName}
                      </span>
                      {/* Servings detail (FOR-108/FOR-117): omitted for non-food
                          items (`servings: null`), never fabricated. */}
                      {item.servings != null && (
                        <span className={styles.servings}>{item.servings} raciones</span>
                      )}
                    </span>

                    {/* Quantity +/- controls (FOR-109/FOR-118): disabled during
                        this item's own in-flight request or while a regenerate
                        is in flight, to avoid racing a per-item edit against a
                        full-list rebuild. The decrement is additionally
                        disabled at quantity 1 (client-side guard mirroring the
                        backend's `quantity >= 1` invariant). */}
                    <span className={styles.quantityStepper}>
                      <button
                        type="button"
                        className={styles.stepperButton}
                        aria-label={`Disminuir cantidad de ${item.productName}`}
                        disabled={item.quantity <= 1 || pendingId === item.id || regenerating}
                        onClick={() => onChangeQuantity(item, -1)}
                      >
                        −
                      </button>
                      <span className={styles.quantityValue}>{item.quantity}</span>
                      <button
                        type="button"
                        className={styles.stepperButton}
                        aria-label={`Aumentar cantidad de ${item.productName}`}
                        disabled={pendingId === item.id || regenerating}
                        onClick={() => onChangeQuantity(item, 1)}
                      >
                        +
                      </button>
                    </span>

                    <span className={styles.unit}>{unitLabel(item.unit)}</span>

                    <span className={styles.cost}>{EUR.format(item.estimatedCostEur)}</span>

                    <span className={styles.itemActions}>
                      {/* Edit product price/URL (FOR-36). */}
                      <button
                        type="button"
                        className={styles.actionButton}
                        onClick={() => onEdit(item)}
                        aria-label={`Editar producto ${item.productName}`}
                      >
                        <Icon name="edit" size={16} />
                      </button>
                      {/* Provider link-out (FOR-108/FOR-109/FOR-118): a real
                          link opening in a new tab; omitted (not disabled) when
                          the item has no productUrl. */}
                      {item.productUrl && (
                        <a
                          href={item.productUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className={styles.actionButton}
                          aria-label={`${item.productName} (se abre en una nueva pestaña)`}
                        >
                          <Icon name="externalLink" size={16} />
                        </a>
                      )}
                      {/* Checked toggle (FOR-63): a real checkbox styled as the
                          mockup's green "done" square; its accessible name is
                          the product name. */}
                      <input
                        type="checkbox"
                        className={styles.checkToggle}
                        aria-label={item.productName}
                        checked={item.checked}
                        disabled={pendingId === item.id || regenerating}
                        onChange={() => toggle(item)}
                      />
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          ))}

          {/* Full-list CTA — visual only (FOR-164 mockup): every item is already
              rendered, so this is a static footer affordance, not a paginator. */}
          <button type="button" className={styles.fullListButton}>
            Ver lista completa ({items.length} productos)
          </button>
        </Card>
      )}
    </>
  );
}

type ProductState =
  | { readonly status: 'loading' }
  | { readonly status: 'not-found' }
  | { readonly status: 'error'; readonly detail?: string }
  | { readonly status: 'ready'; readonly product: ShoppingProduct };

const PRODUCT_NOT_FOUND =
  'No se pudo encontrar el producto asociado a este artículo para editarlo.';
const PRODUCT_LOAD_ERROR = 'No se pudo cargar el producto. Inténtalo de nuevo.';
const PRODUCT_SAVE_ERROR = 'No se pudo guardar el producto. Inténtalo de nuevo.';

/**
 * Entry point to edit a product's price/URL (FOR-36), reached from a list item.
 *
 * <p>FOR-113: this modal's loading/error/not-found states now render the
 * FOR-60 shared components ({@link LoadingState}, {@link ErrorState},
 * {@link EmptyState}) instead of the ad hoc paragraphs this modal originally
 * shipped with, closing the deferral this doc comment used to document.
 * Not-found uses {@link EmptyState} rather than {@link ErrorState} — a
 * product id that no longer resolves is not a retry-recoverable failure, so
 * it stays {@code role="status"} instead of {@code role="alert"} (per the
 * Open Question recommendation in `specs/FOR-113/spec.md`), which also keeps
 * it visually/semantically distinct from the error case. No {@code onRetry}
 * is passed to the error state either: closing and reopening the modal
 * (which re-fetches) is the retry path for a nested flow like this one, not
 * a button. The error state also wires {@link ErrorState}'s dev-only
 * {@code detail}/{@code showDetail} props to the caught error's message,
 * gated by {@code import.meta.env.DEV} — the first real caller of that
 * escape hatch (FOR-113).
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
      .catch((error: unknown) => {
        if (active) {
          setState({
            status: 'error',
            detail: error instanceof ApiRequestError ? error.message : undefined,
          });
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
      {state.status === 'loading' && <LoadingState message="Cargando producto…" />}
      {state.status === 'error' && (
        <ErrorState
          message={PRODUCT_LOAD_ERROR}
          detail={state.detail}
          showDetail={import.meta.env.DEV}
        />
      )}
      {state.status === 'not-found' && <EmptyState title={PRODUCT_NOT_FOUND} />}
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
