import { Fragment, type ReactNode } from 'react';
import { Button } from '../../components/Button';
import { Icon } from '../../components/Icon';
import { IconButton } from '../../components/IconButton';
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
  /**
   * What this column sorts by. Absent means the column does not sort — which is
   * the right answer for free text like "Caja 0.8 kg", whose alphabetical order
   * carries no meaning.
   */
  readonly sortBy?: (row: T) => string | number | undefined;
}

/** Which column is sorted and which way. */
export interface CatalogSort {
  readonly header: string;
  readonly direction: 'asc' | 'desc';
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
  /**
   * How the name is drawn, when it is more than text — a link out to a shop, for
   * instance. `nameOf` is still what labels the row's actions, so an action keeps
   * a plain name whatever this renders.
   */
  readonly renderName?: (row: T) => ReactNode;
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
  /** Sorts the name column; the caller decides what a name compares as. */
  readonly nameSortBy?: (row: T) => string | number | undefined;
  readonly sort?: CatalogSort;
  readonly onSort?: (header: string) => void;
  readonly narrow: boolean;
  /** Id of the row unfolded on the phone layout; ignored when wide. */
  readonly expandedId?: string;
  readonly onToggle: (id: string) => void;
  readonly onEdit: (row: T) => void;
  /**
   * Absent for a catalog whose rows cannot be removed — the categories, whose
   * set is fixed by the database's own constraints. No button beats a button
   * that always fails.
   */
  readonly onDelete?: (row: T) => void;
}

/**
 * A header that sorts when it can (FOR-199), on a phone as much as on a laptop: a catalog of
 * hundreds is read by ordering it, and that is not a desktop-only need.
 *
 * <p>A button inside the cell rather than a click handler on the cell itself, so it is reachable by
 * keyboard and announced as what it is. `aria-sort` goes on the header, which is where assistive
 * tech looks for it.
 */
function SortableHeader({
  header,
  sortable,
  sort,
  onSort,
  className,
}: {
  readonly header: string;
  readonly sortable: boolean;
  readonly sort?: CatalogSort;
  readonly onSort?: (header: string) => void;
  readonly className?: string;
}) {
  const active = sort?.header === header;
  return (
    <th
      scope="col"
      className={className}
      aria-sort={active ? (sort.direction === 'asc' ? 'ascending' : 'descending') : undefined}
    >
      {sortable && onSort ? (
        <button type="button" className={styles.sortButton} onClick={() => onSort(header)}>
          {header}
          <span className={styles.sortMark} aria-hidden="true">
            {active ? (sort.direction === 'asc' ? '▲' : '▼') : '↕'}
          </span>
        </button>
      ) : (
        header
      )}
    </th>
  );
}

export function CatalogTable<T>({
  rows,
  idOf,
  nameOf,
  renderName,
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
  nameSortBy,
  sort,
  onSort,
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
            <SortableHeader
              header={nameHeader}
              sortable={nameSortBy !== undefined}
              sort={sort}
              onSort={onSort}
            />
            {compactColumns.map((column) => (
              <SortableHeader
                key={column.header}
                header={column.header}
                sortable={column.sortBy !== undefined}
                sort={sort}
                onSort={onSort}
                className={styles.numeric}
              />
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
                      {mediaOf?.(row)}
                      {glyphOf && (
                        <span className={styles.glyph} aria-hidden="true">
                          {glyphOf(row)}
                        </span>
                      )}
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
                          <Button
                            variant="soft"
                            className={styles.detailAction}
                            aria-label={`Editar ${nameOf(row)}`}
                            onClick={() => onEdit(row)}
                          >
                            <Icon name="edit" size={16} />
                            Editar
                          </Button>
                          {onDelete && (
                            <Button
                              variant="soft"
                              tone="danger"
                              className={styles.detailAction}
                              aria-label={`Eliminar ${nameOf(row)}`}
                              onClick={() => onDelete(row)}
                            >
                              <Icon name="trash" size={16} />
                              Eliminar
                            </Button>
                          )}
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
          <SortableHeader
            header={nameHeader}
            sortable={nameSortBy !== undefined}
            sort={sort}
            onSort={onSort}
          />
          {columns.map((column) => (
            <SortableHeader
              key={column.header}
              header={column.header}
              sortable={column.sortBy !== undefined}
              sort={sort}
              onSort={onSort}
              className={column.numeric ? styles.numeric : undefined}
            />
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
                {/* The category as a glyph here too (FOR-196): a wide row is
                    scanned by shape before it is read, same as a narrow one. */}
                {glyphOf && (
                  <span className={styles.glyph} aria-hidden="true">
                    {glyphOf(row)}
                  </span>
                )}
                {renderName?.(row) ?? nameOf(row)}
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
                <IconButton
                  variant="soft"
                  label={`Editar ${nameOf(row)}`}
                  onClick={() => onEdit(row)}
                >
                  <Icon name="edit" size={18} />
                </IconButton>
                {onDelete && (
                  <IconButton
                    variant="soft"
                    tone="danger"
                    label={`Eliminar ${nameOf(row)}`}
                    onClick={() => onDelete(row)}
                  >
                    <Icon name="trash" size={18} />
                  </IconButton>
                )}
              </div>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
