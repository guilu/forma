import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { Icon } from '../../components/Icon';
import { WidgetLoading } from '../../components/WidgetLoading';
import type { DayConsumption, NutritionMeal } from '../../api/nutrition';
import type { TodayMenuState } from './todayNutrition';
import { WidgetSection } from './WidgetSection';
import { ProgressBar } from './ProgressBar';
import styles from './NutritionWidget.module.css';

/**
 * "Menú de hoy" widget (FOR-51, rebuilt for the FOR-164 dashboard mockup):
 * today's planned meals from the FOR-33 nutrition day (`GET
 * /nutrition/days/{type}`), paired with the same date's consumption read model.
 * Meal kcal and food descriptions come from the plan; daily progress comes from
 * persisted consumption. The server-provided day type selects the plan day.
 */
const KCAL = new Intl.NumberFormat('es-ES');

const MEAL_LABELS: Record<string, string> = {
  BREAKFAST: 'Desayuno',
  MID_MORNING: 'Media mañana',
  LUNCH: 'Comida',
  SNACK: 'Merienda',
  PRE_WORKOUT: 'Pre-entreno',
  POST_WORKOUT: 'Post-entreno',
  DINNER: 'Cena',
};

function descriptionOf(meal: NutritionMeal): string | undefined {
  return meal.items.length > 0 ? meal.items.map((item) => item.food).join(', ') : undefined;
}

export function NutritionWidget({
  menu,
  consumption,
}: {
  readonly menu: TodayMenuState;
  readonly consumption?: DayConsumption;
}) {
  return (
    <WidgetSection
      id="nutrition-widget-title"
      title="Menu"
      linkTo="/app/nutrition"
      linkLabel="Ver plan"
    >
      {renderContent(menu, consumption)}
    </WidgetSection>
  );
}

function renderContent(state: TodayMenuState, consumption: DayConsumption | undefined) {
  if (state.status === 'loading') {
    return <WidgetLoading label="Cargando tu menú de hoy…" rows={2} />;
  }
  if (state.status === 'error') {
    return <ErrorState message="No se pudo cargar tu menú de hoy. Inténtalo de nuevo más tarde." />;
  }
  if (state.status === 'empty') {
    return <EmptyState variant="filtered" title="No hay un plan de comidas para hoy todavía." />;
  }

  const { day } = state;
  const targetKcal = consumption?.target?.kcal ?? null;
  const consumedKcal = consumption?.consumed.kcal ?? 0;

  return (
    <div className={styles.card}>
      <ul className={styles.meals}>
        {day.meals.map((meal) => (
          <li key={meal.id} className={styles.meal}>
            <span className={styles.mealIcon} aria-hidden="true">
              <Icon name="nutrition" size={18} />
            </span>
            <span className={styles.mealText}>
              <span className={styles.mealName}>{MEAL_LABELS[meal.mealType] ?? meal.mealType}</span>
              {descriptionOf(meal) && (
                <span className={styles.mealDescription}>{descriptionOf(meal)}</span>
              )}
            </span>
            <span className={styles.mealKcal}>{KCAL.format(meal.totals.calories)} kcal</span>
          </li>
        ))}
      </ul>
      <div className={styles.total}>
        <span className={styles.totalLabel}>
          {KCAL.format(consumedKcal)} kcal{' '}
          {targetKcal !== null ? `/ ${KCAL.format(targetKcal)} kcal` : '· Sin objetivo'}
        </span>
        {targetKcal !== null && (
          <ProgressBar value={consumedKcal} max={targetKcal} label="Calorías del plan de hoy" />
        )}
      </div>
    </div>
  );
}
