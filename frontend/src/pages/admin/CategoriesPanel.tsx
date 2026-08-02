import { useCallback, useState } from 'react';
import { Card } from '../../components/Card';
import { ErrorState } from '../../components/ErrorState';
import { LoadingState } from '../../components/LoadingState';
import { Modal } from '../../components/Modal';
import { listCategories, type CategoryDisplay } from '../../api/categories';
import { CatalogTable, type CatalogColumn, type CatalogDetail } from './CatalogTable';
import { CategoryForm } from './CategoryForm';
import { Pagination } from './Pagination';
import { useCatalogAdmin } from './useCatalogAdmin';
import styles from './panel.module.css';

/**
 * The Categorías tab (FOR-197): how each category is written and drawn.
 *
 * <p>Both vocabularies in one table, told apart by a column. `FOOD` files an
 * ingredient by what it is made of and `SHOPPING` files a product by which aisle
 * it sits in — "Proteína" is in both and means a different thing in each, so they
 * are listed side by side rather than merged.
 *
 * <p>Edit only. Which categories exist is fixed by the domain enums and by the
 * database's CHECK constraints: a category created here could never be filed
 * under anything, and one deleted here would leave the rows using it with nothing
 * to render. An action that always fails is worse than no action, so the screen
 * offers neither.
 */
const SCOPE_LABELS: Record<CategoryDisplay['scope'], string> = {
  FOOD: 'Macros',
  SHOPPING: 'Compra',
};

const COLUMNS: CatalogColumn<CategoryDisplay>[] = [
  { header: 'Catálogo', value: (category) => SCOPE_LABELS[category.scope] },
  // The stored token: an admin renaming a category should see what the rows
  // underneath actually hold, and that it is not what they are editing.
  { header: 'Código', value: (category) => category.code },
];

const COMPACT_COLUMNS: CatalogColumn<CategoryDisplay>[] = [
  { header: 'Catálogo', value: (category) => SCOPE_LABELS[category.scope] },
];

const DETAILS: CatalogDetail<CategoryDisplay>[] = [
  { glyph: '🗂️', label: 'Catálogo', value: (category) => SCOPE_LABELS[category.scope] },
  { glyph: '🔖', label: 'Código', value: (category) => category.code },
];

export function CategoriesPanel() {
  const list = useCallback(() => listCategories(), []);
  const catalog = useCatalogAdmin<CategoryDisplay>({
    list,
    // Never called: the table renders no delete for a set nothing may add to.
    remove: () => Promise.reject(new Error('Las categorías no se borran')),
    deleteErrorMessage: 'Las categorías no se pueden borrar.',
  });
  const [editing, setEditing] = useState<CategoryDisplay | undefined>(undefined);

  return (
    <>
      {catalog.actionError && (
        <p className={styles.actionError} role="alert">
          {catalog.actionError}
        </p>
      )}
      {catalog.state.status === 'loading' && <LoadingState message="Cargando las categorías…" />}
      {catalog.state.status === 'error' && (
        <ErrorState message="No se pudieron cargar las categorías." onRetry={catalog.reload} />
      )}
      {catalog.state.status === 'ready' && (
        <Card>
          <CatalogTable
            rows={catalog.visible}
            idOf={(category) => `${category.scope}-${category.code}`}
            nameOf={(category) => category.label}
            glyphOf={(category) => category.icon ?? '🏷️'}
            label="Categorías"
            nameHeader="Categoría"
            columns={COLUMNS}
            compactColumns={COMPACT_COLUMNS}
            details={DETAILS}
            narrow={catalog.narrow}
            expandedId={catalog.expandedId}
            onToggle={catalog.toggle}
            onEdit={(category) => {
              catalog.setActionError(undefined);
              setEditing(category);
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
        <Modal title={`Editar ${editing.label}`} onClose={() => setEditing(undefined)}>
          <CategoryForm
            category={editing}
            onCancel={() => setEditing(undefined)}
            onSaved={() => {
              setEditing(undefined);
              catalog.reload();
            }}
          />
        </Modal>
      )}
    </>
  );
}
