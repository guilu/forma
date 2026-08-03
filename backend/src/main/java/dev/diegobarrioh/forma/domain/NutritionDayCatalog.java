package dev.diegobarrioh.forma.domain;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * The initial nutrition day templates (FOR-33): RUNNING, STRENGTH and REST days, each with meals
 * built from catalog foods.
 *
 * <p>The meals themselves are defined in code (consistent with the FOR-23/FOR-24/FOR-25 seed
 * precedents) — they are food ids and gram amounts, which need no nutrition data to write down.
 * Each day's macro <em>targets</em> are the computed totals of its meals (FOR-32), so the default
 * plan is self-consistent; values are directional defaults the user edits later, not medical
 * prescriptions.
 *
 * <p>Those totals are computed per call from a caller-supplied {@link FoodLookup}, not once in a
 * static initializer. Foods live in {@code food_catalog}, and a static initializer runs before any
 * database is reachable — precomputing here would have forced nutrition values to stay compiled
 * into the jar. The cost is that referential integrity is no longer proven at class-load: a meal
 * referencing an unknown food id is now rejected when a day is built rather than when the class is
 * first touched. {@link NutritionCalculator} still rejects it rather than skipping it, so a bad id
 * fails loudly either way.
 *
 * <p>Meals are chosen so daily protein lands around 150–170 g and running days front-load
 * carbohydrates (running carbs &gt; rest carbs).
 */
public final class NutritionDayCatalog {

  /** A day's fixed part: everything that can be stated without resolving any food. */
  private record Seed(NutritionDayType type, List<MealTemplate> meals, String note) {}

  private static final List<Seed> SEEDS =
      List.of(
          new Seed(
              NutritionDayType.RUNNING,
              runningMeals(),
              "Día de carrera: más carbohidratos, antes de correr."),
          new Seed(
              NutritionDayType.STRENGTH,
              strengthMeals(),
              "Día de fuerza: proteína alta, carbohidratos moderados."),
          new Seed(NutritionDayType.REST, restMeals(), "Día de descanso: menos carbohidratos."));

  private NutritionDayCatalog() {}

  /** All seeded nutrition days, with targets computed from {@code foods}. */
  public static List<NutritionDay> days(FoodLookup foods) {
    return SEEDS.stream().map(seed -> day(seed, foods)).toList();
  }

  /** Finds a seeded day by its type, with targets computed from {@code foods}. */
  public static Optional<NutritionDay> findByType(NutritionDayType type, FoodLookup foods) {
    return SEEDS.stream()
        .filter(seed -> seed.type() == type)
        .findFirst()
        .map(seed -> day(seed, foods));
  }

  /**
   * Builds a day, deriving its targets from the computed totals of its meals (also validates ids).
   */
  private static NutritionDay day(Seed seed, FoodLookup foods) {
    NutritionTotals totals = NutritionCalculator.dayTotals(seed.meals(), foods);
    NutritionDayTemplate template =
        new NutritionDayTemplate(
            seed.type(),
            totals.calories(),
            (int) Math.round(totals.proteinG()),
            (int) Math.round(totals.carbsG()),
            (int) Math.round(totals.fatG()),
            seed.note());
    return new NutritionDay(template, seed.meals());
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
            new MealItem("chicken", 150),
            new MealItem("vegetables", 150)),
        meal(
            d,
            MealType.PRE_WORKOUT,
            "Snack pre-carrera",
            LocalTime.of(18, 0),
            new MealItem("banana", 120),
            new MealItem("oats", 40)),
        // Optional post-run recovery (late-run flow, FOR-34): skip if the daily protein target is
        // already met. Presented as optional in the UI.
        meal(
            d,
            MealType.POST_WORKOUT,
            "Recuperación (opcional)",
            LocalTime.of(20, 0),
            new MealItem("whey-protein", 20)),
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
