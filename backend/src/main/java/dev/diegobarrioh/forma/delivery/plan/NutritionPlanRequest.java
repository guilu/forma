package dev.diegobarrioh.forma.delivery.plan;

import dev.diegobarrioh.forma.application.NutritionPlan;
import dev.diegobarrioh.forma.application.PlanDay;
import dev.diegobarrioh.forma.application.PlanGeneration;
import dev.diegobarrioh.forma.application.PlanItem;
import dev.diegobarrioh.forma.application.PlanMeal;
import dev.diegobarrioh.forma.application.PlanTargets;
import dev.diegobarrioh.forma.application.ValidationException;
import dev.diegobarrioh.forma.domain.MacroTargets;
import dev.diegobarrioh.forma.domain.MainGoal;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import dev.diegobarrioh.forma.domain.PlanOrigin;
import dev.diegobarrioh.forma.domain.PlanStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Request body for creating and replacing a nutrition plan (V53/V54).
 *
 * <p>The whole plan arrives at once — days, meals and lines — because that is how it is stored and
 * how it is edited. A partial write would need a rule for what an omitted day means, and "the
 * caller states the complete plan" needs none.
 *
 * <p>Ids are absent throughout. A day is identified by where it falls and a line by where it sits,
 * so there is nothing for the caller to send back; the rows are rewritten from the lists.
 */
public record NutritionPlanRequest(
    @NotBlank @Size(max = 200) String name,
    String description,
    MainGoal objective,
    LocalDate startDate,
    LocalDate endDate,
    Targets targets,
    Generation generation,
    @Valid List<Day> days) {

  /** A calorie band and single macro figures — how an objective is actually stated. */
  public record Targets(
      Integer kcalMin, Integer kcalMax, Double proteinG, Double carbsG, Double fatG) {}

  /** How the plan came to exist. Absent means somebody wrote it. */
  public record Generation(PlanOrigin by, String prompt, String metadata) {}

  public record Day(
      @Positive Integer weekNumber,
      @NotNull Integer dayNumber,
      NutritionDayType dayType,
      Macros targets,
      String notes,
      @Valid List<Meal> meals) {}

  public record Meal(
      @NotNull MealType mealType,
      @NotBlank @Size(max = 200) String name,
      LocalTime scheduledTime,
      Macros targets,
      String instructions,
      Boolean optional,
      @Valid List<Item> items) {}

  /**
   * One line. Exactly one of {@code foodId} and {@code recipeId}, and {@code amount} counts
   * whatever the line names — grams, portions of {@code servingId}, or servings of the dish.
   */
  public record Item(
      String foodId,
      String recipeId,
      String servingId,
      @NotNull @Positive Double amount,
      String preparationNotes,
      Boolean optional) {}

  public record Macros(Integer calories, Double proteinG, Double carbsG, Double fatG) {}

  /** The plan this body describes, for the given owner. Status is never taken from the body. */
  public NutritionPlan toPlan(UUID userId, PlanStatus status) {
    return new NutritionPlan(
        null,
        userId,
        name == null ? null : name.trim(),
        blankToNull(description),
        objective,
        status,
        startDate,
        endDate,
        targets == null
            ? PlanTargets.none()
            : new PlanTargets(
                targets.kcalMin(),
                targets.kcalMax(),
                targets.proteinG(),
                targets.carbsG(),
                targets.fatG()),
        generation == null || generation.by() == null
            ? PlanGeneration.byHand()
            : new PlanGeneration(
                generation.by(),
                blankToNull(generation.prompt()),
                blankToNull(generation.metadata())),
        days == null ? List.of() : days.stream().map(NutritionPlanRequest::day).toList());
  }

  private static PlanDay day(Day day) {
    return new PlanDay(
        null,
        day.weekNumber() == null ? 1 : day.weekNumber(),
        day.dayNumber(),
        day.dayType(),
        macros(day.targets()),
        blankToNull(day.notes()),
        day.meals() == null
            ? List.of()
            : day.meals().stream().map(NutritionPlanRequest::meal).toList());
  }

  private static PlanMeal meal(Meal meal) {
    return new PlanMeal(
        null,
        meal.mealType(),
        meal.name() == null ? null : meal.name().trim(),
        meal.scheduledTime(),
        macros(meal.targets()),
        blankToNull(meal.instructions()),
        Boolean.TRUE.equals(meal.optional()),
        meal.items() == null
            ? List.of()
            : meal.items().stream().map(NutritionPlanRequest::item).toList());
  }

  private static PlanItem item(Item item) {
    // Checked here as well as in PlanItem, and not redundantly: the record refuses an impossible
    // state for every caller, but an IllegalArgumentException raised while mapping a request body
    // reaches the client as a 500. Malformed input is the client's mistake and deserves to be told
    // so — the same fault a plain constructor throw caused for an unknown food group in #196.
    boolean isFood = notBlank(item.foodId());
    boolean isRecipe = notBlank(item.recipeId());
    if (isFood == isRecipe) {
      throw new ValidationException(
          "Cada línea es un alimento o una receta, nunca las dos cosas ni ninguna.");
    }
    if (notBlank(item.servingId()) && !isFood) {
      throw new ValidationException("Una ración cuenta raciones de un alimento, no de una receta.");
    }
    return new PlanItem(
        null,
        blankToNull(item.foodId()),
        blankToNull(item.recipeId()),
        blankToNull(item.servingId()),
        item.amount() == null ? 0 : item.amount(),
        blankToNull(item.preparationNotes()),
        Boolean.TRUE.equals(item.optional()));
  }

  private static MacroTargets macros(Macros macros) {
    return macros == null
        ? MacroTargets.none()
        : new MacroTargets(macros.calories(), macros.proteinG(), macros.carbsG(), macros.fatG());
  }

  private static boolean notBlank(String value) {
    return value != null && !value.isBlank();
  }

  /** A field the form left empty is a field nobody filled in, not an empty string (FOR-134). */
  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
