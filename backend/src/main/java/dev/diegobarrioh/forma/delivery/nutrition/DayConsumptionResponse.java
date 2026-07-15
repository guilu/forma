package dev.diegobarrioh.forma.delivery.nutrition;

import dev.diegobarrioh.forma.application.DayConsumption;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import java.time.LocalDate;
import java.util.List;

/**
 * Response body for {@code GET /api/v1/nutrition/consumption?date=} (FOR-127 api.md).
 *
 * <p>Delivery read model, distinct from the application {@link DayConsumption} view (ADR-005).
 * {@code target}/{@code comparison} are explicit JSON {@code null} (never omitted) when no plan
 * target is resolvable — matching the FOR-125 {@code GoalResponse} progress-null precedent. In this
 * slice they are <b>always</b> {@code null}: see {@link
 * dev.diegobarrioh.forma.application.MealLogService} javadoc for why (no date-to-day-type schedule
 * exists yet, so a calendar date cannot be resolved to a plan target). This is the spec's own
 * documented "no plan target" edge case, not a shortcut.
 */
public record DayConsumptionResponse(
    LocalDate date,
    Macros consumed,
    Macros target,
    Comparison comparison,
    List<EntrySummary> entries) {

  public record Macros(int kcal, double proteinG, double carbsG, double fatG) {
    static Macros from(NutritionTotals totals) {
      return new Macros(totals.calories(), totals.proteinG(), totals.carbsG(), totals.fatG());
    }
  }

  /** Present for API-shape parity with FOR-127 api.md; unused while target is always null. */
  public record Comparison(Integer kcalDelta, Boolean withinTarget) {}

  public record EntrySummary(String id, String mealType, String name, int kcal) {}

  public static DayConsumptionResponse from(DayConsumption view) {
    List<EntrySummary> entries =
        view.entries().stream()
            .map(
                stored ->
                    new EntrySummary(
                        stored.id(),
                        stored.entry().mealType().name(),
                        stored.entry().name(),
                        stored.entry().totals().calories()))
            .toList();
    return new DayConsumptionResponse(
        view.date(), Macros.from(view.consumed()), null, null, entries);
  }
}
