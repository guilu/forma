import { useCallback, useEffect, useState } from 'react';
import { Card } from '../../components/Card';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { ErrorState } from '../../components/ErrorState';
import { LoadingState } from '../../components/LoadingState';
import { Modal } from '../../components/Modal';
import { listFoods, type CatalogFood } from '../../api/foods';
import { deleteRecipe, listRecipes, type Recipe } from '../../api/recipes';
import { CatalogTable, type CatalogColumn, type CatalogDetail } from './CatalogTable';
import { Pagination } from './Pagination';
import { RecipeForm } from './RecipeForm';
import { useCatalogAdmin } from './useCatalogAdmin';
import styles from './panel.module.css';

/**
 * The Recetas tab (V52): dishes made of catalog foods.
 *
 * <p>Every figure on this screen is computed. A recipe stores no nutrition — its totals are the sum
 * over its ingredients of what the food catalog holds — so the numbers here move when somebody
 * corrects a food, and there is nowhere to type them.
 *
 * <p>Per serving rather than per dish in the columns: it is the figure anybody eating it wants, and
 * showing the whole thing next to a stew for four invites reading one as the other.
 */
const COLUMNS: CatalogColumn<Recipe>[] = [
  { header: 'Raciones', value: (recipe) => String(recipe.servings), numeric: true },
  { header: 'kcal / ración', value: (recipe) => String(recipe.perServing.calories), numeric: true },
  { header: 'P', value: (recipe) => `${recipe.perServing.proteinG} g`, numeric: true },
  { header: 'HC', value: (recipe) => `${recipe.perServing.carbsG} g`, numeric: true },
  { header: 'G', value: (recipe) => `${recipe.perServing.fatG} g`, numeric: true },
];

const COMPACT_COLUMNS: CatalogColumn<Recipe>[] = [
  { header: 'kcal / ración', value: (recipe) => String(recipe.perServing.calories), numeric: true },
];

const DETAILS: CatalogDetail<Recipe>[] = [
  { glyph: '🍽️', label: 'Raciones', value: (recipe) => String(recipe.servings) },
  { glyph: '🥣', label: 'Ingredientes', value: (recipe) => String(recipe.ingredients.length) },
  { glyph: '🔥', label: 'kcal totales', value: (recipe) => String(recipe.total.calories) },
  {
    glyph: '🥩',
    label: 'Proteínas / ración',
    value: (recipe) => `${recipe.perServing.proteinG} g`,
  },
];

const SORT_KEYS: Record<string, (recipe: Recipe) => string | number | undefined> = {
  Receta: (recipe) => recipe.name,
  Raciones: (recipe) => recipe.servings,
  'kcal / ración': (recipe) => recipe.perServing.calories,
};

interface RecipesPanelProps {
  /** The header asked for a new one; the form opens on this. */
  readonly creating: boolean;
  readonly onCreateClose: () => void;
}

export function RecipesPanel({ creating, onCreateClose }: RecipesPanelProps) {
  const list = useCallback(() => listRecipes(), []);
  const catalog = useCatalogAdmin<Recipe>({
    list,
    remove: deleteRecipe,
    deleteErrorMessage: 'No se pudo eliminar la receta. Inténtalo de nuevo.',
    sortKeys: SORT_KEYS,
  });
  const [editing, setEditing] = useState<Recipe | undefined>(undefined);
  const [deleting, setDeleting] = useState<Recipe | undefined>(undefined);
  // The catalog, for the ingredient pickers. Loaded here rather than in the form so opening the
  // modal does not wait on a request the panel could have made already.
  const [foods, setFoods] = useState<CatalogFood[]>([]);

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

  const closeForm = () => {
    setEditing(undefined);
    onCreateClose();
  };

  if (catalog.state.status === 'loading') {
    return <LoadingState message="Cargando recetas…" />;
  }
  if (catalog.state.status === 'error') {
    return <ErrorState message="No se pudieron cargar las recetas." onRetry={catalog.reload} />;
  }

  return (
    <>
      <Card>
        {catalog.actionError && (
          <p className={styles.error} role="alert">
            {catalog.actionError}
          </p>
        )}
        {catalog.state.rows.length === 0 ? (
          <p className={styles.empty}>
            Todavía no hay recetas. Una receta es un plato hecho de alimentos del catálogo, y sus
            macros salen de ellos.
          </p>
        ) : (
          <>
            <CatalogTable
              rows={catalog.visible}
              idOf={(recipe) => recipe.id}
              nameOf={(recipe) => recipe.name}
              glyphOf={() => '🥣'}
              label="Recetas"
              nameHeader="Receta"
              columns={COLUMNS}
              compactColumns={COMPACT_COLUMNS}
              details={DETAILS}
              narrow={catalog.narrow}
              expandedId={catalog.expandedId}
              onToggle={catalog.toggle}
              onEdit={(recipe) => {
                catalog.setActionError(undefined);
                setEditing(recipe);
              }}
              onDelete={(recipe) => {
                catalog.setActionError(undefined);
                setDeleting(recipe);
              }}
              nameSortBy={(recipe) => recipe.name}
              sort={catalog.sort}
              onSort={catalog.toggleSort}
            />
            <Pagination
              page={catalog.page}
              pageCount={catalog.pageCount}
              onChange={catalog.goToPage}
            />
          </>
        )}
      </Card>

      {(creating || editing) && (
        <Modal title={editing ? `Editar ${editing.name}` : 'Nueva receta'} onClose={closeForm}>
          <RecipeForm
            recipe={editing}
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
          title={`Eliminar ${deleting.name}`}
          message="La receta y su lista de ingredientes se eliminan. Los alimentos no se tocan."
          confirmLabel="Eliminar"
          onConfirm={() => catalog.confirmDelete(deleting.id, () => setDeleting(undefined))}
          onCancel={() => setDeleting(undefined)}
        />
      )}
    </>
  );
}
