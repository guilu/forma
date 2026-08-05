import { useCallback, useEffect, useState } from 'react';
import { Card, type HeadingLevel } from './Card';
import { getHydration, logWaterIntake, type HydrationProgress } from '../api/nutrition';
import styles from './WaterTracker.module.css';

/**
 * Hydration tile (FOR-164 dashboard mockup: "AGUA 2.1 / 2.5 L · 84%").
 *
 * <p>This used to render four invented numbers under a comment that read "there is
 * no hydration endpoint anywhere in the backend (verified: no controller, no read
 * model, no persisted intake)". There is, and there was: FOR-130 built `GET` and
 * `POST /api/v1/nutrition/hydration`, and nothing ever called them. The placeholder
 * outlived the reason for it — the same way the nutrition page's macro chips did,
 * and the same way this comment will if nobody deletes it once it stops being true.
 *
 * <p><b>The row of glasses is a progress meter, not a count of drinks.</b> Five
 * segments of a fifth of the goal each, filled by what has been logged. Nothing in
 * the model counts glasses, and drawing four full ones would be claiming somebody
 * drank four times when they may have drunk once.
 *
 * <p>The two buttons log a volume and say which. A glass is not a unit the API
 * knows — it counts millilitres — so the label carries the number rather than
 * hiding it behind a word that means something different in every kitchen.
 */
const SEGMENTS = 5;

/** Common amounts, offered as shortcuts. The label says the millilitres it logs. */
const SHORTCUTS = [
  { label: '+ Vaso', ml: 250 },
  { label: '+ Botella', ml: 500 },
] as const;

const NUM = new Intl.NumberFormat('es-ES', { minimumFractionDigits: 1, maximumFractionDigits: 1 });

interface WaterTrackerProps {
  readonly headingLevel?: HeadingLevel;
  /** The day being shown, ISO. Defaults to today. */
  readonly date?: string;
}

export function WaterTracker({ headingLevel, date }: WaterTrackerProps = {}) {
  const day = date ?? new Date().toISOString().slice(0, 10);
  const [progress, setProgress] = useState<HydrationProgress | undefined>(undefined);
  const [failed, setFailed] = useState(false);
  const [pending, setPending] = useState(false);

  const reload = useCallback(() => {
    getHydration(day)
      .then((fresh) => {
        setProgress(fresh);
        setFailed(false);
      })
      .catch(() => setFailed(true));
  }, [day]);

  useEffect(reload, [reload]);

  async function add(volumeMl: number) {
    if (pending) return;
    setPending(true);
    try {
      await logWaterIntake(day, volumeMl);
      reload();
    } catch {
      setFailed(true);
    } finally {
      setPending(false);
    }
  }

  if (failed || progress === undefined) {
    return (
      <Card title="Agua" headingLevel={headingLevel}>
        <p className={styles.empty}>{failed ? 'No se pudo cargar el agua de hoy.' : 'Cargando…'}</p>
      </Card>
    );
  }

  const currentL = progress.totalMl / 1000;
  const goalL = progress.goalMl === null ? null : progress.goalMl / 1000;
  // Uncapped on the wire — 1.2 means past the goal — but a meter cannot draw past full.
  const filled =
    progress.progress === null ? 0 : Math.min(SEGMENTS, Math.round(progress.progress * SEGMENTS));
  const percent = progress.progress === null ? null : Math.round(progress.progress * 100);

  return (
    <Card title="Agua" headingLevel={headingLevel}>
      <p className={styles.value}>
        {NUM.format(currentL)}
        {/* No goal is a real state the API documents: it renders as no denominator,
            not as a denominator of zero. */}
        {goalL === null ? (
          <span className={styles.goal}> L</span>
        ) : (
          <span className={styles.goal}> / {NUM.format(goalL)} L</span>
        )}
      </p>
      {percent !== null && <p className={styles.percent}>{percent}%</p>}
      <div
        className={styles.glasses}
        role="img"
        aria-label={
          goalL === null
            ? `Hidratación: ${NUM.format(currentL)} litros, sin objetivo fijado`
            : `Hidratación: ${NUM.format(currentL)} de ${NUM.format(goalL)} litros (${percent}%)`
        }
      >
        {Array.from({ length: SEGMENTS }, (_, i) => (
          <span
            key={i}
            className={i < filled ? styles.glassFilled : styles.glass}
            aria-hidden="true"
          />
        ))}
      </div>
      <div className={styles.actions}>
        {SHORTCUTS.map((shortcut) => (
          <button
            key={shortcut.ml}
            type="button"
            className={styles.add}
            disabled={pending}
            onClick={() => add(shortcut.ml)}
          >
            {shortcut.label} ({shortcut.ml} ml)
          </button>
        ))}
      </div>
    </Card>
  );
}
