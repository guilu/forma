package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.NutritionDayType;
import java.util.Optional;
import java.util.UUID;

/**
 * What the user's active plan says about a kind of day (V53/V54, widened by V55).
 *
 * <p>Replaces {@code DayTargetSource}, which answered only "what does this kind of day aim for".
 * That was the whole need when the consumption read model compared totals to a target; V55 gave it
 * a second one — which meals the day plans, so it can say which of them have been eaten — and the
 * two come from the same lookup. Two ports over one query would have read the plan twice per
 * request to split an answer that arrives whole.
 *
 * <p>Still one question, and that is the test of whether a port has grown too far: "what does the
 * plan say about a running day?" is one thing to ask, however much the answer contains.
 */
@FunctionalInterface
public interface PlannedDaySource {

  /**
   * The day of that kind in the user's active plan, worked out.
   *
   * @return empty when the user has no active plan, or none of it classifies a day that way
   */
  Optional<ResolvedDay> dayOfType(UUID userId, NutritionDayType type);
}
