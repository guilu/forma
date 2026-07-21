import styles from './BodyFigure.module.css';

/**
 * Placeholder body silhouette (FOR-164 training mockup
 * `docs/3-entrenamiento-dash.png`). The mockup renders anatomical front/back
 * figures with per-muscle highlighting; a dedicated asset pack for those is
 * being produced separately and will replace this component's internals later.
 *
 * <p>Until then this draws a simple schematic silhouette so the layout reads
 * correctly, with an optional `active` accent tint to stand in for
 * "worked/highlighted". It is intentionally minimal and clearly a placeholder
 * (see the `data-placeholder` marker) — NOT real anatomy and NOT driven by the
 * FOR-136 muscle-map data yet. Decorative by default (`aria-hidden`): callers
 * provide the real label/heatmap text alongside.
 */
interface BodyFigureProps {
  readonly view?: 'front' | 'back';
  readonly variant?: 'strength' | 'running' | 'rest';
  readonly active?: boolean;
  readonly size?: number;
  readonly label?: string;
}

export function BodyFigure({
  view = 'front',
  variant = 'strength',
  active = false,
  size = 96,
  label,
}: BodyFigureProps) {
  const decorative = label === undefined;
  return (
    <svg
      className={[styles.figure, active ? styles.active : ''].filter(Boolean).join(' ')}
      width={(size * 3) / 4}
      height={size}
      viewBox="0 0 48 64"
      data-placeholder="body-figure"
      data-view={view}
      data-variant={variant}
      role={decorative ? undefined : 'img'}
      aria-hidden={decorative ? true : undefined}
      aria-label={label}
    >
      {variant === 'rest' ? (
        // Seated / meditation pose stand-in for a rest day.
        <>
          <circle cx="24" cy="14" r="6" />
          <path d="M14 44c0-9 4-16 10-16s10 7 10 16z" />
          <path d="M12 46h24l-2 6H14z" />
        </>
      ) : (
        <>
          {/* head */}
          <circle cx="24" cy="9" r="6" />
          {/* torso */}
          <path d="M16 17h16l-2 22h-12z" />
          {/* arms */}
          <path d="M16 18l-6 3-2 16 4 1 5-15z" />
          <path d="M32 18l6 3 2 16-4 1-5-15z" />
          {/* legs */}
          <path d="M18 39h5l-1 22h-5z" />
          <path d="M25 39h5l1 22h-5z" />
        </>
      )}
    </svg>
  );
}
