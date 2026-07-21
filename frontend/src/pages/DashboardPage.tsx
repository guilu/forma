import { Icon } from '../components/Icon';
import { WaterTracker } from '../components/WaterTracker';
import { BodyWidget } from './dashboard/BodyWidget';
import { CaloriesWidget } from './dashboard/CaloriesWidget';
import { TrainingWidget } from './dashboard/TrainingWidget';
import { NutritionWidget } from './dashboard/NutritionWidget';
import { MacrosWidget } from './dashboard/MacrosWidget';
import { TrendWidget } from './dashboard/TrendWidget';
import { FirstSummaryWidget } from './dashboard/FirstSummaryWidget';
import { ShoppingWidget } from './dashboard/ShoppingWidget';
import { TipWidget } from './dashboard/TipWidget';
import { PlanBanner } from './dashboard/PlanBanner';
import { WidgetSection } from './dashboard/WidgetSection';
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
const TODAY = new Intl.DateTimeFormat('es-ES', {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
  year: 'numeric',
}).format(new Date());

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}

export function DashboardPage() {
  return (
    <div className={styles.wrapper}>
      <header className={styles.header}>
        <div className={styles.titles}>
          <h1 className={styles.title}>Hola Diego 👋</h1>
          <p className={styles.subtitle}>Este es tu resumen de hoy</p>
        </div>
        {/* Date navigator — visual only (no date-parameterised read model). */}
        <div className={styles.dateNav} aria-hidden="true">
          <span className={styles.dateArrow}>
            <Icon name="chevron" size={16} className={styles.dateArrowPrev} />
          </span>
          <span className={styles.date}>{capitalize(TODAY)}</span>
          <span className={styles.dateArrow}>
            <Icon name="chevron" size={16} />
          </span>
        </div>
      </header>

      <WidgetSection id="metrics-row-title" title="Resumen de hoy" titleHidden surface={false}>
        <div className={styles.metrics}>
          <BodyWidget />
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
        <FirstSummaryWidget />
        <ShoppingWidget />
        <div className={styles.tipColumn}>
          <TipWidget />
          <PlanBanner />
        </div>
      </div>
    </div>
  );
}
