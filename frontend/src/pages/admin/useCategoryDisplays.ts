import { useEffect, useMemo, useState } from 'react';
import { listCategories, type CategoryDisplay, type CategoryScope } from '../../api/categories';

/**
 * How the categories of one vocabulary are written and drawn, as edited in the
 * Categorías tab (FOR-197).
 *
 * <p>The labels and glyphs used to be constants in this bundle. They are data
 * now, so the screens that render them have to ask — otherwise renaming a
 * category would change the tab that edits it and nothing else.
 *
 * <p>Callers pass a fallback, and that is not laziness: this is a request, and a
 * table must not render blanks while it is in flight or if it fails. The worst
 * case is a stale name, not an empty column.
 *
 * <p>`options` is the same answer as a list, for the screens that offer a choice
 * rather than render one. It is empty until the request lands — a form has to
 * cope with that rather than assume the set is known.
 */
interface CategoryLookup {
  readonly label: (code: string | undefined, fallback: string) => string;
  readonly glyph: (code: string | undefined, fallback: string) => string;
  /** Every category of this vocabulary, in the order the backend serves them. */
  readonly options: readonly CategoryDisplay[];
}

export function useCategoryDisplays(scope: CategoryScope): CategoryLookup {
  const [displays, setDisplays] = useState<CategoryDisplay[]>([]);

  useEffect(() => {
    let active = true;
    listCategories(scope)
      .then((rows) => {
        if (active) setDisplays(rows);
      })
      .catch(() => undefined);
    return () => {
      active = false;
    };
  }, [scope]);

  return useMemo(() => {
    const byCode = new Map(displays.map((display) => [display.code, display]));
    return {
      label: (code, fallback) => (code ? (byCode.get(code)?.label ?? fallback) : fallback),
      glyph: (code, fallback) => (code ? (byCode.get(code)?.icon ?? fallback) : fallback),
      options: displays,
    };
  }, [displays]);
}
