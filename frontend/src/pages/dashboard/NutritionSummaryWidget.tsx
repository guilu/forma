import { CalorieRing } from '../../components/CalorieRing';
import { ErrorState } from '../../components/ErrorState';
import { WidgetLoading } from '../../components/WidgetLoading';
import type { TodayConsumptionState } from './todayNutrition';
import { ProgressBar } from './ProgressBar';
import { WidgetSection } from './WidgetSection';
import styles from './NutritionSummaryWidget.module.css';

const MACROS = [
  { key: 'proteinG', label: 'Proteínas', color: 'var(--color-accent)' },
  { key: 'carbsG', label: 'Carbohidratos', color: 'var(--color-warning-graphic)' },
  { key: 'fatG', label: 'Grasas', color: 'var(--color-text-muted)' },
] as const;

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
      <CalorieRing consumed={consumed.kcal} target={target?.kcal ?? null} compact />
      <ul className={styles.macros}>
        {MACROS.map((macro) => {
          const eaten = consumed[macro.key];
          const goal = target?.[macro.key] ?? null;
          return (
            <li key={macro.key} className={styles.macro}>
              <span className={styles.macroLabel}>{macro.label}</span>
              <span className={styles.macroValue}>
                {NUM.format(eaten)}
                {goal !== null ? ` / ${NUM.format(goal)} g` : ' g · Sin objetivo'}
              </span>
              {goal !== null && (
                <ProgressBar
                  value={eaten}
                  max={goal}
                  color={macro.color}
                  label={`${macro.label}: ${eaten} de ${goal} gramos`}
                  showPercent={false}
                />
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
