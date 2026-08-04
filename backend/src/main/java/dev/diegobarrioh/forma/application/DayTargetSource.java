package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.MacroTargets;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import java.util.Optional;
import java.util.UUID;

/**
 * What a user's plan asks of a given kind of day (V53/V54).
 *
 * <p>A narrow contract rather than the whole {@link NutritionPlanReader}, in the same spirit as
 * {@link dev.diegobarrioh.forma.domain.FoodLookup}: the consumption read model needs one number per
 * macro and has no business knowing that behind it sit four tables, a fallback chain and a food
 * catalog. Stating what it needs also keeps it testable without standing up any of that.
 */
@FunctionalInterface
public interface DayTargetSource {

  /**
   * The effective target for that kind of day in the user's active plan.
   *
   * @return empty when the user has no active plan, or none of it classifies a day that way
   */
  Optional<MacroTargets> targetsForDayType(UUID userId, NutritionDayType type);
}
