import { useState } from 'react';
import { useNotify } from '../components/NotificationProvider';
import { logPlannedMealAsPlanned, unmarkPlannedMeal, type NutritionMeal } from '../api/nutrition';

/**
 * Marking a planned meal as eaten, and taking it back.
 *
 * <p>Shared by the two places that offer it — the nutrition page's meal list and
 * the dashboard's menu widget — because they are the same action on the same
 * data, and the parts worth getting right are exactly the parts that would have
 * drifted between two copies: which call undoes which, that a failed write must
 * not refresh (so the row never shows as done when the server said no), and
 * that the wait is tracked per meal rather than page-wide, so one row in flight
 * does not freeze the other four.
 *
 * <p>`reload` is the caller's own refresh of the day's consumption. The two
 * screens hold that state in different places, and this hook has no business
 * owning it — it just says when it went stale.
 */
export function usePlannedMealToggle(
  dateIso: string,
  reload: () => Promise<unknown>,
): {
  readonly marking: ReadonlySet<string>;
  readonly toggle: (meal: NutritionMeal, eaten: boolean) => void;
} {
  const notify = useNotify();
  const [marking, setMarking] = useState<ReadonlySet<string>>(() => new Set());

  const toggle = (meal: NutritionMeal, eaten: boolean) => {
    setMarking((current) => new Set(current).add(meal.id));
    (eaten ? unmarkPlannedMeal(dateIso, meal.id) : logPlannedMealAsPlanned(dateIso, meal))
      .then(reload)
      .catch(() => notify.error('No se pudo actualizar la comida. Inténtalo de nuevo.'))
      .finally(() =>
        setMarking((current) => {
          const next = new Set(current);
          next.delete(meal.id);
          return next;
        }),
      );
  };

  return { marking, toggle };
}
