import { useCallback, useState } from 'react';
import { Card } from '../../components/Card';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { ErrorState } from '../../components/ErrorState';
import { LoadingState } from '../../components/LoadingState';
import { Modal } from '../../components/Modal';
import { deleteFood, listFoods, type CatalogFood } from '../../api/foods';
import { CatalogTable, type CatalogColumn, type CatalogDetail } from './CatalogTable';
import { FoodForm } from './FoodForm';
import { Pagination } from './Pagination';
import { CATEGORY_LABELS, categoryGlyph } from './foodDisplay';
import { useCatalogAdmin } from './useCatalogAdmin';
import styles from './panel.module.css';

/**
 * The Macros tab (FOR-190): the global food catalog every plan is built from.
 */
const categoryLabel = (food: CatalogFood) => (food.category ? CATEGORY_LABELS[food.category] : '—');
const serving = (food: CatalogFood) => (food.servingSizeG ? `${food.servingSizeG} g` : '—');

const COLUMNS: CatalogColumn<CatalogFood>[] = [
  { header: 'Categoría', value: categoryLabel },
  { header: 'kcal', value: (food) => food.kcal, numeric: true },
  { header: 'Prot.', value: (food) => food.proteinG, numeric: true },
  { header: 'HC', value: (food) => food.carbsG, numeric: true },
  { header: 'Grasa', value: (food) => food.fatG, numeric: true },
  { header: 'Ración', value: serving, numeric: true },
];

const COMPACT_COLUMNS: CatalogColumn<CatalogFood>[] = [
  { header: 'kcal', value: (food) => food.kcal, numeric: true },
  { header: 'Ración', value: serving, numeric: true },
];

/**
 * The ration is not among these: it is a column of its own at this width, and a
 * value shown twice is a value that can look like two different things.
 */
const DETAILS: CatalogDetail<CatalogFood>[] = [
  { glyph: '🏷️', label: 'Categoría', value: categoryLabel },
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

  // Closes whichever way the form was opened: an edit from a row, or a create
  // from the header — the panel does not own the second one.
  const closeForm = () => {
    setEditing(undefined);
    onCreateClose();
  };

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
            glyphOf={(food) => categoryGlyph(food.category)}
            label="Alimentos"
            nameHeader="Alimento"
            columns={COLUMNS}
            compactColumns={COMPACT_COLUMNS}
            details={DETAILS}
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
            food={editing}
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
