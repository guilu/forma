package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.MacroTargets;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import dev.diegobarrioh.forma.domain.TargetComparison;
import java.time.LocalDate;
import java.util.List;

/**
 * A day of a plan, worked out (V53/V54).
 *
 * <p>{@link #targets} is the EFFECTIVE target, resolved down a chain of three: what the day was
 * asked to hit, or failing that what the plan was, or failing that what the user's own profile says
 * (V20). Each level meaning "nothing decided here, ask above" is what keeps the profile's real
 * figures in one place instead of copied onto every plan that wants them.
 *
 * <p>{@link #totals} is the sum of the day's meals, computed here and stored nowhere. The two being
 * separate is what lets this say "you asked for 2320 and this comes to 2100" — which the model this
 * replaces could not, because it set each day's target to that day's own total and so could only
 * ever answer yes.
 *
 * @param dayType RUNNING / STRENGTH / REST; null when nobody classified it
 * @param weekNumber which week of the plan
 * @param dayNumber which day of that week, 1 = monday
 * @param date the calendar date, when the plan has a start date
 * @param notes free text from the plan
 * @param targets the effective target, after the fallback chain
 * @param totals what the day's meals come to
 * @param comparison whether the totals reach the target; null when there is no target to reach
 * @param meals the day's meals, in order
 */
public record ResolvedDay(
    NutritionDayType dayType,
    int weekNumber,
    int dayNumber,
    LocalDate date,
    String notes,
    MacroTargets targets,
    NutritionTotals totals,
    TargetComparison comparison,
    List<ResolvedMeal> meals) {

  public ResolvedDay {
    meals = List.copyOf(meals);
  }
}
