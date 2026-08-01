import { Fragment } from 'react';
import { Icon } from '../../components/Icon';
import type { CatalogFood } from '../../api/foods';
import { CATEGORY_LABELS } from './foodDisplay';
import styles from './FoodTable.module.css';

/**
 * The catalog table (FOR-190), in the two shapes it needs.
 *
 * <p>Seven columns do not fit a phone. The first attempt let the table scroll
 * sideways inside its card, which reads badly — half the row is off screen, the
 * actions are the part that falls off, and a sideways flick started outside the
 * card scrolled the whole page instead.
 *
 * <p>So the narrow layout drops to what an admin actually scans by — name and
 * kcal — and moves the rest behind a row disclosure: tap the name, the macros
 * and the two actions unfold underneath. Progressive disclosure rather than a
 * second data source: same rows, same page, same order, only the columns
 * differ. One row is open at a time, because two detail panels on a 390 px
 * screen leave nothing of the table visible.
 *
 * <p>The alternative — a card per food — was rejected: it loses the column
 * alignment that makes a catalog comparable at a glance, which is the whole
 * reason this screen is a table and not a list.
 */
interface FoodTableProps {
  readonly foods: readonly CatalogFood[];
  /** Phone layout when true; the caller owns the media query. */
  readonly narrow: boolean;
  /** Id of the row unfolded on the phone layout; ignored when wide. */
  readonly expandedId?: string;
  readonly onToggle: (id: string) => void;
  readonly onEdit: (food: CatalogFood) => void;
  readonly onDelete: (food: CatalogFood) => void;
}

const categoryLabel = (food: CatalogFood) => (food.category ? CATEGORY_LABELS[food.category] : '—');

const serving = (food: CatalogFood) => (food.servingSizeG ? `${food.servingSizeG} g` : '—');

export function FoodTable({
  foods,
  narrow,
  expandedId,
  onToggle,
  onEdit,
  onDelete,
}: FoodTableProps) {
  const actions = (food: CatalogFood) => (
    <div className={styles.rowActions}>
      <button
        type="button"
        className={styles.rowAction}
        aria-label={`Editar ${food.name}`}
        onClick={() => onEdit(food)}
      >
        <Icon name="edit" size={18} />
      </button>
      <button
        type="button"
        className={styles.rowAction}
        aria-label={`Eliminar ${food.name}`}
        onClick={() => onDelete(food)}
      >
        <Icon name="trash" size={18} />
      </button>
    </div>
  );

  if (narrow) {
    return (
      <table className={styles.table}>
        <thead>
          <tr>
            <th scope="col">Alimento</th>
            <th scope="col" className={styles.numeric}>
              kcal
            </th>
          </tr>
        </thead>
        <tbody>
          {foods.map((food) => {
            const open = food.id === expandedId;
            return (
              <Fragment key={food.id}>
                <tr>
                  <td>
                    <button
                      type="button"
                      className={styles.rowToggle}
                      aria-expanded={open}
                      aria-controls={`food-detail-${food.id}`}
                      onClick={() => onToggle(food.id)}
                    >
                      <Icon
                        name="chevron"
                        size={16}
                        className={open ? styles.chevronOpen : styles.chevron}
                      />
                      {food.name}
                    </button>
                  </td>
                  <td className={styles.numeric}>{food.kcal}</td>
                </tr>
                {open && (
                  <tr id={`food-detail-${food.id}`}>
                    <td colSpan={2} className={styles.detailCell}>
                      <dl className={styles.detail}>
                        <div>
                          <dt>Categoría</dt>
                          <dd>{categoryLabel(food)}</dd>
                        </div>
                        <div>
                          <dt>Prot.</dt>
                          <dd>{food.proteinG}</dd>
                        </div>
                        <div>
                          <dt>HC</dt>
                          <dd>{food.carbsG}</dd>
                        </div>
                        <div>
                          <dt>Grasa</dt>
                          <dd>{food.fatG}</dd>
                        </div>
                        <div>
                          <dt>Ración</dt>
                          <dd>{serving(food)}</dd>
                        </div>
                      </dl>
                      {actions(food)}
                    </td>
                  </tr>
                )}
              </Fragment>
            );
          })}
        </tbody>
      </table>
    );
  }

  return (
    <table className={styles.table}>
      <thead>
        <tr>
          <th scope="col">Alimento</th>
          <th scope="col">Categoría</th>
          <th scope="col" className={styles.numeric}>
            kcal
          </th>
          <th scope="col" className={styles.numeric}>
            Prot.
          </th>
          <th scope="col" className={styles.numeric}>
            HC
          </th>
          <th scope="col" className={styles.numeric}>
            Grasa
          </th>
          <th scope="col" className={styles.numeric}>
            Ración
          </th>
          <th scope="col" className={styles.actionsHeader}>
            Acciones
          </th>
        </tr>
      </thead>
      <tbody>
        {foods.map((food) => (
          <tr key={food.id}>
            <td>{food.name}</td>
            <td>{categoryLabel(food)}</td>
            <td className={styles.numeric}>{food.kcal}</td>
            <td className={styles.numeric}>{food.proteinG}</td>
            <td className={styles.numeric}>{food.carbsG}</td>
            <td className={styles.numeric}>{food.fatG}</td>
            <td className={styles.numeric}>{serving(food)}</td>
            <td>{actions(food)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
