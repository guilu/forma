import { useEffect, useState } from 'react';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { WidgetLoading } from '../../components/WidgetLoading';
import { getNutritionDay, type NutritionDay } from '../../api/nutrition';
import { WidgetSection } from './WidgetSection';
import styles from './NutritionWidget.module.css';

/**
 * Today nutrition summary widget (FOR-51): today's calorie and macro targets from the
 * FOR-33 seeded nutrition day (`GET /nutrition/days/{type}`). Renders the API values as
 * returned (ADR-006).
 *
 * <p>The mockup (`docs/1-dashboard.png`) shows "calories eaten so far vs. target" (e.g.
 * "2.110 / 2.300 kcal") and a hydration tracker. Neither is backed: the nutrition API
 * only returns the day's *target* macros and its planned meal template — there is no
 * consumption-logging endpoint (FOR-32 computes meal/day totals from the template
 * itself, not from what the user actually ate) and no hydration data anywhere in the
 * backend. This widget therefore shows the day's targets only, honestly framed as a
 * target rather than "vs. eaten"; hydration is omitted. Documented gap, see FOR-51 PR
 * "Known limitations".
 *
 * <p>Like `NutritionPage` (FOR-34), the day type is hardcoded to `running` — there is no
 * "which type is today" resolution yet (inherited simplification, not introduced here).
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'empty' }
  | { readonly status: 'ready'; readonly day: NutritionDay };

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
        if (active) {
          setState({ status: 'error' });
        }
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <WidgetSection id="nutrition-widget-title" title="Nutrición de hoy" linkTo="/nutricion">
      {renderContent(state)}
    </WidgetSection>
  );
}

function renderContent(state: State) {
  if (state.status === 'loading') {
    return <WidgetLoading label="Cargando tu nutrición de hoy…" rows={2} />;
  }

  if (state.status === 'error') {
    return (
      <ErrorState message="No se pudo cargar tu nutrición de hoy. Inténtalo de nuevo más tarde." />
    );
  }

  if (state.status === 'empty') {
    return <EmptyState variant="filtered" title="No hay un plan de comidas para hoy todavía." />;
  }

  const { targets } = state.day;

  return (
    <div className={styles.card}>
      <p className={styles.calories}>
        <span className={styles.caloriesValue}>{targets.calories}</span>
        <span className={styles.caloriesUnit}>kcal objetivo hoy</span>
      </p>
      <ul className={styles.macros}>
        <li className={styles.macro}>
          <span>Proteínas</span>
          <span>{targets.proteinG} g</span>
        </li>
        <li className={styles.macro}>
          <span>Carbohidratos</span>
          <span>{targets.carbsG} g</span>
        </li>
        <li className={styles.macro}>
          <span>Grasas</span>
          <span>{targets.fatG} g</span>
        </li>
      </ul>
    </div>
  );
}
