package dev.diegobarrioh.forma.delivery.plan;

import dev.diegobarrioh.forma.application.NutritionPlan;
import dev.diegobarrioh.forma.application.PlanDay;
import dev.diegobarrioh.forma.application.PlanItem;
import dev.diegobarrioh.forma.application.PlanMeal;
import dev.diegobarrioh.forma.application.ResolvedDay;
import dev.diegobarrioh.forma.application.ResolvedItem;
import dev.diegobarrioh.forma.application.ResolvedMeal;
import dev.diegobarrioh.forma.domain.MacroTargets;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Response body for the plan endpoints (V53/V54).
 *
 * <p>Carries both what the plan SAYS and what it COMES TO. The stored side is what an editor puts
 * back in its fields — a line holds an amount and maybe a portion, never a weight — and the
 * resolved side is what that works out to today. Sending only the second would make the plan
 * uneditable; sending only the first would make the screen do the arithmetic, which is exactly the
 * duplication this model exists to avoid.
 *
 * <p>Nulls are real: a target nobody set is {@code null}, not {@code 0}. This is the one place in
 * the API where that distinction survives to the client, and it is why the editor can show an empty
 * field rather than a zero somebody has to notice is not a decision.
 */
public record NutritionPlanResponse(
    UUID id,
    String name,
    String description,
    String objective,
    String status,
    boolean active,
    LocalDate startDate,
    LocalDate endDate,
    Targets targets,
    Generation generation,
    List<Day> days) {

  public record Targets(
      Integer kcalMin, Integer kcalMax, Double proteinG, Double carbsG, Double fatG) {}

  public record Generation(String by, String prompt, String metadata) {}

  public record Macros(Integer calories, Double proteinG, Double carbsG, Double fatG) {

    static Macros from(MacroTargets targets) {
      return new Macros(targets.calories(), targets.proteinG(), targets.carbsG(), targets.fatG());
    }
  }

  public record Totals(int calories, double proteinG, double carbsG, double fatG) {

    static Totals from(NutritionTotals totals) {
      return new Totals(totals.calories(), totals.proteinG(), totals.carbsG(), totals.fatG());
    }
  }

  /**
   * @param targets what this day was asked to hit, as stored — null fields and all
   * @param effectiveTargets what applies after falling back to the plan and the profile
   * @param totals what its meals come to
   * @param date the calendar date, when the plan has a start date
   */
  public record Day(
      int weekNumber,
      int dayNumber,
      String dayType,
      LocalDate date,
      String notes,
      Macros targets,
      Macros effectiveTargets,
      Totals totals,
      List<Meal> meals) {}

  public record Meal(
      String mealType,
      String name,
      String scheduledTime,
      boolean optional,
      String instructions,
      Macros targets,
      Totals totals,
      List<Item> items) {}

  /**
   * @param foodId set when the line names a food; null when it names a dish
   * @param recipeId set when the line names a dish
   * @param servingId which portion {@code amount} counts; null means grams
   * @param amount how much, in the unit the other fields name
   * @param label the food's or dish's name, resolved
   * @param grams what the amount works out to
   * @param unresolved the id that could not be found, or null
   */
  public record Item(
      String foodId,
      String recipeId,
      String servingId,
      double amount,
      String preparationNotes,
      boolean optional,
      String label,
      double grams,
      Totals totals,
      String unresolved) {}

  /**
   * The plan with its worked-out days.
   *
   * <p>The two sides are zipped by position, which holds because both come from the same list in
   * the same order — the stored plan and its resolution are the same days, read once.
   */
  public static NutritionPlanResponse from(NutritionPlan plan, List<ResolvedDay> resolved) {
    Map<Integer, ResolvedDay> bySlot =
        resolved.stream()
            .collect(
                Collectors.toMap(
                    day -> slot(day.weekNumber(), day.dayNumber()), Function.identity()));
    List<Day> days =
        plan.days().stream()
            .map(day -> day(day, bySlot.get(slot(day.weekNumber(), day.dayNumber()))))
            .toList();
    return new NutritionPlanResponse(
        plan.id(),
        plan.name(),
        plan.description(),
        plan.objective() == null ? null : plan.objective().name(),
        plan.status().name(),
        plan.active(),
        plan.startDate(),
        plan.endDate(),
        new PlanTargetsView(plan).targets(),
        new Generation(
            plan.generation().by().name(),
            plan.generation().prompt(),
            plan.generation().metadata()),
        days);
  }

  /** The plan header only, for a list where nobody is going to read a hundred lines. */
  public static NutritionPlanResponse summary(NutritionPlan plan) {
    return from(plan, List.of());
  }

  private static int slot(int week, int day) {
    return week * 10 + day;
  }

  private static Day day(PlanDay day, ResolvedDay resolved) {
    List<Meal> meals =
        resolved == null
            ? day.meals().stream().map(meal -> meal(meal, null)).toList()
            : zipMeals(day.meals(), resolved.meals());
    return new Day(
        day.weekNumber(),
        day.dayNumber(),
        day.dayType() == null ? null : day.dayType().name(),
        resolved == null ? null : resolved.date(),
        day.notes(),
        Macros.from(day.targets()),
        resolved == null ? null : Macros.from(resolved.targets()),
        resolved == null ? null : Totals.from(resolved.totals()),
        meals);
  }

  private static List<Meal> zipMeals(List<PlanMeal> stored, List<ResolvedMeal> resolved) {
    return java.util.stream.IntStream.range(0, stored.size())
        .mapToObj(at -> meal(stored.get(at), at < resolved.size() ? resolved.get(at) : null))
        .toList();
  }

  private static Meal meal(PlanMeal meal, ResolvedMeal resolved) {
    List<Item> items =
        java.util.stream.IntStream.range(0, meal.items().size())
            .mapToObj(
                at ->
                    item(
                        meal.items().get(at),
                        resolved == null || at >= resolved.items().size()
                            ? null
                            : resolved.items().get(at)))
            .toList();
    return new Meal(
        meal.mealType().name(),
        meal.name(),
        meal.scheduledTime() == null ? null : meal.scheduledTime().toString(),
        meal.optional(),
        meal.instructions(),
        Macros.from(meal.targets()),
        resolved == null ? null : Totals.from(resolved.totals()),
        items);
  }

  private static Item item(PlanItem item, ResolvedItem resolved) {
    return new Item(
        item.foodId(),
        item.recipeId(),
        item.servingId(),
        item.amount(),
        item.preparationNotes(),
        item.optional(),
        resolved == null ? null : resolved.label(),
        resolved == null ? 0 : resolved.grams(),
        resolved == null ? null : Totals.from(resolved.totals()),
        resolved == null ? null : resolved.unresolved());
  }

  /** Small adapter so the plan's own targets map without repeating five field reads. */
  private record PlanTargetsView(NutritionPlan plan) {

    Targets targets() {
      return new Targets(
          plan.targets().kcalMin(),
          plan.targets().kcalMax(),
          plan.targets().proteinG(),
          plan.targets().carbsG(),
          plan.targets().fatG());
    }
  }
}
