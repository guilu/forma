import { BodyWidget } from './dashboard/BodyWidget';
import { TrainingWidget } from './dashboard/TrainingWidget';
import { NutritionWidget } from './dashboard/NutritionWidget';
import { ShoppingWidget } from './dashboard/ShoppingWidget';
import { InsightWidget } from './dashboard/InsightWidget';
import { SyncWidget } from './dashboard/SyncWidget';
import styles from './DashboardPage.module.css';

/**
 * Dashboard page (FOR-19, built out to the mockup by FOR-51). The daily entry point:
 * answers "what should I pay attention to today?" by composing widgets from existing
 * feature read models — body (FOR-17), training (FOR-26/27), nutrition (FOR-33),
 * shopping (FOR-39) and insights (FOR-45) — plus a static integration status chip. Each
 * widget owns its own loading/empty/error state so one failing widget never breaks the
 * others (spec `specs/FOR-51/spec.md` edge case). No domain calculations happen here or
 * in the widgets (ADR-006) — every widget renders API values as returned.
 *
 * <p>The mockup (`docs/1-dashboard.png`) also shows a prev/next date navigator and a
 * mobile Hoy/Semana/Mes tab switch. Neither is backed — every read model here only
 * exposes "the current week"/"today", with no date parameter — so the header shows
 * today's date as static text and the layout is a single ("Hoy") responsive view, per
 * `specs/FOR-51/ui.md`'s own recommendation ("Hoy for MVP unless weekly/month data is
 * available").
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
        <p className={styles.date}>{capitalize(TODAY)}</p>
      </header>

      <BodyWidget />

      <div className={styles.grid}>
        <TrainingWidget />
        <NutritionWidget />
        <ShoppingWidget />
        <InsightWidget />
        <SyncWidget />
      </div>
    </div>
  );
}
