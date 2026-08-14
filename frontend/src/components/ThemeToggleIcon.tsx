import { useId } from 'react';
import styles from './ThemeToggleIcon.module.css';

/**
 * Sun/moon glyph for the topbar's theme toggle, drawn as one persistent shape
 * so the two states morph into each other instead of cutting.
 *
 * <p>Why this exists next to {@link Icon} rather than inside it: `Icon` renders
 * a single `<path d>` per name, which is what makes it cheap and uniform — a
 * name change swaps the `d` outright, and there is nothing for CSS to animate
 * between. A morph needs the two endpoints to be the *same* elements in two
 * states, so this component keeps the disc, the crescent's inner arc and the
 * ray bundle mounted at all times and moves them with CSS.
 *
 * <p>The geometry is not invented — it is decomposed out of `Icon`'s own `sun`
 * and `moon` paths, so both ends of the morph draw the glyph the rest of the
 * app already uses. `moon` (`M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z`) is
 * an r=9 arc centred on (12, 12) closed by an r=7 arc centred on (16.8, 7.2);
 * `sun`'s disc is the same centre at r=4, ringed by the eight rays. So the
 * morph is: grow the disc from r=4 to r=9, slide the r=7 circle in from
 * off-frame to take its bite out of one side and supply the crescent's inner
 * edge, and retract the rays.
 *
 * <p>Both circles are clipped, not merely drawn: the outer one hides where the
 * bite covers it, the inner one shows only where it falls inside the outer.
 * Masking the outer circle alone would leave a plain arc — a circle with a gap
 * — rather than a crescent's two joined edges.
 *
 * <p>Icons are decorative here exactly as in `Icon`: the toggle button's
 * `aria-label` is the accessible name and already announces the state.
 */
export function ThemeToggleIcon({
  icon,
  size = 20,
  className,
}: {
  /** Which glyph to show — the *destination* theme, matching the button label. */
  readonly icon: 'sun' | 'moon';
  readonly size?: number;
  readonly className?: string;
}) {
  // Two instances on one page would otherwise share their mask ids and each
  // would clip against the other's geometry — the collision Brand.tsx
  // documents for the logo's gradient ids.
  const id = useId();
  const biteMask = `theme-toggle-bite-${id}`;
  const insideMask = `theme-toggle-inside-${id}`;

  return (
    <svg
      className={[styles.root, className].filter(Boolean).join(' ')}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      // Same escape hatch Icon exposes, so tests can pin the glyph without
      // reading computed styles.
      data-icon={icon}
    >
      {/*
       * `maskUnits="userSpaceOnUse"` on both: the default bounding-box region
       * is 120% of the *unstroked* bbox, which clips the circles' own 1.75
       * stroke. Spelling out a region larger than the viewBox keeps every
       * stroke inside the mask.
       */}
      <mask id={biteMask} maskUnits="userSpaceOnUse" x="-2" y="-2" width="28" height="28">
        <rect x="-2" y="-2" width="28" height="28" fill="#fff" />
        <circle className={styles.bite} cx="16.8" cy="7.2" r="7" fill="#000" />
      </mask>
      <mask id={insideMask} maskUnits="userSpaceOnUse" x="-2" y="-2" width="28" height="28">
        {/* Tracks .disc's transform exactly — see the CSS. */}
        <circle className={styles.discMask} cx="12" cy="12" r="9" fill="#fff" />
      </mask>
      {/*
       * The masks hang on wrapper groups, not on the circles. A mask resolves
       * in the local coordinate system of the element that carries it, so
       * masking a circle directly would drag its mask along with the circle's
       * own transform — the bite would shrink with the sun instead of sliding
       * away. Untransformed wrappers pin both masks to user space.
       */}
      <g mask={`url(#${biteMask})`}>
        <circle className={styles.disc} cx="12" cy="12" r="9" />
      </g>
      <g mask={`url(#${insideMask})`}>
        <circle className={styles.innerArc} cx="16.8" cy="7.2" r="7" />
      </g>
      <g className={styles.rays}>
        <path d="M12 4V2M12 22v-2M4 12H2M22 12h-2M5.6 5.6L4.2 4.2M19.8 19.8l-1.4-1.4M18.4 5.6l1.4-1.4M4.2 19.8l1.4-1.4" />
      </g>
    </svg>
  );
}
