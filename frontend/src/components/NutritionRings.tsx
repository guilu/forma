import type { DayConsumption } from '../api/nutrition';
import styles from './NutritionRings.module.css';

/**
 * Today's calories and macros as four concentric rings (Apple Fitness's shape, FORMA's palette).
 *
 * <p>Four rings and not three: calories are the day's headline number, and the card that this
 * replaces gave them a donut of their own. The outermost ring carries them; the three inside are
 * the macros, in the same colours their dots and bars use everywhere else in the app — the ring is
 * never the only thing saying which macro it is.
 *
 * <p>SVG rather than the `conic-gradient` the other rings in this codebase use ({@link CalorieRing}
 * did, {@link MacroRing} and {@link ProgressRing} still do). A conic gradient cannot round the end
 * of its sweep and needs one nested box per ring to punch its own hole; four stroked circles share
 * one `viewBox` and get round caps for free. Still no charting dependency (ADR-013 covers charts,
 * and a progress ring is not one).
 *
 * <p><b>Each arc stops at one full lap.</b> The numbers beside the rings are uncapped, so going
 * over is visible where it can be read exactly; a second lap drawn over the first would make a
 * ring that has been overshot look emptier than one that has just been closed.
 */
interface NutritionRingsProps {
  readonly consumed: DayConsumption['consumed'];
  /** The day's targets, or `null` when the plan sets none — then there is nothing to draw against. */
  readonly target: DayConsumption['target'];
  /** Outer diameter, any CSS length. Defaults to the dashboard's tile size. */
  readonly size?: string;
}

/**
 * Outermost first. `r` and `width` are in the 100x100 user space of the `viewBox`, so the whole
 * block scales with whatever `size` the caller asks for. The rings are thin and evenly spaced by
 * ten units: a fourth ring means the innermost one is short, and a fat stroke there turns its arc
 * into a nub that cannot be read as a proportion.
 *
 * <p>The colours are the ones `NutritionPage` already pins to each macro (its legend dots, its
 * bars and every meal's macro label), so a ring and the figure next to it never disagree. Calories
 * take a hue no macro uses.
 */
export const RING_ARCS = [
  { key: 'kcal', label: 'kcal', color: 'var(--color-violet)', r: 46, width: 7 },
  { key: 'proteinG', label: 'Proteínas', color: 'var(--color-info)', r: 36, width: 7 },
  { key: 'carbsG', label: 'Carbohidratos', color: 'var(--color-accent)', r: 26, width: 7 },
  { key: 'fatG', label: 'Grasas', color: 'var(--color-warning-graphic)', r: 16, width: 7 },
] as const;

const NUM = new Intl.NumberFormat('es-ES', { maximumFractionDigits: 1 });

export function NutritionRings({ consumed, target, size = '8rem' }: NutritionRingsProps) {
  return (
    <svg
      className={styles.rings}
      style={{ width: size, height: size }}
      viewBox="0 0 100 100"
      role="img"
      aria-label={summarize(consumed, target)}
    >
      {/* Twelve o'clock is where every arc starts, which is a quarter turn back from where SVG
          puts zero degrees. */}
      <g transform="rotate(-90 50 50)">
        {RING_ARCS.map((arc) => {
          const circumference = 2 * Math.PI * arc.r;
          const goal = target?.[arc.key] ?? null;
          const ratio = goal !== null && goal > 0 ? Math.min(consumed[arc.key] / goal, 1) : 0;
          return (
            <g key={arc.key} style={{ '--ring-color': arc.color } as React.CSSProperties}>
              <circle
                className={styles.track}
                data-track={arc.key}
                cx="50"
                cy="50"
                r={arc.r}
                strokeWidth={arc.width}
              />
              <circle
                className={styles.arc}
                data-arc={arc.key}
                cx="50"
                cy="50"
                r={arc.r}
                strokeWidth={arc.width}
                strokeDasharray={circumference}
                strokeDashoffset={circumference * (1 - ratio)}
                /* A round cap on an empty arc still paints a dot, which would claim a value
                   nobody logged. */
                strokeLinecap={ratio > 0 ? 'round' : 'butt'}
              />
            </g>
          );
        })}
      </g>
    </svg>
  );
}

/** The whole card in one sentence, for anyone who is not looking at the rings. */
function summarize(consumed: DayConsumption['consumed'], target: DayConsumption['target']): string {
  const macros = RING_ARCS.slice(1);
  if (target === null) {
    const figures = macros
      .map((arc, index) => {
        const label = index === 0 ? arc.label : arc.label.toLowerCase();
        return `${label} ${NUM.format(consumed[arc.key])} g`;
      })
      .join(', ');
    return `${NUM.format(consumed.kcal)} kcal. ${figures}. Tu plan no fija objetivos.`;
  }
  const figures = macros
    .map((arc, index) => {
      const label = index === 0 ? arc.label : arc.label.toLowerCase();
      return `${label} ${NUM.format(consumed[arc.key])} de ${NUM.format(target[arc.key])} g`;
    })
    .join(', ');
  return `${NUM.format(consumed.kcal)} de ${NUM.format(target.kcal)} kcal. ${figures}.`;
}
