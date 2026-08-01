import { Icon } from '../../components/Icon';
import styles from './Pagination.module.css';

/**
 * Previous/next paging for the catalog table (FOR-190).
 *
 * <p>Client-side: `GET /api/v1/foods` answers with the whole catalog because the
 * plan builder needs it whole anyway, so paging here is about what fits on a
 * screen, not about what crosses the wire. It stays that way until the catalog
 * is big enough that shipping it all is the problem — at which point the paging
 * moves to the query, not just to this component.
 *
 * <p>Renders nothing for a single page: a control whose every button is disabled
 * is noise.
 */
interface PaginationProps {
  /** Zero-based. */
  readonly page: number;
  readonly pageCount: number;
  readonly onChange: (page: number) => void;
}

export function Pagination({ page, pageCount, onChange }: PaginationProps) {
  if (pageCount <= 1) {
    return null;
  }
  return (
    <nav className={styles.pagination} aria-label="Paginación del catálogo">
      <button
        type="button"
        className={styles.step}
        aria-label="Página anterior"
        disabled={page === 0}
        onClick={() => onChange(page - 1)}
      >
        <Icon name="chevron" size={18} className={styles.back} />
      </button>
      {/* Polite, not assertive: the page number changing is a confirmation, not
          an interruption — the rows underneath already changed. */}
      <span className={styles.status} aria-live="polite">
        {`Página ${page + 1} de ${pageCount}`}
      </span>
      <button
        type="button"
        className={styles.step}
        aria-label="Página siguiente"
        disabled={page >= pageCount - 1}
        onClick={() => onChange(page + 1)}
      >
        <Icon name="chevron" size={18} />
      </button>
    </nav>
  );
}
