import { Fragment } from 'react';
import { Icon } from '../../components/Icon';
import type { CatalogFood } from '../../api/foods';
import { CATEGORY_LABELS, categoryGlyph } from './foodDisplay';
import styles from './FoodTable.module.css';

/**
 * The catalog table (FOR-190), in the two shapes it needs.
 *
 * <p>Seven columns do not fit a phone. The first attempt let the table scroll
 * sideways inside its card, which reads badly — half the row is off screen, the
 * actions are the part that falls off, and a sideways flick started outside the
 * card scrolled the whole page instead.
 *
 * <p>So the narrow layout keeps the three things a catalog is scanned by — what
 * to eat, what it costs, how much of it — and moves the rest behind a row
 * disclosure: tap the name, the macros and the two actions unfold underneath.
 * Progressive disclosure rather than a second data source: same rows, same page,
 * same order, only the columns differ. One row is open at a time, because two
 * detail panels on a 390 px screen leave nothing of the table visible.
 *
 * <p>Three columns fit 390 px only at the smaller type size the narrow layout
 * uses; the point of dropping four columns was to stop scrolling sideways, so
 * the type shrinks rather than the row overflowing again.
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

/**
 * The figures behind a row, in the order the detail panel reads them. The
 * ration is not among them: it is a column of its own at this width, and a
 * value shown twice is a value that can look like two different things.
 * Glyphs are decorative; every entry is labelled in words beside it.
 */
const details = (food: CatalogFood) => [
  { glyph: '🏷️', label: 'Categoría', value: categoryLabel(food) },
  { glyph: '🍞', label: 'HC (hidratos)', value: `${food.carbsG} g` },
  { glyph: '🥩', label: 'Proteínas', value: `${food.proteinG} g` },
  { glyph: '💧', label: 'Grasa', value: `${food.fatG} g` },
];

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
      <table className={`${styles.table} ${styles.compact}`} aria-label="Alimentos">
        <thead>
          <tr>
            <th scope="col">Alimento</th>
            <th scope="col" className={styles.numeric}>
              kcal
            </th>
            <th scope="col" className={styles.numeric}>
              Ración
            </th>
          </tr>
        </thead>
        <tbody>
          {foods.map((food) => {
            const open = food.id === expandedId;
            return (
              <Fragment key={food.id}>
                <tr className={open ? styles.openRow : undefined}>
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
                      <span className={styles.glyph} aria-hidden="true">
                        {categoryGlyph(food.category)}
                      </span>
                      <span className={styles.rowName}>{food.name}</span>
                    </button>
                  </td>
                  <td className={styles.numeric}>{food.kcal}</td>
                  <td className={styles.numeric}>{serving(food)}</td>
                </tr>
                {open && (
                  <tr id={`food-detail-${food.id}`} className={styles.openRow}>
                    <td colSpan={3} className={styles.detailCell}>
                      <div className={styles.detailPanel}>
                        <dl className={styles.detail}>
                          {details(food).map((item) => (
                            <div key={item.label} className={styles.detailItem}>
                              <span className={styles.detailGlyph} aria-hidden="true">
                                {item.glyph}
                              </span>
                              <div>
                                <dt>{item.label}</dt>
                                <dd>{item.value}</dd>
                              </div>
                            </div>
                          ))}
                        </dl>
                        {/* Grams of what: the catalog stores every macro per
                            100 g, and the column header that used to say so is
                            not on screen at this width. */}
                        <p className={styles.basis}>Por 100 g</p>
                        <div className={styles.detailActions}>
                          <button
                            type="button"
                            className={styles.detailEdit}
                            aria-label={`Editar ${food.name}`}
                            onClick={() => onEdit(food)}
                          >
                            <Icon name="edit" size={16} />
                            Editar
                          </button>
                          <button
                            type="button"
                            className={styles.detailDelete}
                            aria-label={`Eliminar ${food.name}`}
                            onClick={() => onDelete(food)}
                          >
                            <Icon name="trash" size={16} />
                            Eliminar
                          </button>
                        </div>
                      </div>
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
    <table className={styles.table} aria-label="Alimentos">
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
