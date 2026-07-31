import { useEffect, useState } from 'react';
import { Icon } from '../components/Icon';
import { WaterTracker } from '../components/WaterTracker';
import { BodyWidget, type BodyState } from './dashboard/BodyWidget';
import { CaloriesWidget } from './dashboard/CaloriesWidget';
import { TrainingWidget } from './dashboard/TrainingWidget';
import { NutritionWidget } from './dashboard/NutritionWidget';
import { MacrosWidget } from './dashboard/MacrosWidget';
import { TrendWidget } from './dashboard/TrendWidget';
import { EvolutionWidget } from './dashboard/EvolutionWidget';
import { ShoppingWidget } from './dashboard/ShoppingWidget';
import { TipWidget } from './dashboard/TipWidget';
import { PlanBanner } from './dashboard/PlanBanner';
import { WidgetSection } from './dashboard/WidgetSection';
import { getProfile } from '../api/profile';
import { listBodyMeasurements, type BodyMeasurement } from '../api/bodyMeasurements';
import styles from './DashboardPage.module.css';

/**
 * Dashboard page (FOR-19, rebuilt to the FOR-164 mockup
 * `docs/1-dashboard-1-medicion.png`). The daily entry point, composed from
 * self-fetching widgets so one failing widget never breaks the others (spec
 * `specs/FOR-51/spec.md`). No domain calculations happen here or in the widgets
 * (ADR-006) — every widget renders API values as returned.
 *
 * <p>Layout mirrors the mockup: a metrics row (body-composition tiles +
 * calories + hydration), a second row (next training / today's menu / macros /
 * 30-day trend), and a third row (first-summary / shopping preview / tip + plan
 * banner).
 *
 * <p>The header date navigator arrows are visual-only: no read model here takes
 * a date parameter (every widget exposes "today"/"this week" only), so the
 * arrows are inert decorative affordances and the label is today's date, per
 * `specs/FOR-51/ui.md`. Hydration and per-meal calories are placeholder
 * template data — see {@link WaterTracker} / {@link NutritionWidget}.
 */
const MEASURED_ON = new Intl.DateTimeFormat('es-ES', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
});

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}

export function DashboardPage() {
  // Greeting name comes from the profile (FOR-169): on an empty first-run there
  // is no saved profile, so the greeting stays generic ("Hola 👋") rather than
  // leaking a seeded persona. A failed/absent profile also falls back to generic.
  const [name, setName] = useState<string | undefined>(undefined);
  useEffect(() => {
    let active = true;
    getProfile()
      .then((profile) => {
        if (active) setName(profile.name?.trim() || undefined);
      })
      .catch(() => {
        // Greeting stays generic if the profile can't load.
      });
    return () => {
      active = false;
    };
  }, []);

  /*
   * The measurement history is fetched here rather than inside BodyWidget: the
   * header's navigator and the tiles are two views of one selection, and a
   * selection cannot be shared between components that each fetch their own
   * copy (FOR-189).
   */
  const [history, setHistory] = useState<BodyMeasurement[] | undefined>(undefined);
  const [failed, setFailed] = useState(false);
  const [selected, setSelected] = useState(0);
  useEffect(() => {
    let active = true;
    listBodyMeasurements()
      .then((measurements) => {
        if (active) setHistory(measurements);
      })
      .catch(() => {
        if (active) setFailed(true);
      });
    return () => {
      active = false;
    };
  }, []);

  const body: BodyState = failed
    ? { status: 'error' }
    : history === undefined
      ? { status: 'loading' }
      : history.length === 0
        ? { status: 'empty' }
        : { status: 'ready', history, selected: Math.min(selected, history.length - 1) };

  return (
    <div className={styles.wrapper}>
      <header className={styles.header}>
        <div className={styles.titles}>
          <h1 className={styles.title}>{name ? `Hola ${name} 👋` : 'Hola 👋'}</h1>
          <p className={styles.subtitle}>Este es tu resumen</p>
        </div>
        {/*
          Steps through the dates that actually have a measurement, not through
          the calendar: a day with nothing recorded has nothing to show. Absent
          entirely with no measurements — a navigator over an empty history is a
          control that cannot do anything.
        */}
        {body.status === 'ready' && (
          <div className={styles.dateNav}>
            <button
              type="button"
              className={styles.dateArrow}
              aria-label="Medición anterior"
              // `history` is newest-first, so "previous" walks the index up.
              disabled={body.selected >= body.history.length - 1}
              onClick={() => setSelected((index) => index + 1)}
            >
              <Icon name="chevron" size={16} className={styles.dateArrowPrev} />
            </button>
            <span className={styles.date}>
              {capitalize(MEASURED_ON.format(new Date(body.history[body.selected].measuredAt)))}
            </span>
            <button
              type="button"
              className={styles.dateArrow}
              aria-label="Medición siguiente"
              disabled={body.selected <= 0}
              onClick={() => setSelected((index) => index - 1)}
            >
              <Icon name="chevron" size={16} />
            </button>
          </div>
        )}
      </header>

      <WidgetSection id="metrics-row-title" title="Resumen de hoy" titleHidden surface={false}>
        <div className={styles.metrics}>
          <BodyWidget state={body} />
          <CaloriesWidget />
          <WaterTracker />
        </div>
      </WidgetSection>

      <div className={styles.rowFour}>
        <TrainingWidget />
        <NutritionWidget />
        <MacrosWidget />
        <TrendWidget />
      </div>

      <div className={styles.rowThree}>
        {/* Evolución takes two tracks: it inherited the width the retired
            "Tu progreso" card left behind, and a chart is what actually uses
            it (see DashboardPage.module.css `.rowThreeWide`). */}
        <EvolutionWidget />
        <ShoppingWidget />
        <div className={styles.tipColumn}>
          <TipWidget />
          <PlanBanner />
        </div>
      </div>
    </div>
  );
}
