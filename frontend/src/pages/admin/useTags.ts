import { useEffect, useMemo, useState } from 'react';
import { listTags, type Tag } from '../../api/tags';

/**
 * The labels a food can carry, as the backend has them (V50).
 *
 * <p>Asked for rather than compiled in, for the same reason as the food groups
 * and the chains before them: the vocabulary is a table, and a bundle carrying
 * its own copy would go on offering twelve after somebody adds a thirteenth.
 */
interface TagLookup {
  /** Every label, in the order a list of checkboxes should show them. Empty until it answers. */
  readonly options: readonly Tag[];
  /** How a label reads, falling back to its own id. */
  readonly label: (id: string) => string;
}

export function useTags(): TagLookup {
  const [tags, setTags] = useState<Tag[]>([]);

  useEffect(() => {
    let active = true;
    listTags()
      .then((rows) => {
        if (active) setTags(rows);
      })
      .catch(() => undefined);
    return () => {
      active = false;
    };
  }, []);

  return useMemo(() => {
    const byId = new Map(tags.map((tag) => [tag.id, tag]));
    return {
      options: tags,
      label: (id) => byId.get(id)?.name ?? id,
    };
  }, [tags]);
}
