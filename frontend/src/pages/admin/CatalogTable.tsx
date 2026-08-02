import { Fragment, type ReactNode } from 'react';
import { Icon } from '../../components/Icon';
import styles from './CatalogTable.module.css';

/**
 * The admin catalogs' table (FOR-190 foods, FOR-191 store products), in the two
 * shapes it needs.
 *
 * <p>Seven columns do not fit a phone. The first attempt let the table scroll
 * sideways inside its card, which reads badly — half the row is off screen, the
 * actions are the part that falls off, and a sideways flick started outside the
 * card scrolled the whole page instead.
 *
 * <p>So the narrow layout keeps the two or three things a catalog is scanned by
 * and moves the rest behind a row disclosure: tap the name, the remaining
 * figures and the two actions unfold underneath. Progressive disclosure rather
 * than a second data source: same rows, same page, same order, only the columns
 * differ. One row is open at a time, because two detail panels on a 390 px
 * screen leave nothing of the table visible.
 *
 * <p>Generic over the row type because both catalogs need exactly this and a
 * copy would drift: the caller supplies the columns and the detail entries, this
 * owns the disclosure, the touch targets and the two layouts.
 *
 * <p>The alternative — a card per row — was rejected: it loses the column
 * alignment that makes a catalog comparable at a glance, which is the whole
 * reason these screens are tables and not lists.
 */
export interface CatalogColumn<T> {
  readonly header: string;
  readonly value: (row: T) => ReactNode;
  /** Right-aligned with tabular figures. */
  readonly numeric?: boolean;
}

export interface CatalogDetail<T> {
  /** Decorative; the label beside it carries the meaning. */
  readonly glyph: string;
  readonly label: string;
  readonly value: (row: T) => ReactNode;
}

interface CatalogTableProps<T> {
  readonly rows: readonly T[];
  readonly idOf: (row: T) => string;
  readonly nameOf: (row: T) => string;
  /** Decorative glyph before the name on the phone layout. */
  readonly glyphOf?: (row: T) => string;
  /**
   * Rendered before the name in both layouts, e.g. a product photo. Takes
   * precedence over `glyphOf`: a real picture beats a stand-in for its category.
   */
  readonly mediaOf?: (row: T) => ReactNode;
  /** Actions shown before edit and delete, e.g. refreshing an imported row. */
  readonly extraActions?: (row: T) => ReactNode;
  /** Accessible name of the table — no visible caption is rendered. */
  readonly label: string;
  /** Header of the first column, the one the row is named by. */
  readonly nameHeader: string;
  /** Columns after the name, wide layout. */
  readonly columns: readonly CatalogColumn<T>[];
  /** Columns after the name, phone layout. Everything else moves to `details`. */
  readonly compactColumns: readonly CatalogColumn<T>[];
  readonly details: readonly CatalogDetail<T>[];
  /** Footnote inside the open panel, e.g. the basis of the figures. */
  readonly detailBadge?: string;
  /** Extra content under the figures of an open row; nothing is rendered when it returns undefined. */
  readonly detailFooter?: (row: T) => ReactNode;
  readonly narrow: boolean;
  /** Id of the row unfolded on the phone layout; ignored when wide. */
  readonly expandedId?: string;
  readonly onToggle: (id: string) => void;
  readonly onEdit: (row: T) => void;
  readonly onDelete: (row: T) => void;
}

export function CatalogTable<T>({
  rows,
  idOf,
  nameOf,
  glyphOf,
  mediaOf,
  extraActions,
  label,
  nameHeader,
  columns,
  compactColumns,
  details,
  detailBadge,
  detailFooter,
  narrow,
  expandedId,
  onToggle,
  onEdit,
  onDelete,
}: CatalogTableProps<T>) {
  if (narrow) {
    return (
      <table className={`${styles.table} ${styles.compact}`} aria-label={label}>
        <thead>
          <tr>
            <th scope="col">{nameHeader}</th>
            {compactColumns.map((column) => (
              <th key={column.header} scope="col" className={styles.numeric}>
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => {
            const id = idOf(row);
            const open = id === expandedId;
            return (
              <Fragment key={id}>
                <tr className={open ? styles.openRow : undefined}>
                  <td>
                    <button
                      type="button"
                      className={styles.rowToggle}
                      aria-expanded={open}
                      aria-controls={`catalog-detail-${id}`}
                      onClick={() => onToggle(id)}
                    >
                      <Icon
                        name="chevron"
                        size={16}
                        className={open ? styles.chevronOpen : styles.chevron}
                      />
                      {mediaOf?.(row) ??
                        (glyphOf && (
                          <span className={styles.glyph} aria-hidden="true">
                            {glyphOf(row)}
                          </span>
                        ))}
                      <span className={styles.rowName}>{nameOf(row)}</span>
                    </button>
                  </td>
                  {compactColumns.map((column) => (
                    <td key={column.header} className={styles.numeric}>
                      {column.value(row)}
                    </td>
                  ))}
                </tr>
                {open && (
                  <tr id={`catalog-detail-${id}`} className={styles.openRow}>
                    <td colSpan={compactColumns.length + 1} className={styles.detailCell}>
                      <div className={styles.detailPanel}>
                        <dl className={styles.detail}>
                          {details.map((item) => (
                            <div key={item.label} className={styles.detailItem}>
                              <span className={styles.detailGlyph} aria-hidden="true">
                                {item.glyph}
                              </span>
                              <div>
                                <dt>{item.label}</dt>
                                <dd>{item.value(row)}</dd>
                              </div>
                            </div>
                          ))}
                        </dl>
                        {(detailBadge || detailFooter) && (
                          <p className={styles.basis}>
                            {detailBadge && <span className={styles.basisPill}>{detailBadge}</span>}
                            {detailFooter?.(row)}
                          </p>
                        )}
                        <div className={styles.detailActions}>
                          {extraActions?.(row)}
                          <button
                            type="button"
                            className={styles.detailEdit}
                            aria-label={`Editar ${nameOf(row)}`}
                            onClick={() => onEdit(row)}
                          >
                            <Icon name="edit" size={16} />
                            Editar
                          </button>
                          <button
                            type="button"
                            className={styles.detailDelete}
                            aria-label={`Eliminar ${nameOf(row)}`}
                            onClick={() => onDelete(row)}
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
    <table className={styles.table} aria-label={label}>
      <thead>
        <tr>
          <th scope="col">{nameHeader}</th>
          {columns.map((column) => (
            <th
              key={column.header}
              scope="col"
              className={column.numeric ? styles.numeric : undefined}
            >
              {column.header}
            </th>
          ))}
          <th scope="col" className={styles.actionsHeader}>
            Acciones
          </th>
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={idOf(row)}>
            <td>
              <span className={styles.nameCell}>
                {mediaOf?.(row)}
                {nameOf(row)}
              </span>
            </td>
            {columns.map((column) => (
              <td key={column.header} className={column.numeric ? styles.numeric : undefined}>
                {column.value(row)}
              </td>
            ))}
            <td>
              <div className={styles.rowActions}>
                {extraActions?.(row)}
                <button
                  type="button"
                  className={styles.rowAction}
                  aria-label={`Editar ${nameOf(row)}`}
                  onClick={() => onEdit(row)}
                >
                  <Icon name="edit" size={18} />
                </button>
                <button
                  type="button"
                  className={styles.rowAction}
                  aria-label={`Eliminar ${nameOf(row)}`}
                  onClick={() => onDelete(row)}
                >
                  <Icon name="trash" size={18} />
                </button>
              </div>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
