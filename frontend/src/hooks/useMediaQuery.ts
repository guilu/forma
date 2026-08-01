import { useEffect, useState } from 'react';

/**
 * Whether a CSS media query currently matches, kept in sync as the viewport
 * changes.
 *
 * <p>For the rare case where a breakpoint has to change *content* rather than
 * presentation — a button whose label is a different phrase on a phone, not the
 * same phrase restyled. Anything that CSS can express (hiding, reflowing,
 * resizing) belongs in a stylesheet instead: this runs on the client, so it
 * renders once with the query unmatched before settling.
 *
 * <p>jsdom does not implement `matchMedia`; `src/test/setup.ts` stubs it to
 * answer "no match", so tests see the wide branch unless they say otherwise.
 */
export function useMediaQuery(query: string): boolean {
  const [matches, setMatches] = useState(() => window.matchMedia(query).matches);

  useEffect(() => {
    const list = window.matchMedia(query);
    const update = () => setMatches(list.matches);
    // Re-read on mount too: the query may have changed between the initial
    // state and the effect, and a remount under a different viewport would
    // otherwise keep the first answer.
    update();
    list.addEventListener('change', update);
    return () => list.removeEventListener('change', update);
  }, [query]);

  return matches;
}
