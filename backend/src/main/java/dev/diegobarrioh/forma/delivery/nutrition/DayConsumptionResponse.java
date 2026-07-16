package dev.diegobarrioh.forma.delivery.nutrition;

import dev.diegobarrioh.forma.application.DayConsumption;
import dev.diegobarrioh.forma.domain.NutritionDayTemplate;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import java.time.LocalDate;
import java.util.List;

/**
 * Response body for {@code GET /api/v1/nutrition/consumption?date=} (FOR-127 api.md, {@code
 * target}/{@code comparison} wired by FOR-128 api.md).
 *
 * <p>Delivery read model, distinct from the application {@link DayConsumption} view (ADR-005).
 * {@code dayType} is the date's resolved {@code NutritionDayType} (FOR-128, added per api.md's
 * recommendation so the UI can label the day). {@code target}/{@code comparison} are explicit JSON
 * {@code null} (never omitted) only in the fail-safe case where the catalog has no template for the
 * resolved day type — matching the FOR-125 {@code GoalResponse} progress-null precedent.
 *
 * <p><b>Discrepancy vs specs/FOR-128/api.md</b>: that doc's example {@code comparison} shows {@code
 * kcalDelta}/{@code withinTarget}, but the actual domain {@code TargetComparison} (FOR-32, reused
 * here as required by the story's non-functional requirement "no duplicated math") is a per-macro
 * reached/short comparison, not a kcal delta — the same shape already serialized by {@code
 * NutritionDayResponse.TargetComparison} for {@code GET /nutrition/days/{type}}. Computing a {@code
 * kcalDelta}/{@code withinTarget} pair would be new comparison logic that does not exist in the
 * domain, so this reuses {@code TargetComparison} as-is instead of inventing it. Documented per
 * AGENTS.md ("repository state has priority over docs; document the discrepancy").
 */
public record DayConsumptionResponse(
    LocalDate date,
    String dayType,
    Macros consumed,
    Macros target,
    Comparison comparison,
    List<EntrySummary> entries) {

  public record Macros(int kcal, double proteinG, double carbsG, double fatG) {
    static Macros from(NutritionTotals totals) {
      return new Macros(totals.calories(), totals.proteinG(), totals.carbsG(), totals.fatG());
    }

    static Macros from(NutritionDayTemplate target) {
      return new Macros(
          target.targetCalories(),
          target.targetProteinG(),
          target.targetCarbsG(),
          target.targetFatG());
    }
  }

  /**
   * Whether the day's consumed totals reach the resolved day type's targets, per macro (FOR-32
   * {@code TargetComparison}, reused as-is — see class javadoc for the api.md shape discrepancy).
   */
  public record Comparison(
      boolean caloriesReached, boolean proteinReached, boolean carbsReached, boolean fatReached) {
    static Comparison from(dev.diegobarrioh.forma.domain.TargetComparison comparison) {
      return new Comparison(
          comparison.caloriesReached(),
          comparison.proteinReached(),
          comparison.carbsReached(),
          comparison.fatReached());
    }
  }

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
    Macros target = view.target() == null ? null : Macros.from(view.target());
    Comparison comparison = view.comparison() == null ? null : Comparison.from(view.comparison());
    return new DayConsumptionResponse(
        view.date(),
        view.dayType() == null ? null : view.dayType().name(),
        Macros.from(view.consumed()),
        target,
        comparison,
        entries);
  }
}
