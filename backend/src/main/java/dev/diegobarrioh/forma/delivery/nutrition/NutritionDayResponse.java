package dev.diegobarrioh.forma.delivery.nutrition;

import dev.diegobarrioh.forma.application.ResolvedDay;
import dev.diegobarrioh.forma.application.ResolvedItem;
import dev.diegobarrioh.forma.application.ResolvedMeal;
import dev.diegobarrioh.forma.domain.MacroTargets;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import java.util.List;

/**
 * Response body for {@code GET /api/v1/nutrition/days/{type}} (FOR-34, enriched by FOR-105, moved
 * onto real plans by V53/V54).
 *
 * <p>Delivery read model (ADR-005) over the application's {@link ResolvedDay}. The wire shape is
 * unchanged from when this served three constants compiled into the jar — the frontend reads the
 * same fields — but every figure now comes from a plan somebody owns, resolved against today's food
 * catalog on each request.
 *
 * <p>Two things it can now say that it could not before. {@code optional} is read from the plan
 * instead of being {@code mealType == POST_WORKOUT} decided here, which was a fact about one seeded
 * plan applied to every plan there would ever be. And {@code targets} can genuinely differ from
 * {@code totals}: the old catalog set each day's target to that day's own total, so {@code
 * targetComparison} could only ever answer yes.
 *
 * <p><b>A target nobody set renders as 0.</b> {@link MacroTargets} distinguishes "no target" from
 * "a target of zero" and this response does not, because the frontend has no third state to render
 * — its zeroed day IS its empty state. Giving it one is a change to the page rather than to the
 * model, so it is left as known debt rather than half-done here.
 */
public record NutritionDayResponse(
    String type,
    Targets targets,
    Totals totals,
    TargetComparison targetComparison,
    List<Meal> meals) {

  public record Targets(int calories, int proteinG, int carbsG, int fatG) {

    static Targets from(MacroTargets targets) {
      return new Targets(
          orZero(targets.calories()),
          (int) Math.round(orZero(targets.proteinG())),
          (int) Math.round(orZero(targets.carbsG())),
          (int) Math.round(orZero(targets.fatG())));
    }

    private static int orZero(Integer value) {
      return value == null ? 0 : value;
    }

    private static double orZero(Double value) {
      return value == null ? 0 : value;
    }
  }

  /** A meal or day's computed macro totals (FOR-32 {@link NutritionTotals}, carried as-is). */
  public record Totals(int calories, double proteinG, double carbsG, double fatG) {

    static Totals from(NutritionTotals totals) {
      return new Totals(totals.calories(), totals.proteinG(), totals.carbsG(), totals.fatG());
    }
  }

  /** Whether the day's totals reach its targets, per macro (FOR-32 {@code TargetComparison}). */
  public record TargetComparison(
      boolean caloriesReached, boolean proteinReached, boolean carbsReached, boolean fatReached) {

    static TargetComparison from(dev.diegobarrioh.forma.domain.TargetComparison comparison) {
      return new TargetComparison(
          comparison.caloriesReached(),
          comparison.proteinReached(),
          comparison.carbsReached(),
          comparison.fatReached());
    }
  }

  public record Meal(
      String mealType,
      String name,
      String preferredTime,
      boolean optional,
      Totals totals,
      List<Item> items) {}

  public record Item(String food, int quantityG) {}

  /**
   * Empty day: the requested type with zeroed targets/totals and no meals.
   *
   * <p>Two ways to get here now. The first-run gate (FOR-169) returns it before onboarding, as it
   * always did. The second is new and is the honest consequence of plans being owned: an account
   * with no active plan has no day to show, where before every account silently shared the same
   * three constants. The frontend treats an empty {@code meals} list as its empty state either way.
   */
  public static NutritionDayResponse empty(NutritionDayType type) {
    return new NutritionDayResponse(
        type.name(),
        new Targets(0, 0, 0, 0),
        new Totals(0, 0, 0, 0),
        new TargetComparison(false, false, false, false),
        List.of());
  }

  /** Maps a worked-out plan day to its API read model. No arithmetic happens here (ADR-001). */
  public static NutritionDayResponse from(NutritionDayType type, ResolvedDay day) {
    List<Meal> meals = day.meals().stream().map(NutritionDayResponse::meal).toList();
    dev.diegobarrioh.forma.domain.TargetComparison comparison = day.comparison();
    return new NutritionDayResponse(
        type.name(),
        Targets.from(day.targets()),
        Totals.from(day.totals()),
        comparison == null
            ? new TargetComparison(false, false, false, false)
            : TargetComparison.from(comparison),
        meals);
  }

  private static Meal meal(ResolvedMeal meal) {
    return new Meal(
        meal.mealType().name(),
        meal.name(),
        meal.scheduledTime() == null ? null : meal.scheduledTime().toString(),
        meal.optional(),
        Totals.from(meal.totals()),
        meal.items().stream().map(NutritionDayResponse::item).toList());
  }

  private static Item item(ResolvedItem item) {
    // Grams are rounded for the wire: the plan holds a tenth of a gram of precision so a portion
    // can be counted exactly, and nobody weighs oats to a decimal.
    return new Item(item.label(), (int) Math.round(item.grams()));
  }
}
