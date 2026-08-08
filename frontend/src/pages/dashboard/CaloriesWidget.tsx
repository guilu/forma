import { Card } from '../../components/Card';
import { CalorieRing } from '../../components/CalorieRing';
import { ErrorState } from '../../components/ErrorState';
import { WidgetLoading } from '../../components/WidgetLoading';
import type { TodayConsumptionState } from './todayNutrition';

/**
 * "Calorías hoy" metrics tile (FOR-164 dashboard mockup). Shows today's calorie
 * consumed and target calories from the shared date-based consumption read.
 * Uses the same {@link CalorieRing} as NutritionPage, only at compact dimensions.
 *
 * <p>With no plan for today the tile says so instead of rendering figures: a
 * day with no meals carries no calorie target either, and the tile used to show
 * the placeholder consumed against it — "2120 kcal / Objetivo: 0 kcal / 0%",
 * three numbers that mean nothing. Same wording as {@link NutritionWidget}, so
 * the two cards for the same missing plan read as one message.
 */
export function CaloriesWidget({ state }: { readonly state: TodayConsumptionState }) {
  if (state.status === 'loading') {
    return <WidgetLoading label="Cargando tus calorías de hoy…" rows={1} />;
  }
  if (state.status === 'error') {
    return <ErrorState message="No se pudieron cargar tus calorías de hoy." />;
  }
  return (
    <Card title="Calorías hoy">
      <CalorieRing
        consumed={state.consumption.consumed.kcal}
        target={state.consumption.target?.kcal ?? null}
        compact
      />
    </Card>
  );
}
