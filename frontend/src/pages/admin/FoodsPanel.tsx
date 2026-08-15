import { useCallback, useMemo, useState } from 'react';
import { Card } from '../../components/Card';
import { Icon } from '../../components/Icon';
import { IconButton } from '../../components/IconButton';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { ErrorState } from '../../components/ErrorState';
import { LoadingState } from '../../components/LoadingState';
import { Modal } from '../../components/Modal';
import { deleteFood, listFoods, type CatalogFood, type PrimaryMacro } from '../../api/foods';

/** How each macronutrient reads. A closed set for good: there are three. */
const MACRO_LABELS: Record<PrimaryMacro, string> = {
  PROTEIN: 'Proteínas',
  CARBS: 'Hidratos',
  FAT: 'Grasas',
};
import { listStoreProducts, type StoreProduct } from '../../api/storeProducts';
import { CatalogTable, type CatalogColumn, type CatalogDetail } from './CatalogTable';
import { FoodForm } from './FoodForm';
import { ImportFromStore } from './ImportFromStore';
import { EquivalencesManager } from './EquivalencesManager';
import { ServingsManager } from './ServingsManager';
import { StoreProductForm } from './StoreProductForm';
import { Pagination } from './Pagination';
import { useCatalogAdmin } from './useCatalogAdmin';
import { useCategoryDisplays } from './useCategoryDisplays';
import styles from './panel.module.css';

/**
 * The Macros tab (FOR-190): the global food catalog every plan is built from.
 */
const serving = (food: CatalogFood) => (food.servingSizeG ? `${food.servingSizeG} g` : '—');

const COMPACT_COLUMNS: CatalogColumn<CatalogFood>[] = [
  { header: 'kcal', value: (food) => food.kcal, numeric: true },
  { header: 'Ración', value: serving, numeric: true },
];

/**
 * Built per render: the category label comes from a request now (FOR-197).
 *
 * <p>The glyph rides in the Categoría column rather than in front of the food's
 * name (FOR-198). In front of the name it read as that food's own icon, which it
 * is not — every carbohydrate wore the same wheat ear. Beside the word it is what
 * it always was: a second reading of the category.
 */
const columnsWith = (
  categoryLabel: (food: CatalogFood) => string,
  categoryGlyphOf: (food: CatalogFood) => string,
): CatalogColumn<CatalogFood>[] => [
  {
    header: 'Categoría',
    value: (food) => (
      <span className={styles.withGlyph}>
        <span aria-hidden="true">{categoryGlyphOf(food)}</span>
        {categoryLabel(food)}
      </span>
    ),
  },
  { header: 'kcal', value: (food) => food.kcal, numeric: true },
  { header: 'Prot.', value: (food) => food.proteinG, numeric: true },
  { header: 'HC', value: (food) => food.carbsG, numeric: true },
  { header: 'Grasa', value: (food) => food.fatG, numeric: true },
  { header: 'Ración', value: serving, numeric: true },
];

/**
 * Which macro the food is mostly made of, by calories. Absent for a food whose
 * numbers decide nothing — water and a two-way tie both land here — and an em
 * dash says that more honestly than picking one would.
 */
const macroLabel = (food: CatalogFood) =>
  food.primaryMacro ? MACRO_LABELS[food.primaryMacro] : '—';

/**
 * The ration is not among these: it is a column of its own at this width, and a
 * value shown twice is a value that can look like two different things.
 */
const detailsWith = (
  categoryLabel: (food: CatalogFood) => string,
): CatalogDetail<CatalogFood>[] => [
  { glyph: '🏷️', label: 'Categoría', value: categoryLabel },
  { glyph: '⚖️', label: 'Macro principal', value: macroLabel },
  { glyph: '🍞', label: 'HC (hidratos)', value: (food) => `${food.carbsG} g` },
  { glyph: '🥩', label: 'Proteínas', value: (food) => `${food.proteinG} g` },
  { glyph: '💧', label: 'Grasa', value: (food) => `${food.fatG} g` },
];

interface FoodsPanelProps {
  /** The header asked for a new one; the form opens on this. */
  readonly creating: boolean;
  readonly onCreateClose: () => void;
}

export function FoodsPanel({ creating, onCreateClose }: FoodsPanelProps) {
  const list = useCallback(() => listFoods(), []);
  const catalog = useCatalogAdmin<CatalogFood>({
    list,
    remove: deleteFood,
    deleteErrorMessage: 'No se pudo eliminar el alimento. Puede estar en uso por un producto.',
  });
  const [editing, setEditing] = useState<CatalogFood | undefined>(undefined);
  const [deleting, setDeleting] = useState<CatalogFood | undefined>(undefined);
  // Importing starts here because it is a question about a FOOD — "what does
  // Mercadona sell for this?" — even though what it produces is a store product.
  const [importingFor, setImportingFor] = useState<CatalogFood | undefined>(undefined);
  const [portioning, setPortioning] = useState<CatalogFood | undefined>(undefined);
  const [substituting, setSubstituting] = useState<CatalogFood | undefined>(undefined);
  const [draft, setDraft] = useState<StoreProduct | undefined>(undefined);
  const [existing, setExisting] = useState<StoreProduct | undefined>(undefined);
  const categories = useCategoryDisplays('FOOD');
  const categoryLabel = useCallback(
    (food: CatalogFood) =>
      // Falls back to the stored code, not to a bundled label: since V43 the set
      // of groups is data, so any table here would be a copy that goes stale and
      // still misses whatever was added last. A code reads worse than a name and
      // better than a blank.
      categories.label(food.foodGroupId, food.foodGroupId ?? '—'),
    [categories],
  );
  const categoryGlyphOf = useCallback(
    // A neutral plate until the real glyph lands, and for a group that has none.
    (food: CatalogFood) => categories.glyph(food.foodGroupId, '🍽️'),
    [categories],
  );
  const columns = useMemo(
    () => columnsWith(categoryLabel, categoryGlyphOf),
    [categoryLabel, categoryGlyphOf],
  );
  const details = useMemo(() => detailsWith(categoryLabel), [categoryLabel]);

  // Closes whichever way the form was opened: an edit from a row, or a create
  // from the header — the panel does not own the second one.
  const closeForm = () => {
    setEditing(undefined);
    onCreateClose();
  };

  const closeImport = () => {
    setDraft(undefined);
    setExisting(undefined);
  };

  /**
   * Importing the same product twice updates the row that exists instead of
   * failing on its id. The check runs here, against the catalog as it is now,
   * because the picker only knows what the shop sells — not what we already
   * took from it.
   */
  async function pick(picked: StoreProduct) {
    setImportingFor(undefined);
    try {
      const products = await listStoreProducts(picked.store);
      const match = products.find((product) => product.externalId === picked.externalId);
      if (match) {
        // Keep our curation (aisle, notes, food link if it differs) and take the
        // shop's current figures over the stored ones.
        setExisting({ ...match, ...picked, category: match.category, notes: match.notes });
        return;
      }
    } catch {
      // Not being able to check is not a reason to refuse the import: the create
      // call will refuse a duplicate id on its own, with a message that says so.
    }
    setDraft(picked);
  }

  return (
    <>
      {catalog.actionError && (
        <p className={styles.actionError} role="alert">
          {catalog.actionError}
        </p>
      )}
      {catalog.state.status === 'loading' && <LoadingState message="Cargando el catálogo…" />}
      {catalog.state.status === 'error' && (
        <ErrorState message="No se pudo cargar el catálogo." onRetry={catalog.reload} />
      )}
      {catalog.state.status === 'ready' && (
        <Card>
          <CatalogTable
            rows={catalog.visible}
            idOf={(food) => food.id}
            nameOf={(food) => food.name}
            label="Alimentos"
            nameHeader="Alimento"
            columns={columns}
            compactColumns={COMPACT_COLUMNS}
            details={details}
            detailBadge="/100 g"
            narrow={catalog.narrow}
            expandedId={catalog.expandedId}
            onToggle={catalog.toggle}
            onEdit={(food) => {
              catalog.setActionError(undefined);
              setEditing(food);
            }}
            onDelete={(food) => {
              catalog.setActionError(undefined);
              setDeleting(food);
            }}
            extraActions={(food) => (
              <>
                <IconButton
                  variant="surface"
                  label={`Raciones de ${food.name}`}
                  onClick={() => {
                    catalog.setActionError(undefined);
                    setPortioning(food);
                  }}
                >
                  <Icon name="measurements" size={18} />
                </IconButton>
                <IconButton
                  variant="surface"
                  label={`Importar ${food.name} de Mercadona`}
                  onClick={() => {
                    catalog.setActionError(undefined);
                    setImportingFor(food);
                  }}
                >
                  <Icon name="shopping" size={18} />
                </IconButton>
              </>
            )}
          />
          <Pagination
            page={catalog.page}
            pageCount={catalog.pageCount}
            onChange={catalog.goToPage}
          />
        </Card>
      )}

      {(editing || creating) && (
        <Modal title={editing ? `Editar ${editing.name}` : 'Nuevo alimento'} onClose={closeForm}>
          <FoodForm
            groups={categories.options}
            food={editing}
            onCancel={closeForm}
            onSaved={() => {
              closeForm();
              catalog.reload();
            }}
          />
        </Modal>
      )}

      {portioning && (
        <Modal title={`Raciones de ${portioning.name}`} onClose={() => setPortioning(undefined)}>
          <ServingsManager food={portioning} onClose={() => setPortioning(undefined)} />
        </Modal>
      )}

      {substituting && (
        <Modal
          title={`Equivalencias de ${substituting.name}`}
          onClose={() => setSubstituting(undefined)}
        >
          <EquivalencesManager
            food={substituting}
            // The whole catalog, not the page on screen: the food that replaces
            // this one may well be on another page.
            foods={catalog.state.status === 'ready' ? catalog.state.rows : []}
            onClose={() => setSubstituting(undefined)}
          />
        </Modal>
      )}

      {importingFor && (
        <Modal title="Importar de Mercadona" onClose={() => setImportingFor(undefined)}>
          <ImportFromStore
            store="MERCADONA"
            food={importingFor}
            onCancel={() => setImportingFor(undefined)}
            onPicked={pick}
          />
        </Modal>
      )}

      {(draft || existing) && (
        <Modal
          title={existing ? `Actualizar ${existing.name}` : 'Nuevo producto'}
          onClose={closeImport}
        >
          <StoreProductForm
            product={existing}
            draft={draft}
            foods={catalog.state.status === 'ready' ? catalog.state.rows : []}
            onCancel={closeImport}
            onSaved={closeImport}
          />
        </Modal>
      )}

      {deleting && (
        <ConfirmDialog
          title="Eliminar alimento"
          message={`¿Seguro que quieres eliminar "${deleting.name}" del catálogo? Es referencia compartida: si algún producto lo enlaza, el borrado se rechazará.`}
          confirmLabel="Eliminar"
          pending={catalog.pending}
          onConfirm={() => catalog.confirmDelete(deleting.id, () => setDeleting(undefined))}
          onCancel={() => setDeleting(undefined)}
        />
      )}
    </>
  );
}
