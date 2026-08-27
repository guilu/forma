import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { Icon } from '../../components/Icon';
import { WidgetLoading } from '../../components/WidgetLoading';
import type { DayConsumption, NutritionMeal } from '../../api/nutrition';
import { usePlannedMealToggle } from '../usePlannedMealToggle';
import type { TodayMenuState } from './todayNutrition';
import { WidgetSection } from './WidgetSection';
import { ProgressBar } from './ProgressBar';
import { measure } from '../../format/measures';
import styles from './NutritionWidget.module.css';

/**
 * "Menú de hoy" widget (FOR-51, rebuilt for the FOR-164 dashboard mockup):
 * today's planned meals from the FOR-33 nutrition day (`GET
 * /nutrition/days/{type}`), paired with the same date's consumption read model.
 * Meal kcal and food descriptions come from the plan; daily progress comes from
 * persisted consumption. The server-provided day type selects the plan day.
 */

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

/**
 * The per-meal "eaten" control, drawn as the row's leading glyph.
 *
 * <p>A real checkbox under the paint (`appearance: none`), not a button with a
 * tick in it: this is a two-state thing that can be switched back, which is
 * what a checkbox already means to a keyboard and to a screen reader. The label
 * carries the whole sentence — the meal name is not in the row's markup as far
 * as this control is concerned.
 */
function MealCheck({
  meal,
  eaten,
  marking,
  onToggle,
}: {
  readonly meal: NutritionMeal;
  readonly eaten: boolean;
  readonly marking: boolean;
  readonly onToggle: (meal: NutritionMeal, eaten: boolean) => void;
}) {
  return (
    <label className={styles.check}>
      <input
        type="checkbox"
        className={styles.checkInput}
        checked={eaten}
        disabled={marking}
        onChange={() => onToggle(meal, eaten)}
      />
      <span className={styles.checkMark} aria-hidden="true">
        <Icon name="check" size={14} />
      </span>
      <span className={styles.checkLabel}>
        {marking
          ? `Actualizando ${meal.name}`
          : eaten
            ? `Desmarcar ${meal.name} como hecha`
            : `Marcar ${meal.name} como hecha`}
      </span>
    </label>
  );
}

export function NutritionWidget({
  menu,
  consumption,
  dateIso,
  onMealToggled,
}: {
  readonly menu: TodayMenuState;
  readonly consumption?: DayConsumption;
  /** The day these meals belong to, so a mark lands on the right date. */
  readonly dateIso: string;
  /** Refreshes the dashboard's consumption once a meal has been marked. */
  readonly onMealToggled: () => Promise<unknown>;
}) {
  const { marking, toggle } = usePlannedMealToggle(dateIso, onMealToggled);
  return (
    <WidgetSection
      id="nutrition-widget-title"
      title="Menu"
      linkTo="/app/nutrition"
      linkLabel="Ver plan"
    >
      {renderContent(menu, consumption, { marking, toggle })}
    </WidgetSection>
  );
}

interface MealActions {
  readonly marking: ReadonlySet<string>;
  readonly toggle: (meal: NutritionMeal, eaten: boolean) => void;
}

function renderContent(
  state: TodayMenuState,
  consumption: DayConsumption | undefined,
  actions: MealActions,
) {
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
  const states = new Map(consumption?.plannedMeals?.map((meal) => [meal.id, meal.state]) ?? []);

  return (
    <div className={styles.card}>
      <ul className={styles.meals}>
        {day.meals.map((meal) => (
          <li key={meal.id} className={styles.meal}>
            {/*
             * The row's glyph is the control, not decoration. A five-row card
             * has room for one affordance per meal, and "I ate this" is the
             * only thing anyone wants to do from here — the same write the
             * nutrition page makes from its own check.
             */}
            <MealCheck
              meal={meal}
              eaten={states.get(meal.id) === 'EATEN'}
              marking={actions.marking.has(meal.id)}
              onToggle={actions.toggle}
            />
            <span className={styles.mealText}>
              <span className={styles.mealName}>{MEAL_LABELS[meal.mealType] ?? meal.mealType}</span>
              {descriptionOf(meal) && (
                <span className={styles.mealDescription}>{descriptionOf(meal)}</span>
              )}
            </span>
            <span className={styles.mealKcal}>{measure(meal.totals.calories)} kcal</span>
          </li>
        ))}
      </ul>
      <div className={styles.total}>
        <span className={styles.totalLabel}>
          {measure(consumedKcal)} kcal{' '}
          {targetKcal !== null ? `/ ${measure(targetKcal)} kcal` : '· Sin objetivo'}
        </span>
        {targetKcal !== null && (
          /* Violeta, que es el color de las calorías en los aros de nutrición:
             esta barra mide justo eso. */
          <ProgressBar
            value={consumedKcal}
            max={targetKcal}
            label="Calorías del plan de hoy"
            color="var(--color-violet)"
          />
        )}
      </div>
    </div>
  );
}
