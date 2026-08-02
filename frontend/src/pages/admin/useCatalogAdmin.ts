import { useCallback, useEffect, useState } from 'react';
import { ApiRequestError } from '../../api/client';
import { useMediaQuery } from '../../hooks/useMediaQuery';
import { useMountedRef } from '../../hooks/useMountedRef';

/**
 * The plumbing every admin catalog panel needs (FOR-191): load, page, open a
 * row, delete with a confirmation, report what the server refused.
 *
 * <p>Extracted when the shopping catalog arrived and the foods panel's body
 * would otherwise have been copied wholesale. Only the *mechanics* live here —
 * what a row looks like, which columns it has and what its form asks for stay
 * with each panel, because that is the part that genuinely differs.
 */
export type CatalogState<T> =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly rows: T[] };

/** Rows per page. Ten fills a phone screen without the card growing past it. */
export const PAGE_SIZE = 10;

/** Below this the tables drop to two columns — see `CatalogTable`. */
export const NARROW = '(max-width: 767px)';

interface CatalogAdminOptions<T> {
  /** Reads the catalog. Changing its identity reloads, so memoise it. */
  readonly list: () => Promise<T[]>;
  readonly remove: (id: string) => Promise<void>;
  /** Shown when the delete is refused and the server said nothing useful. */
  readonly deleteErrorMessage: string;
}

export function useCatalogAdmin<T>({ list, remove, deleteErrorMessage }: CatalogAdminOptions<T>) {
  const mountedRef = useMountedRef();
  const [state, setState] = useState<CatalogState<T>>({ status: 'loading' });
  const [page, setPage] = useState(0);
  const [expandedId, setExpandedId] = useState<string | undefined>(undefined);
  const [pending, setPending] = useState(false);
  const [actionError, setActionError] = useState<string | undefined>(undefined);
  const narrow = useMediaQuery(NARROW);

  const load = useCallback(() => {
    setState({ status: 'loading' });
    list()
      .then((rows) => {
        if (mountedRef.current) setState({ status: 'ready', rows });
      })
      .catch(() => {
        if (mountedRef.current) setState({ status: 'error' });
      });
  }, [list, mountedRef]);

  useEffect(() => {
    load();
  }, [load]);

  const rows = state.status === 'ready' ? state.rows : [];
  const pageCount = Math.max(1, Math.ceil(rows.length / PAGE_SIZE));
  // Clamped on read rather than corrected in an effect: deleting the last row of
  // the last page shrinks the catalog under the current page, and a render that
  // shows an empty table before an effect fixes it is a flicker users notice.
  const currentPage = Math.min(page, pageCount - 1);
  const visible = rows.slice(currentPage * PAGE_SIZE, currentPage * PAGE_SIZE + PAGE_SIZE);

  const confirmDelete = useCallback(
    async (id: string, onDeleted: () => void) => {
      setPending(true);
      setActionError(undefined);
      try {
        await remove(id);
        onDeleted();
        load();
      } catch (caught) {
        setActionError(caught instanceof ApiRequestError ? caught.message : deleteErrorMessage);
      } finally {
        setPending(false);
      }
    },
    [remove, load, deleteErrorMessage],
  );

  const goToPage = useCallback((next: number) => {
    setPage(next);
    // The open row belongs to the page that just left.
    setExpandedId(undefined);
  }, []);

  const toggle = useCallback(
    (id: string) => setExpandedId((open) => (open === id ? undefined : id)),
    [],
  );

  return {
    state,
    visible,
    page: currentPage,
    pageCount,
    goToPage,
    narrow,
    expandedId,
    toggle,
    pending,
    actionError,
    setActionError,
    confirmDelete,
    reload: load,
  };
}
