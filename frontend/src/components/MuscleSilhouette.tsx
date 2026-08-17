import {
  MUSCLE_CODES_BY_VIEW,
  type BodyView,
  type MuscleCode,
  type MuscleRole,
} from '../pages/trainingMuscleOverlay';
import styles from './MuscleSilhouette.module.css';

export type AnatomySex = 'male' | 'female';
export type SilhouetteVariant = 'strength' | 'running' | 'rest';

/**
 * Anatomical body illustration with the worked muscles lit up.
 *
 * <p>The pack ships one bitmap silhouette per sex and sheet, plus one SVG per
 * muscle. The muscle SVGs are used as **masks over a solid colour block**, not
 * as images: an `<img>` would paint whatever fill the file was authored with,
 * while a mask lets the fill come from `--color-accent`, so the highlight
 * follows the theme and the two emphasis levels are just opacity. That is the
 * technique the reference page in `docs/forma_muscle_overlays` uses, and the
 * reason the pack ships the muscles as separate files at all.
 *
 * <p>Assets resolve through `import.meta.glob` rather than a runtime-built
 * `/assets/...` URL, so the bundler sees every file: they get hashed for
 * cache-busting and a missing one is a build error instead of a silent 404 in
 * front of a user.
 *
 * <p>Running and rest have their own whole-body art with no muscles to show, so
 * a `muscles` map is ignored for them rather than being the caller's problem.
 */
interface MuscleSilhouetteProps {
  readonly sex: AnatomySex;
  /** Which sheet to draw. Ignored for the running and rest variants. */
  readonly view?: BodyView;
  readonly variant?: SilhouetteVariant;
  /** Muscle code -> emphasis, as built by `overlayFromMuscleMap`. */
  readonly muscles?: Readonly<Partial<Record<MuscleCode, MuscleRole>>>;
  /** Naming it makes it an image; leaving it out keeps it decorative. */
  readonly label?: string;
  readonly className?: string;
}

/*
 * Eager rather than lazy: these are a handful of small files that the card
 * needs on first paint, and a lazy glob would return promises the render path
 * would have to unwrap for no benefit.
 */
const SILHOUETTES = import.meta.glob<string>('../assets/anatomy/*/*/silhouette.webp', {
  eager: true,
  query: '?url',
  import: 'default',
});

const MASKS = import.meta.glob<string>('../assets/anatomy/*/*/*.svg', {
  eager: true,
  query: '?url',
  import: 'default',
});

function silhouetteUrl(sex: AnatomySex, sheet: string): string | undefined {
  return SILHOUETTES[`../assets/anatomy/${sex}/${sheet}/silhouette.webp`];
}

/**
 * Pixel size of each silhouette, so the box can carry that art's own aspect
 * ratio. They are not interchangeable — the anatomical sheets are tall and
 * narrow (854x1840) while the running and rest bodies are wider — so a single
 * hardcoded ratio stretched the runner past the bottom of its card.
 *
 * <p>Measured from the files themselves; the pack's `muscle-map.json` only
 * documents the two anatomical sheets. Mask alignment depends on the front and
 * back numbers being exact, since the masks are stretched to this same box.
 */
const SILHOUETTE_SIZE: Readonly<Record<string, readonly [number, number]>> = {
  'male/front': [854, 1840],
  'male/back': [854, 1842],
  'male/run': [996, 1579],
  'male/rest': [1109, 1419],
  'female/front': [854, 1842],
  'female/back': [854, 1842],
  'female/run': [1024, 1536],
  'female/rest': [1305, 1206],
};

function maskUrl(sex: AnatomySex, view: BodyView, code: MuscleCode): string | undefined {
  return MASKS[`../assets/anatomy/${sex}/${view}/${code}.svg`];
}

export function MuscleSilhouette({
  sex,
  view = 'front',
  variant = 'strength',
  muscles,
  label,
  className,
}: MuscleSilhouetteProps) {
  /*
   * Running and rest have their own whole-body art; the muscle sheets are
   * strength-only. The `running` variant keeps the name the training API and
   * `BodyFigure` already use, while the pack's folder is `run` — mapped here
   * rather than renaming either side to match the other.
   */
  const sheet = variant === 'strength' ? view : variant === 'running' ? 'run' : 'rest';
  const source = silhouetteUrl(sex, sheet);

  /*
   * Only the codes this sheet can actually draw. The overlay covers both sides
   * of the body — a pull session works the lats and the biceps — so each view
   * renders its own half rather than asking for masks that do not exist.
   */
  const drawn =
    variant === 'strength' && muscles
      ? MUSCLE_CODES_BY_VIEW[view]
          .map((code) => ({ code, role: muscles[code] }))
          .filter(
            (entry): entry is { code: MuscleCode; role: MuscleRole } => entry.role !== undefined,
          )
      : [];

  const decorative = label === undefined;
  const [width, height] = SILHOUETTE_SIZE[`${sex}/${sheet}`] ?? [854, 1840];

  return (
    <div
      className={[styles.bodyMap, className].filter(Boolean).join(' ')}
      style={{ '--silhouette-ratio': `${width} / ${height}` } as React.CSSProperties}
    >
      <img
        className={styles.silhouette}
        src={source}
        alt={label ?? ''}
        aria-hidden={decorative ? true : undefined}
        data-silhouette={`${sex}/${sheet}`}
        draggable={false}
      />
      {drawn.map(({ code, role }) => (
        <span
          key={code}
          className={styles.muscle}
          data-muscle={code}
          data-role={role}
          aria-hidden="true"
          /*
           * Inline because the URL is per-muscle and hashed by the bundler:
           * there is no stylesheet that could name twenty of them per sex and
           * view without restating the whole pack.
           */
          style={
            {
              '--muscle-mask': `url("${maskUrl(sex, view, code)}")`,
            } as React.CSSProperties
          }
        />
      ))}
    </div>
  );
}
