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
 * <p>The old constants stay as the fallback, and that is not laziness: this is a
 * request, and a table must not render blanks while it is in flight or if it
 * fails. Falling back to what shipped means the worst case is a stale name, not
 * an empty column.
 */
interface CategoryLookup {
  readonly label: (code: string | undefined, fallback: string) => string;
  readonly glyph: (code: string | undefined, fallback: string) => string;
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
    };
  }, [displays]);
}
