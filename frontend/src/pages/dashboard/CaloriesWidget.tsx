import { useEffect, useState } from 'react';
import { ErrorState } from '../../components/ErrorState';
import { MetricCard } from '../../components/MetricCard';
import { WidgetLoading } from '../../components/WidgetLoading';
import { getNutritionDay, type NutritionDay } from '../../api/nutrition';

/**
 * "Calorías hoy" metrics tile (FOR-164 dashboard mockup). Shows today's calorie
 * *target* from the FOR-33 nutrition day (`GET /nutrition/days/{type}`).
 *
 * <p>The mockup renders a consumed-vs-target donut ("2.320 / 2.300 kcal ·
 * 78%"). Consumption is NOT backed — there is no calorie-logging endpoint (the
 * nutrition API returns only the day's targets and its planned meal template),
 * so this honestly shows the target alone rather than inventing an "eaten"
 * figure or a fake progress ring (ADR-006, same precedent as the other
 * documented dashboard gaps). Day type is hardcoded to `running`, matching
 * NutritionPage.
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly day: NutritionDay };

const KCAL = new Intl.NumberFormat('es-ES');

export function CaloriesWidget() {
  const [state, setState] = useState<State>({ status: 'loading' });

  useEffect(() => {
    let active = true;
    getNutritionDay('running')
      .then((day) => {
        if (active) setState({ status: 'ready', day });
      })
      .catch(() => {
        if (active) setState({ status: 'error' });
      });
    return () => {
      active = false;
    };
  }, []);

  if (state.status === 'loading') {
    return <WidgetLoading label="Cargando tus calorías de hoy…" rows={1} />;
  }
  if (state.status === 'error') {
    return <ErrorState message="No se pudieron cargar tus calorías de hoy." />;
  }

  return (
    <MetricCard
      label="Calorías hoy"
      value={KCAL.format(state.day.targets.calories)}
      unit="kcal"
      caption="objetivo diario"
    />
  );
}
