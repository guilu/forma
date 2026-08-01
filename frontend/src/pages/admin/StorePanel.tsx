import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button } from '../../components/Button';
import { Card } from '../../components/Card';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { ErrorState } from '../../components/ErrorState';
import { LoadingState } from '../../components/LoadingState';
import { Modal } from '../../components/Modal';
import { SelectField } from '../../components/FormField';
import { listFoods, type CatalogFood } from '../../api/foods';
import {
  deleteStoreProduct,
  listStoreProducts,
  type Store,
  type StoreProduct,
} from '../../api/storeProducts';
import { CatalogTable, type CatalogColumn, type CatalogDetail } from './CatalogTable';
import { Pagination } from './Pagination';
import { StoreProductForm } from './StoreProductForm';
import {
  SHOPPING_CATEGORY_LABELS,
  STORE_LABELS,
  STORE_OPTIONS,
  priceLabel,
  shoppingCategoryGlyph,
} from './storeDisplay';
import { useCatalogAdmin } from './useCatalogAdmin';
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
const categoryLabel = (product: StoreProduct) => SHOPPING_CATEGORY_LABELS[product.category];
const packageLabel = (product: StoreProduct) => product.packageSize ?? '—';

const COLUMNS: CatalogColumn<StoreProduct>[] = [
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
const detailsWith = (foodName: (id: string) => string): CatalogDetail<StoreProduct>[] => [
  { glyph: '🏪', label: 'Tienda', value: (product) => STORE_LABELS[product.store] },
  { glyph: '🏷️', label: 'Categoría', value: categoryLabel },
  { glyph: '📦', label: 'Formato', value: packageLabel },
  {
    glyph: '🍽️',
    label: 'Alimento',
    value: (product) => (product.foodId ? foodName(product.foodId) : 'Sin enlazar'),
  },
];

export function StorePanel() {
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
  const details = useMemo(
    () => detailsWith((id) => foods.find((food) => food.id === id)?.name ?? id),
    [foods],
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
        <Button
          variant="accent"
          type="button"
          onClick={() => {
            catalog.setActionError(undefined);
            setEditing(NEW_PRODUCT);
          }}
        >
          + Producto
        </Button>
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
            glyphOf={(product) => shoppingCategoryGlyph(product.category)}
            label="Productos"
            nameHeader="Producto"
            columns={COLUMNS}
            compactColumns={COMPACT_COLUMNS}
            details={details}
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

      {editing && (
        <Modal
          title={editing === NEW_PRODUCT ? 'Nuevo producto' : `Editar ${editing.name}`}
          onClose={() => setEditing(undefined)}
        >
          <StoreProductForm
            product={editing === NEW_PRODUCT ? undefined : editing}
            foods={foods}
            onCancel={() => setEditing(undefined)}
            onSaved={() => {
              setEditing(undefined);
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

/** Sentinel for "the form is open on a product that does not exist yet". */
const NEW_PRODUCT = {
  id: '',
  store: 'MERCADONA',
  name: '',
  category: 'OTROS',
} as StoreProduct;
