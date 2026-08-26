import { ErrorState } from '../../components/ErrorState';
import { NutritionRings, RING_ARCS } from '../../components/NutritionRings';
import { WidgetLoading } from '../../components/WidgetLoading';
import type { TodayConsumptionState } from './todayNutrition';
import { WidgetSection } from './WidgetSection';
import styles from './NutritionSummaryWidget.module.css';

const NUM = new Intl.NumberFormat('es-ES', { maximumFractionDigits: 1 });

/** One view of today's consumption: calories and macros share the same read state. */
export function NutritionSummaryWidget({ state }: { readonly state: TodayConsumptionState }) {
  return (
    <WidgetSection id="nutrition-summary-widget-title" title="Nutrición">
      {renderContent(state)}
    </WidgetSection>
  );
}

function renderContent(state: TodayConsumptionState) {
  if (state.status === 'loading') {
    return <WidgetLoading label="Cargando tu nutrición de hoy…" rows={2} />;
  }
  if (state.status === 'error') {
    return <ErrorState message="No se pudo cargar tu nutrición de hoy." />;
  }

  const { consumed, target } = state.consumption;
  return (
    <div className={styles.content}>
      {/* The rings carry the accessible summary; the list beside them repeats every figure as
          text, so nothing here is said by colour alone. */}
      <NutritionRings consumed={consumed} target={target} />
      <ul className={styles.figures}>
        {RING_ARCS.map((arc) => {
          const eaten = consumed[arc.key];
          const goal = target?.[arc.key] ?? null;
          const unit = arc.key === 'kcal' ? 'kcal' : 'g';
          return (
            <li key={arc.key} className={styles.figure}>
              <span className={styles.label}>
                <span className={styles.dot} style={{ background: arc.color }} aria-hidden="true" />
                {arc.key === 'kcal' ? 'Calorías' : arc.label}
              </span>
              <span className={styles.value}>
                {NUM.format(eaten)}
                {goal !== null ? ` / ${NUM.format(goal)} ${unit}` : ` ${unit} · Sin objetivo`}
              </span>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
