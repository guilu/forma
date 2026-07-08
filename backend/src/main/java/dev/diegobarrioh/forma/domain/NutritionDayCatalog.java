package dev.diegobarrioh.forma.domain;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * The initial nutrition day templates (FOR-33): RUNNING, STRENGTH and REST days, each with meals
 * built from FOR-30 catalog foods.
 *
 * <p>Defined in code (consistent with the FOR-23/FOR-24/FOR-25 seed precedents) — no persistence/
 * migration. Referential integrity is enforced fail-fast at class initialization: {@link
 * NutritionCalculator} rejects any meal item referencing an unknown food id. Each day's macro
 * <em>targets</em> are the computed totals of its meals (FOR-32), so the default plan is
 * self-consistent; values are directional defaults the user edits later, not medical prescriptions.
 *
 * <p>Meals are chosen so daily protein lands around 150–170 g and running days front-load
 * carbohydrates (running carbs &gt; rest carbs).
 */
public final class NutritionDayCatalog {

  private static final List<NutritionDay> DAYS =
      List.of(
          day(
              NutritionDayType.RUNNING,
              runningMeals(),
              "Día de carrera: más carbohidratos, antes de correr."),
          day(
              NutritionDayType.STRENGTH,
              strengthMeals(),
              "Día de fuerza: proteína alta, carbohidratos moderados."),
          day(NutritionDayType.REST, restMeals(), "Día de descanso: menos carbohidratos."));

  private NutritionDayCatalog() {}

  /** All seeded nutrition days (immutable). */
  public static List<NutritionDay> days() {
    return DAYS;
  }

  /** Finds a seeded day by its type. */
  public static Optional<NutritionDay> findByType(NutritionDayType type) {
    return DAYS.stream().filter(day -> day.template().type() == type).findFirst();
  }

  /**
   * Builds a day, deriving its targets from the computed totals of its meals (also validates ids).
   */
  private static NutritionDay day(NutritionDayType type, List<MealTemplate> meals, String note) {
    NutritionTotals totals = NutritionCalculator.dayTotals(meals);
    NutritionDayTemplate template =
        new NutritionDayTemplate(
            type,
            totals.calories(),
            (int) Math.round(totals.proteinG()),
            (int) Math.round(totals.carbsG()),
            (int) Math.round(totals.fatG()),
            note);
    return new NutritionDay(template, meals);
  }

  private static MealTemplate meal(
      NutritionDayType day, MealType type, String name, LocalTime time, MealItem... items) {
    return new MealTemplate(day, type, name, time, List.of(items), null);
  }

  private static List<MealTemplate> runningMeals() {
    NutritionDayType d = NutritionDayType.RUNNING;
    return List.of(
        meal(
            d,
            MealType.BREAKFAST,
            "Desayuno",
            LocalTime.of(8, 0),
            new MealItem("oats", 120),
            new MealItem("banana", 120),
            new MealItem("whey-protein", 30)),
        meal(
            d,
            MealType.LUNCH,
            "Comida",
            LocalTime.of(14, 0),
            new MealItem("rice", 200),
            new MealItem("chicken", 200),
            new MealItem("vegetables", 150)),
        meal(
            d,
            MealType.PRE_WORKOUT,
            "Snack pre-carrera",
            LocalTime.of(18, 0),
            new MealItem("banana", 120),
            new MealItem("oats", 40)),
        meal(
            d,
            MealType.DINNER,
            "Cena ligera",
            LocalTime.of(21, 30),
            new MealItem("fish", 150),
            new MealItem("potato", 150),
            new MealItem("vegetables", 150)));
  }

  private static List<MealTemplate> strengthMeals() {
    NutritionDayType d = NutritionDayType.STRENGTH;
    return List.of(
        meal(
            d,
            MealType.BREAKFAST,
            "Desayuno",
            LocalTime.of(8, 0),
            new MealItem("eggs", 150),
            new MealItem("oats", 60),
            new MealItem("yogurt", 125)),
        meal(
            d,
            MealType.MID_MORNING,
            "Media mañana",
            LocalTime.of(11, 0),
            new MealItem("whey-protein", 30),
            new MealItem("banana", 100)),
        meal(
            d,
            MealType.LUNCH,
            "Comida",
            LocalTime.of(14, 0),
            new MealItem("chicken", 150),
            new MealItem("rice", 250),
            new MealItem("vegetables", 200)),
        meal(
            d,
            MealType.DINNER,
            "Cena",
            LocalTime.of(21, 0),
            new MealItem("turkey", 100),
            new MealItem("potato", 150),
            new MealItem("vegetables", 150)));
  }

  private static List<MealTemplate> restMeals() {
    NutritionDayType d = NutritionDayType.REST;
    return List.of(
        meal(
            d,
            MealType.BREAKFAST,
            "Desayuno",
            LocalTime.of(9, 0),
            new MealItem("eggs", 150),
            new MealItem("yogurt", 125),
            new MealItem("fresh-cheese", 100)),
        meal(
            d,
            MealType.MID_MORNING,
            "Media mañana",
            LocalTime.of(12, 0),
            new MealItem("yogurt", 125),
            new MealItem("whey-protein", 10)),
        meal(
            d,
            MealType.LUNCH,
            "Comida",
            LocalTime.of(14, 30),
            new MealItem("chicken", 200),
            new MealItem("vegetables", 200),
            new MealItem("potato", 200)),
        meal(
            d,
            MealType.DINNER,
            "Cena",
            LocalTime.of(21, 0),
            new MealItem("fish", 200),
            new MealItem("vegetables", 200)));
  }
}
