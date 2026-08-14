import styles from './BodyFigure.module.css';
import maleFront from '../assets/anatomy/hombre-front.png';
import maleBack from '../assets/anatomy/hombre-back.png';
import femaleFront from '../assets/anatomy/mujer-front.png';
import femaleBack from '../assets/anatomy/mujer-back.png';

/**
 * Body illustration used by training cards. Strength sessions use the supplied
 * male/female front/back anatomical assets, ready for a future muscle-highlight
 * SVG overlay. Running and rest retain the compact schematic SVG because they
 * do not represent a muscle-side view. Decorative by default (`aria-hidden`).
 */
interface BodyFigureProps {
  readonly view?: 'front' | 'back';
  readonly sex?: 'male' | 'female';
  readonly variant?: 'strength' | 'running' | 'rest';
  readonly active?: boolean;
  readonly size?: number;
  readonly label?: string;
}

export function BodyFigure({
  view = 'front',
  sex = 'male',
  variant = 'strength',
  active = false,
  size = 96,
  label,
}: BodyFigureProps) {
  const decorative = label === undefined;
  if (variant === 'strength') {
    const source =
      sex === 'female'
        ? view === 'back'
          ? femaleBack
          : femaleFront
        : view === 'back'
          ? maleBack
          : maleFront;

    return (
      <img
        className={styles.anatomy}
        src={source}
        width={(size * 360) / 776}
        height={size}
        alt={label ?? ''}
        aria-hidden={decorative ? true : undefined}
        data-testid="anatomy-figure"
        data-view={view}
        data-sex={sex}
      />
    );
  }

  return (
    <svg
      className={[styles.figure, active ? styles.active : ''].filter(Boolean).join(' ')}
      width={(size * 3) / 4}
      height={size}
      viewBox="0 0 48 64"
      data-placeholder="body-figure"
      data-view={view}
      data-variant={variant}
      // Exposed as an attribute, not just the class, so a consumer can restyle
      // the highlighted figure for its own context without reaching for a CSS
      // module class it cannot name.
      data-active={active}
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
