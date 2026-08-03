import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button } from '../../components/Button';
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
  type StoreId,
  type StoreProduct,
} from '../../api/storeProducts';
import { CatalogTable, type CatalogColumn, type CatalogDetail } from './CatalogTable';
import { ImportFromStore } from './ImportFromStore';
import { Pagination } from './Pagination';
import { ProductThumbnail } from './ProductThumbnail';
import { StoreProductForm } from './StoreProductForm';
import { SHOPPING_CATEGORY_LABELS, priceLabel, shoppingCategoryGlyph } from './storeDisplay';
import { useCatalogAdmin } from './useCatalogAdmin';
import { useStores } from './useStores';
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

/**
 * The category glyph sits in the Categoría column (FOR-198). In front of the product's name it
 * competed with the shop's own photo, which is the picture of THIS product rather than of its
 * aisle — two icons saying different things in the same spot.
 */
const columnsWith = (
  categoryLabel: (product: StoreProduct) => string,
  categoryGlyphOf: (product: StoreProduct) => string,
  storeLabel: (id: string | undefined) => string,
): CatalogColumn<StoreProduct>[] => [
  {
    header: 'Tienda',
    value: (product) => storeLabel(product.store),
    sortBy: (product) => storeLabel(product.store),
  },
  {
    header: 'Categoría',
    value: (product) => (
      <span className={styles.withGlyph}>
        <span aria-hidden="true">{categoryGlyphOf(product)}</span>
        {categoryLabel(product)}
      </span>
    ),
    sortBy: categoryLabel,
  },
  // No sortBy: the format is free text ("Caja 0.8 kg", "kg", "Paquete 12 ud")
  // and its alphabetical order carries no meaning.
  { header: 'Formato', value: packageLabel },
  {
    header: 'Precio',
    value: (product) => priceLabel(product.priceEur),
    numeric: true,
    sortBy: (product) => product.priceEur,
  },
];

const COMPACT_COLUMNS: CatalogColumn<StoreProduct>[] = [
  {
    header: 'Precio',
    value: (product) => priceLabel(product.priceEur),
    numeric: true,
    sortBy: (product) => product.priceEur,
  },
];

/**
 * Built per render because the linked food reads better as its name than as its
 * slug, and the names come from a request. Falls back to the id when the food
 * catalog has not landed — an id is worse than a name but better than nothing.
 */
const detailsWith = (
  foodName: (id: string) => string,
  categoryLabel: (product: StoreProduct) => string,
  storeLabel: (id: string | undefined) => string,
): CatalogDetail<StoreProduct>[] => [
  { glyph: '🏪', label: 'Tienda', value: (product) => storeLabel(product.store) },
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
const storeLink = (product: StoreProduct, storeLabel: (id: string | undefined) => string) =>
  product.url ? (
    <a className={styles.linkPill} href={product.url} target="_blank" rel="noopener noreferrer">
      <Icon name="share" size={14} />
      {`Ver en ${storeLabel(product.store)}`}
    </a>
  ) : undefined;

interface StorePanelProps {
  /** The header asked for a new one; the form opens on this. */
  readonly creating: boolean;
  readonly onCreateClose: () => void;
}

/**
 * What each column sorts by (FOR-199), keyed by its header. Formato is absent on purpose — see the
 * column definition. The category sorts by its label rather than its code, because the label is
 * what is on screen and an alphabet nobody can see is not an order.
 */
const sortKeysWith = (
  storeLabel: (id: string | undefined) => string,
): Record<string, (product: StoreProduct) => string | number | undefined> => ({
  Producto: (product) => product.name,
  Tienda: (product) => storeLabel(product.store),
  Categoría: (product) => SHOPPING_CATEGORY_LABELS[product.category],
  Precio: (product) => product.priceEur,
});

export function StorePanel({ creating, onCreateClose }: StorePanelProps) {
  const notify = useNotify();
  const [store, setStore] = useState<StoreId | ''>('');
  // Memoised on the filter: `useCatalogAdmin` reloads whenever this changes,
  // which is exactly what picking a chain should do.
  const stores = useStores();
  const list = useCallback(() => listStoreProducts(store === '' ? undefined : store), [store]);
  const catalog = useCatalogAdmin<StoreProduct>({
    list,
    remove: deleteStoreProduct,
    deleteErrorMessage: 'No se pudo eliminar el producto. Inténtalo de nuevo.',
    sortKeys: sortKeysWith(stores.label),
  });
  const [editing, setEditing] = useState<StoreProduct | undefined>(undefined);
  const [deleting, setDeleting] = useState<StoreProduct | undefined>(undefined);
  const [foods, setFoods] = useState<CatalogFood[]>([]);

  // Closes whichever way the form was opened: an edit from a row, or a create
  // from the header — the panel does not own the second one.
  const [refreshing, setRefreshing] = useState<string | undefined>(undefined);
  const [searching, setSearching] = useState(false);
  const [draft, setDraft] = useState<StoreProduct | undefined>(undefined);

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
      notify.success(`${product.name} actualizado desde ${stores.label(product.store)}.`);
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
  const categoryGlyphOf = useCallback(
    (product: StoreProduct) =>
      categories.glyph(product.category, shoppingCategoryGlyph(product.category)),
    [categories],
  );
  const columns = useMemo(
    () => columnsWith(categoryLabel, categoryGlyphOf, stores.label),
    [categoryLabel, categoryGlyphOf, stores.label],
  );
  const details = useMemo(
    () =>
      detailsWith(
        (id) => foods.find((food) => food.id === id)?.name ?? id,
        categoryLabel,
        stores.label,
      ),
    [foods, categoryLabel, stores.label],
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
            onChange={(event) => setStore(event.target.value)}
          >
            <option value="">Todas</option>
            {stores.options.map((option) => (
              <option key={option.id} value={option.id}>
                {option.name}
              </option>
            ))}
          </SelectField>
        </div>
        {/* Disabled until a chain is chosen: "Todas" is not a shop anybody can
            search, and a button that asks which one after being pressed is a
            question the filter beside it already answers. */}
        <Button
          variant="secondary"
          type="button"
          disabled={store === ''}
          onClick={() => {
            catalog.setActionError(undefined);
            setSearching(true);
          }}
        >
          Importar desde tienda
        </Button>
      </div>

      {searching && store !== '' && (
        <Modal title={`Importar de ${stores.label(store)}`} onClose={() => setSearching(false)}>
          <ImportFromStore
            store={store}
            onCancel={() => setSearching(false)}
            onPicked={(picked) => {
              setSearching(false);
              setDraft(picked);
            }}
          />
        </Modal>
      )}

      {draft && (
        <Modal title="Nuevo producto" onClose={() => setDraft(undefined)}>
          <StoreProductForm
            draft={draft}
            foods={foods}
            onCancel={() => setDraft(undefined)}
            onSaved={() => {
              setDraft(undefined);
              catalog.reload();
            }}
          />
        </Modal>
      )}

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
            // The wide table had no way through to the shop: the link out lived
            // only in the phone layout's disclosure, and the name is the door
            // everybody reaches for anyway.
            renderName={(product) =>
              product.url ? (
                <a
                  className={styles.nameLink}
                  href={product.url}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  {product.name}
                </a>
              ) : (
                product.name
              )
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
            detailFooter={(product) => storeLink(product, stores.label)}
            nameSortBy={(product) => product.name}
            sort={catalog.sort}
            onSort={catalog.toggleSort}
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
