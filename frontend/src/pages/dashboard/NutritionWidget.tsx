import { useEffect, useState } from 'react';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { Icon } from '../../components/Icon';
import { WidgetLoading } from '../../components/WidgetLoading';
import { getNutritionDay, type NutritionDay } from '../../api/nutrition';
import { WidgetSection } from './WidgetSection';
import { ProgressBar } from './ProgressBar';
import styles from './NutritionWidget.module.css';

/**
 * "Menú de hoy" widget (FOR-51, rebuilt for the FOR-164 dashboard mockup):
 * today's planned meals from the FOR-33 nutrition day (`GET
 * /nutrition/days/{type}`) — real meal names + preferred times — and the day's
 * calorie target. Renders API values as returned (ADR-006).
 *
 * <p><b>Placeholder data (hybrid).</b> The mockup shows a kcal figure per meal
 * and a "consumed / target" progress bar. Neither is backed: `NutritionMeal`
 * carries no per-meal calories and there is no consumption-logging endpoint. To
 * match the template these are rendered from clearly-labelled placeholder
 * constants ({@link PLACEHOLDER}), NOT real data — they must be replaced once a
 * nutrition-consumption API exists. Meal names/times and the calorie target are
 * real. Day type is hardcoded to `running`, matching NutritionPage.
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'empty' }
  | { readonly status: 'ready'; readonly day: NutritionDay };

/**
 * Visual-only scaffolding for the FOR-164 mockup: per-meal kcal badges (cycled
 * by meal index) and a "consumed so far" figure for the progress bar. No
 * backend exposes these yet — see the file doc comment.
 */
const PLACEHOLDER = {
  mealKcal: [560, 230, 590, 480, 320],
  consumedKcal: 2320,
} as const;

const KCAL = new Intl.NumberFormat('es-ES');

export function NutritionWidget() {
  const [state, setState] = useState<State>({ status: 'loading' });

  useEffect(() => {
    let active = true;
    getNutritionDay('running')
      .then((day) => {
        if (!active) return;
        setState(day.meals.length === 0 ? { status: 'empty' } : { status: 'ready', day });
      })
      .catch(() => {
        if (active) setState({ status: 'error' });
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <WidgetSection
      id="nutrition-widget-title"
      title="Menú de hoy"
      linkTo="/nutricion"
      linkLabel="Ver plan"
    >
      {renderContent(state)}
    </WidgetSection>
  );
}

function renderContent(state: State) {
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
  const targetKcal = day.targets.calories;

  return (
    <div className={styles.card}>
      <ul className={styles.meals}>
        {day.meals.map((meal, index) => (
          <li key={meal.mealType} className={styles.meal}>
            <span className={styles.mealIcon} aria-hidden="true">
              <Icon name="nutrition" size={18} />
            </span>
            <span className={styles.mealText}>
              <span className={styles.mealName}>{meal.name}</span>
              <span className={styles.mealTime}>{meal.preferredTime}</span>
            </span>
            {/* Placeholder kcal (see file doc comment). */}
            <span className={styles.mealKcal}>
              {KCAL.format(PLACEHOLDER.mealKcal[index % PLACEHOLDER.mealKcal.length])} kcal
            </span>
          </li>
        ))}
      </ul>
      <div className={styles.total}>
        <span className={styles.totalLabel}>
          {/* Consumed figure is placeholder; the target is real. */}
          {KCAL.format(PLACEHOLDER.consumedKcal)} / {KCAL.format(targetKcal)} kcal
        </span>
        <ProgressBar
          value={PLACEHOLDER.consumedKcal}
          max={targetKcal}
          label="Calorías del plan de hoy"
        />
      </div>
    </div>
  );
}
