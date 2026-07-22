import { useEffect, useState } from 'react';
import { ErrorState } from '../../components/ErrorState';
import { MacroRing } from '../../components/MacroRing';
import { WidgetLoading } from '../../components/WidgetLoading';
import { getNutritionDay, type NutritionDay } from '../../api/nutrition';
import { WidgetSection } from './WidgetSection';
import styles from './MacrosWidget.module.css';

/**
 * "Macronutrientes" widget (FOR-164 dashboard mockup). Reuses the FOR-54
 * {@link MacroRing} to render today's protein/carb/fat *targets* (grams) from
 * the FOR-33 nutrition day, plus an "Objetivo diario" summary line.
 *
 * <p>The mockup shows a "current / target" per macro (e.g. "162 / 160 g").
 * Current intake is not backed (no consumption endpoint), so only the target
 * grams are shown — the honest subset, same as {@link CaloriesWidget}. Day type
 * is hardcoded to `running`, matching NutritionPage.
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'empty' }
  | { readonly status: 'ready'; readonly day: NutritionDay };

export function MacrosWidget() {
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
    <WidgetSection id="macros-widget-title" title="Macronutrientes">
      {renderContent(state)}
    </WidgetSection>
  );
}

function renderContent(state: State) {
  if (state.status === 'loading') {
    return <WidgetLoading label="Cargando tus macronutrientes…" rows={2} />;
  }
  if (state.status === 'error') {
    return <ErrorState message="No se pudieron cargar tus macronutrientes." />;
  }
  if (state.status === 'empty') {
    return null;
  }

  const { targets } = state.day;

  return (
    <div className={styles.card}>
      <MacroRing proteinG={targets.proteinG} carbsG={targets.carbsG} fatG={targets.fatG} />
      <p className={styles.objective}>
        Objetivo diario: {targets.proteinG} g proteínas · {targets.carbsG} g carbohidratos ·{' '}
        {targets.fatG} g grasas
      </p>
    </div>
  );
}
