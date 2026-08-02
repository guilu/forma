import { useCallback, useEffect, useMemo, useState } from 'react';
import { Card } from '../../components/Card';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { ErrorState } from '../../components/ErrorState';
import { LoadingState } from '../../components/LoadingState';
import { Icon } from '../../components/Icon';
import { Modal } from '../../components/Modal';
import { SelectField } from '../../components/FormField';
import { useNotify } from '../../components/NotificationProvider';
import { ApiRequestError } from '../../api/client';
import { listFoods, type CatalogFood } from '../../api/foods';
import {
  deleteStoreProduct,
  listStoreProducts,
  refreshStoreProduct,
  type Store,
  type StoreProduct,
} from '../../api/storeProducts';
import { CatalogTable, type CatalogColumn, type CatalogDetail } from './CatalogTable';
import { Pagination } from './Pagination';
import { ProductThumbnail } from './ProductThumbnail';
import { StoreProductForm } from './StoreProductForm';
import {
  SHOPPING_CATEGORY_LABELS,
  STORE_LABELS,
  STORE_OPTIONS,
  priceLabel,
  shoppingCategoryGlyph,
} from './storeDisplay';
import { useCatalogAdmin } from './useCatalogAdmin';
import { useCategoryDisplays } from './useCategoryDisplays';
import styles from './panel.module.css';

/**
 * The Compra tab (FOR-191): the global catalog of what can be bought and where.
 *
 * <p>One tab for every chain, not one per supermarket. The store is a column in
 * V36 and a filter here, so adding Carrefour is data rather than a new tab —
 * which is also why the tab is named after the job ("Compra") and not after the
 * only chain that currently has rows in it.
 *
 * <p>Distinct from the user-facing shopping list: that is each account's own
 * copy of what they are buying this week. This is the shared reference the list
 * will eventually be built from.
 */
const fallbackLabel = (product: StoreProduct) => SHOPPING_CATEGORY_LABELS[product.category];
const packageLabel = (product: StoreProduct) => product.packageSize ?? '—';

const columnsWith = (
  categoryLabel: (product: StoreProduct) => string,
): CatalogColumn<StoreProduct>[] => [
  { header: 'Tienda', value: (product) => STORE_LABELS[product.store] },
  { header: 'Categoría', value: categoryLabel },
  { header: 'Formato', value: packageLabel },
  { header: 'Precio', value: (product) => priceLabel(product.priceEur), numeric: true },
];

const COMPACT_COLUMNS: CatalogColumn<StoreProduct>[] = [
  { header: 'Precio', value: (product) => priceLabel(product.priceEur), numeric: true },
];

/**
 * Built per render because the linked food reads better as its name than as its
 * slug, and the names come from a request. Falls back to the id when the food
 * catalog has not landed — an id is worse than a name but better than nothing.
 */
const detailsWith = (
  foodName: (id: string) => string,
  categoryLabel: (product: StoreProduct) => string,
): CatalogDetail<StoreProduct>[] => [
  { glyph: '🏪', label: 'Tienda', value: (product) => STORE_LABELS[product.store] },
  { glyph: '🏷️', label: 'Categoría', value: categoryLabel },
  { glyph: '📦', label: 'Formato', value: packageLabel },
  {
    glyph: '🍽️',
    label: 'Alimento',
    value: (product) => (product.foodId ? foodName(product.foodId) : 'Sin enlazar'),
  },
];

/**
 * The link out to the shelf, as a pill under the figures.
 *
 * <p>Absent rather than disabled when the product has no url: a dead link that looks live is worse
 * than no link. `noopener` because the destination is somebody else's site.
 */
const storeLink = (product: StoreProduct) =>
  product.url ? (
    <a className={styles.linkPill} href={product.url} target="_blank" rel="noopener noreferrer">
      <Icon name="share" size={14} />
      {`Ver en ${STORE_LABELS[product.store]}`}
    </a>
  ) : undefined;

interface StorePanelProps {
  /** The header asked for a new one; the form opens on this. */
  readonly creating: boolean;
  readonly onCreateClose: () => void;
}

export function StorePanel({ creating, onCreateClose }: StorePanelProps) {
  const notify = useNotify();
  const [store, setStore] = useState<Store | ''>('');
  // Memoised on the filter: `useCatalogAdmin` reloads whenever this changes,
  // which is exactly what picking a chain should do.
  const list = useCallback(() => listStoreProducts(store === '' ? undefined : store), [store]);
  const catalog = useCatalogAdmin<StoreProduct>({
    list,
    remove: deleteStoreProduct,
    deleteErrorMessage: 'No se pudo eliminar el producto. Inténtalo de nuevo.',
  });
  const [editing, setEditing] = useState<StoreProduct | undefined>(undefined);
  const [deleting, setDeleting] = useState<StoreProduct | undefined>(undefined);
  const [foods, setFoods] = useState<CatalogFood[]>([]);

  // Closes whichever way the form was opened: an edit from a row, or a create
  // from the header — the panel does not own the second one.
  const [refreshing, setRefreshing] = useState<string | undefined>(undefined);

  /**
   * Re-reads one product from its shop. Reloads the list afterwards rather than
   * patching the row in place: the server decides what a refresh changed, and a
   * locally merged row would be this screen's guess at it.
   */
  async function refresh(product: StoreProduct) {
    setRefreshing(product.id);
    catalog.setActionError(undefined);
    try {
      await refreshStoreProduct(product.id);
      notify.success(`${product.name} actualizado desde ${STORE_LABELS[product.store]}.`);
      catalog.reload();
    } catch (caught) {
      catalog.setActionError(
        caught instanceof ApiRequestError
          ? caught.message
          : `No se pudo actualizar ${product.name}. Inténtalo de nuevo.`,
      );
    } finally {
      setRefreshing(undefined);
    }
  }

  const closeForm = () => {
    setEditing(undefined);
    onCreateClose();
  };
  const categories = useCategoryDisplays('SHOPPING');
  const categoryLabel = useCallback(
    (product: StoreProduct) => categories.label(product.category, fallbackLabel(product)),
    [categories],
  );
  const columns = useMemo(() => columnsWith(categoryLabel), [categoryLabel]);
  const details = useMemo(
    () => detailsWith((id) => foods.find((food) => food.id === id)?.name ?? id, categoryLabel),
    [foods, categoryLabel],
  );

  // The form links a product to a food by id, so it needs the food catalog. A
  // failure here leaves the select empty rather than blocking the tab: every
  // other field still edits, and "sin enlazar" stays available.
  useEffect(() => {
    let active = true;
    listFoods()
      .then((rows) => {
        if (active) setFoods(rows);
      })
      .catch(() => undefined);
    return () => {
      active = false;
    };
  }, []);

  return (
    <>
      {catalog.actionError && (
        <p className={styles.actionError} role="alert">
          {catalog.actionError}
        </p>
      )}
      <div className={styles.toolbar}>
        <div className={styles.filter}>
          <SelectField
            id="store-filter"
            label="Tienda"
            value={store}
            onChange={(event) => setStore(event.target.value as Store | '')}
          >
            <option value="">Todas</option>
            {STORE_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {STORE_LABELS[option]}
              </option>
            ))}
          </SelectField>
        </div>
      </div>

      {catalog.state.status === 'loading' && <LoadingState message="Cargando el catálogo…" />}
      {catalog.state.status === 'error' && (
        <ErrorState message="No se pudo cargar el catálogo." onRetry={catalog.reload} />
      )}
      {catalog.state.status === 'ready' && (
        <Card>
          <CatalogTable
            rows={catalog.visible}
            idOf={(product) => product.id}
            nameOf={(product) => product.name}
            glyphOf={(product) =>
              categories.glyph(product.category, shoppingCategoryGlyph(product.category))
            }
            mediaOf={(product) => <ProductThumbnail url={product.imageUrl} />}
            extraActions={(product) =>
              // Only what a shop can be asked about: a hand-typed row has no
              // source behind it, so the action is absent rather than disabled.
              product.externalId ? (
                <button
                  type="button"
                  className={styles.rowAction}
                  aria-label={`Refrescar ${product.name}`}
                  disabled={refreshing === product.id}
                  onClick={() => refresh(product)}
                >
                  <Icon name="refresh" size={18} />
                </button>
              ) : null
            }
            label="Productos"
            nameHeader="Producto"
            columns={columns}
            compactColumns={COMPACT_COLUMNS}
            details={details}
            detailFooter={storeLink}
            narrow={catalog.narrow}
            expandedId={catalog.expandedId}
            onToggle={catalog.toggle}
            onEdit={(product) => {
              catalog.setActionError(undefined);
              setEditing(product);
            }}
            onDelete={(product) => {
              catalog.setActionError(undefined);
              setDeleting(product);
            }}
          />
          <Pagination
            page={catalog.page}
            pageCount={catalog.pageCount}
            onChange={catalog.goToPage}
          />
        </Card>
      )}

      {(editing || creating) && (
        <Modal title={editing ? `Editar ${editing.name}` : 'Nuevo producto'} onClose={closeForm}>
          <StoreProductForm
            product={editing}
            foods={foods}
            onCancel={closeForm}
            onSaved={() => {
              closeForm();
              catalog.reload();
            }}
          />
        </Modal>
      )}

      {deleting && (
        <ConfirmDialog
          title="Eliminar producto"
          message={`¿Seguro que quieres eliminar "${deleting.name}" del catálogo de compra? Es referencia compartida por todas las cuentas.`}
          confirmLabel="Eliminar"
          pending={catalog.pending}
          onConfirm={() => catalog.confirmDelete(deleting.id, () => setDeleting(undefined))}
          onCancel={() => setDeleting(undefined)}
        />
      )}
    </>
  );
}
