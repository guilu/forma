import { useEffect, useMemo, useState } from 'react';
import { listStores, type Store } from '../../api/stores';

/**
 * The chains a product can be filed under, as the backend has them (V45).
 *
 * <p>They used to be three constants in this bundle. They are rows now, so the
 * screens that offer them have to ask — otherwise adding Lidl would change the
 * database and nothing else.
 *
 * <p>`label` falls back to the stored id, which is not laziness: this is a
 * request, and a table must not render blanks while it is in flight or if it
 * fails. A code reads worse than a name and better than a hole.
 */
interface StoreLookup {
  /** Every chain, in the order the backend serves them. Empty until it answers. */
  readonly options: readonly Store[];
  /** How a chain reads, falling back to its own id. */
  readonly label: (id: string | undefined) => string;
}

export function useStores(): StoreLookup {
  const [stores, setStores] = useState<Store[]>([]);

  useEffect(() => {
    let active = true;
    listStores()
      .then((rows) => {
        if (active) setStores(rows);
      })
      .catch(() => undefined);
    return () => {
      active = false;
    };
  }, []);

  return useMemo(() => {
    const byId = new Map(stores.map((store) => [store.id, store]));
    return {
      options: stores,
      label: (id) => (id ? (byId.get(id)?.name ?? id) : '—'),
    };
  }, [stores]);
}
