package dev.diegobarrioh.forma.delivery.nutrition;

import dev.diegobarrioh.forma.application.DayConsumption;
import dev.diegobarrioh.forma.domain.KeyNutrientTotals;
import dev.diegobarrioh.forma.domain.MacroTargets;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import java.time.LocalDate;
import java.util.List;

/**
 * Response body for {@code GET /api/v1/nutrition/consumption?date=} (FOR-127 api.md, {@code
 * target}/{@code comparison} wired by FOR-128 api.md, {@code keyNutrients} added by FOR-134
 * api.md).
 *
 * <p>Delivery read model, distinct from the application {@link DayConsumption} view (ADR-005).
 * {@code dayType} is the date's resolved {@code NutritionDayType} (FOR-128, added per api.md's
 * recommendation so the UI can label the day). {@code target}/{@code comparison} are explicit JSON
 * {@code null} (never omitted) when the user has no active plan, or has one whose targets nobody
 * completed — matching the FOR-125 {@code GoalResponse} progress-null precedent.
 *
 * <p><b>Key nutrients (FOR-134).</b> {@code keyNutrients} is always present (never omitted); its
 * four fields are independently {@code null} when the day's logged entries don't honestly support a
 * value — see {@link KeyNutrientTotals}'s javadoc for the documented null/partial rule. Units:
 * {@code sodiumMg} is milligrams; {@code fiberG}/{@code sugarsG}/{@code saturatedFatG} are grams
 * (spec FOR-134 api.md).
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
    KeyNutrients keyNutrients,
    Macros target,
    Comparison comparison,
    List<EntrySummary> entries,
    List<PlannedMeal> plannedMeals) {

  public record Macros(int kcal, double proteinG, double carbsG, double fatG) {
    static Macros from(NutritionTotals totals) {
      return new Macros(totals.calories(), totals.proteinG(), totals.carbsG(), totals.fatG());
    }

    /**
     * The effective target of the user's active plan.
     *
     * <p>Only ever built when every macro is present — see {@link DayConsumptionResponse#from},
     * which pairs it with the comparison. A partial target has no honest shape here: rendered with
     * zeros it would read as "aim for nothing", and these four fields are primitives, so unboxing a
     * null one throws. That is not hypothetical; it shipped, and the plan chain produces exactly
     * such a target whenever a profile has some figures and not others.
     */
    static Macros from(MacroTargets target) {
      return new Macros(target.calories(), target.proteinG(), target.carbsG(), target.fatG());
    }
  }

  /**
   * Consumed key-nutrient totals (FOR-134). {@code sodiumMg} is milligrams; the other three fields
   * are grams. Any field may be {@code null} — see the class javadoc's documented null/partial
   * rule.
   */
  public record KeyNutrients(
      Double fiberG, Double sugarsG, Integer sodiumMg, Double saturatedFatG) {
    static KeyNutrients from(KeyNutrientTotals totals) {
      return new KeyNutrients(
          totals.fiberG(), totals.sugarsG(), totals.sodiumMg(), totals.saturatedFatG());
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

  /**
   * One of the day's planned meals and what has become of it (V55): EATEN, PENDING or SKIPPED.
   *
   * <p>Derived on every read from the entries pointing at it. The source document asks for a stored
   * status column; each of these is a question the rows already answer, and a stored one would go
   * stale by itself as the day goes on.
   */
  public record PlannedMeal(
      String id, String mealType, String name, boolean optional, String state) {

    static PlannedMeal from(dev.diegobarrioh.forma.application.PlannedMealStatus status) {
      return new PlannedMeal(
          status.plannedMealId(),
          status.mealType().name(),
          status.name(),
          status.optional(),
          status.state().name());
    }
  }

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
    // Target and comparison stand or fall together, and the comparison is the stricter test: it
    // exists only when all four macros do. Reporting a target the comparison could not be made
    // against would leave the client with a number and no way to know it was incomplete.
    Comparison comparison = view.comparison() == null ? null : Comparison.from(view.comparison());
    Macros target = comparison == null ? null : Macros.from(view.target());
    return new DayConsumptionResponse(
        view.date(),
        view.dayType() == null ? null : view.dayType().name(),
        Macros.from(view.consumed()),
        KeyNutrients.from(view.keyNutrients()),
        target,
        comparison,
        entries,
        view.plannedMeals().stream().map(PlannedMeal::from).toList());
  }
}
