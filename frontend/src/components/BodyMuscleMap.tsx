import styles from './BodyMuscleMap.module.css';

/**
 * Anatomical front-view figure with the major muscle groups tinted (FOR-188),
 * for the body-composition card.
 *
 * <p>Replaces {@link BodyFigure}'s schematic silhouette *at this one call site*.
 * That component stays where it is: the training page renders a front/back pair,
 * and only a front asset exists — pairing a real figure with a drawn one would
 * look like a bug rather than a placeholder.
 *
 * <p>Static, not a heat map. The tint is baked into the asset, so it does not
 * (yet) reflect the FOR-136 muscle-map data — same honest limitation the
 * schematic had, now better looking. A per-muscle version needs either an SVG
 * with addressable regions or one asset per state.
 */
interface BodyMuscleMapProps {
  /** Rendered height in px; the asset's aspect ratio does the rest. */
  readonly size?: number;
  /**
   * Accessible name. Omit for a decorative figure — the composition legend
   * beside it already carries every number, so `alt=""` is often right.
   */
  readonly label?: string;
}

export function BodyMuscleMap({ size = 168, label }: BodyMuscleMapProps) {
  return (
    <img
      className={styles.figure}
      src="/body/muscle-map-front.png"
      alt={label ?? ''}
      height={size}
      // Intrinsic ratio of the asset (226x512), so the box is reserved before
      // the image loads and the card does not reflow around it.
      width={Math.round((size * 226) / 512)}
      loading="lazy"
      decoding="async"
    />
  );
}
