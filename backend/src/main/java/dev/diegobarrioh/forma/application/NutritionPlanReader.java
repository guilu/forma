package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.FoodItem;
import dev.diegobarrioh.forma.domain.MacroTargets;
import dev.diegobarrioh.forma.domain.MealItem;
import dev.diegobarrioh.forma.domain.NutritionCalculator;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import dev.diegobarrioh.forma.domain.PersonalTargets;
import dev.diegobarrioh.forma.domain.TargetComparison;
import dev.diegobarrioh.forma.domain.UserProfile;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Working a plan out: grams, macros and effective targets, computed on every read (V53/V54).
 *
 * <p>Nothing here is stored. A plan holds an amount and, sometimes, the portion that amount counts;
 * what that comes to in grams and in macros is answered from today's catalog every time. Correcting
 * a food moves every plan that uses it, and correcting a portion moves every plan that says "one
 * medium banana" — which is what whoever wrote that meant.
 *
 * <p>The macro arithmetic is {@link NutritionCalculator}'s, not a second copy: a resolved line is a
 * food and a number of grams, which is exactly a {@link MealItem}. This is the same per-100g
 * formula that computes a logged meal and a recipe, so a plan and a log built from the same foods
 * agree.
 */
@Service
public class NutritionPlanReader implements DayTargetSource {

  private final NutritionPlanService plans;
  private final FoodCatalogService foods;
  private final FoodServingRepository servings;
  private final RecipeService recipes;
  private final UserProfileService profiles;

  public NutritionPlanReader(
      NutritionPlanService plans,
      FoodCatalogService foods,
      FoodServingRepository servings,
      RecipeService recipes,
      UserProfileService profiles) {
    this.plans = plans;
    this.foods = foods;
    this.servings = servings;
    this.recipes = recipes;
    this.profiles = profiles;
  }

  /**
   * The first day of this kind in the user's active plan, worked out.
   *
   * <p>"First" and not "the one for today" because the callers that ask by kind are asking for the
   * shape of a running day, not for a date. A week has three running days and under the seeded plan
   * they carry the same meals; the model no longer requires that, so this returns the earliest and
   * says so rather than pretending the choice does not exist.
   */
  public Optional<ResolvedDay> findDayByType(UUID userId, NutritionDayType type) {
    return plans
        .findActive(userId)
        .flatMap(
            plan ->
                plan.days().stream()
                    .filter(day -> day.dayType() == type)
                    .findFirst()
                    .map(day -> resolve(plan, day)));
  }

  /** Every day of the user's active plan, worked out, in order. */
  public List<ResolvedDay> activePlanDays(UUID userId) {
    return plans
        .findActive(userId)
        .map(plan -> plan.days().stream().map(day -> resolve(plan, day)).toList())
        .orElse(List.of());
  }

  /** A whole plan of this user's, worked out. */
  public List<ResolvedDay> days(UUID userId, UUID planId) {
    NutritionPlan plan = plans.findById(userId, planId);
    return plan.days().stream().map(day -> resolve(plan, day)).toList();
  }

  /** What the user's active plan asks of the given kind of day, or empty when there is no plan. */
  @Override
  public Optional<MacroTargets> targetsForDayType(UUID userId, NutritionDayType type) {
    return findDayByType(userId, type).map(ResolvedDay::targets);
  }

  private ResolvedDay resolve(NutritionPlan plan, PlanDay day) {
    List<ResolvedMeal> meals = day.meals().stream().map(this::resolve).toList();
    NutritionTotals totals = sum(meals.stream().map(ResolvedMeal::totals).toList());
    MacroTargets targets = effectiveTargets(plan, day);
    return new ResolvedDay(
        day.dayType(),
        day.weekNumber(),
        day.dayNumber(),
        day.dateWithin(plan.startDate()).orElse(null),
        day.notes(),
        targets,
        totals,
        TargetComparison.of(totals, targets),
        meals);
  }

  private ResolvedMeal resolve(PlanMeal meal) {
    List<ResolvedItem> items = meal.items().stream().map(this::resolve).toList();
    return new ResolvedMeal(
        meal.mealType(),
        meal.name(),
        meal.scheduledTime(),
        meal.optional(),
        meal.instructions(),
        meal.targets(),
        sum(items.stream().map(ResolvedItem::totals).toList()),
        items);
  }

  private ResolvedItem resolve(PlanItem item) {
    return item.isRecipe() ? resolveRecipe(item) : resolveFood(item);
  }

  private ResolvedItem resolveFood(PlanItem item) {
    Optional<FoodItem> food = foods.findById(item.foodId());
    if (food.isEmpty()) {
      return unresolved(item, item.foodId());
    }
    double grams = gramsOf(item);
    // MealItem counts whole grams, and a tenth of a gram of oats is not something anybody weighs.
    // Floored at 1 so a very small portion still contributes rather than vanishing.
    NutritionTotals totals =
        NutritionCalculator.itemTotals(
            new MealItem(item.foodId(), Math.max(1, (int) Math.round(grams))), foods);
    return new ResolvedItem(
        food.get().name(), round1(grams), totals, item.optional(), item.preparationNotes(), null);
  }

  private ResolvedItem resolveRecipe(PlanItem item) {
    ResolvedRecipe dish;
    try {
      dish = recipes.findById(item.recipeId());
    } catch (NotFoundException absent) {
      return unresolved(item, item.recipeId());
    }
    // An amount of a dish counts its servings, so the numbers are its per-serving figures scaled.
    double grams = item.amount() * gramsPerServing(dish);
    NutritionTotals totals = scale(dish.perServing(), item.amount());
    return new ResolvedItem(
        dish.recipe().name(),
        round1(grams),
        totals,
        item.optional(),
        item.preparationNotes(),
        null);
  }

  /**
   * How many grams a line comes to.
   *
   * <p>The amount counts whatever the line names: grams when there is no portion, and portions of
   * that portion when there is. A portion that has gone falls back to reading the amount as grams
   * rather than to zero — a plan line that silently weighed nothing would drag the whole day's
   * totals down without saying why.
   */
  private double gramsOf(PlanItem item) {
    if (item.servingId() == null) {
      return item.amount();
    }
    return servings
        .find(item.servingId())
        .map(serving -> item.amount() * serving.grams().doubleValue())
        .orElse(item.amount());
  }

  private static double gramsPerServing(ResolvedRecipe dish) {
    BigDecimal total =
        dish.recipe().ingredients().stream()
            .map(RecipeIngredient::grams)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return total.doubleValue() / dish.recipe().servings();
  }

  private static ResolvedItem unresolved(PlanItem item, String id) {
    return new ResolvedItem(
        id, 0, new NutritionTotals(0, 0, 0, 0), item.optional(), item.preparationNotes(), id);
  }

  /**
   * The target that actually applies to a day: its own, or the plan's, or the user's profile.
   *
   * <p>Resolved per macro rather than per level, so a day that fixes only its calories still
   * inherits the rest. Each null means "nothing decided here", which is a different thing from a
   * target of zero (FOR-134) and is exactly why the chain can exist at all.
   *
   * <p>The profile's own figures are the floor (V20, sourced from the Perfil sheet). They are read
   * from where they live rather than copied onto every plan that wants them, so correcting the
   * profile corrects the plan.
   *
   * <p><b>Known gap in that floor.</b> Three of those four figures — base calories, carbohydrates
   * and fat — live in {@code PersonalTargets}, which nothing in the app can write: {@code
   * UserProfileService} carries the record through every update without ever setting it, so V20's
   * seed is its only writer and V23 deletes that row on any install where onboarding has not been
   * completed. Protein is the exception, reachable through {@code DefaultObjectives}. The chain
   * below is correct and will pick those up the moment something can set them; until then the
   * honest answer for a plan with no targets of its own is that nobody has decided one, which is
   * what this returns rather than a number nobody chose (FOR-134).
   */
  private MacroTargets effectiveTargets(NutritionPlan plan, PlanDay day) {
    UserProfile profile = profiles.get();
    PersonalTargets personal = profile.personalTargets();
    Integer profileKcal =
        personal.baseCaloriesKcal() == null ? null : (int) Math.round(personal.baseCaloriesKcal());
    return new MacroTargets(
        firstOf(
            day.targets().calories(),
            middleOf(plan.targets().kcalMin(), plan.targets().kcalMax()),
            profileKcal),
        firstOf(
            day.targets().proteinG(),
            plan.targets().proteinG(),
            profile.defaultObjectives().proteinTargetG()),
        firstOf(day.targets().carbsG(), plan.targets().carbsG(), personal.carbsTargetG()),
        firstOf(day.targets().fatG(), plan.targets().fatG(), personal.fatTargetG()));
  }

  /** A band collapses to its midpoint when a single number is what is being asked for. */
  private static Integer middleOf(Integer min, Integer max) {
    if (min == null) {
      return max;
    }
    if (max == null) {
      return min;
    }
    return (min + max) / 2;
  }

  @SafeVarargs
  private static <T> T firstOf(T... candidates) {
    for (T candidate : candidates) {
      if (candidate != null) {
        return candidate;
      }
    }
    return null;
  }

  private static NutritionTotals sum(List<NutritionTotals> parts) {
    int calories = 0;
    double protein = 0;
    double carbs = 0;
    double fat = 0;
    for (NutritionTotals part : parts) {
      calories += part.calories();
      protein += part.proteinG();
      carbs += part.carbsG();
      fat += part.fatG();
    }
    return new NutritionTotals(calories, round1(protein), round1(carbs), round1(fat));
  }

  private static NutritionTotals scale(NutritionTotals totals, double factor) {
    return new NutritionTotals(
        (int) Math.round(totals.calories() * factor),
        round1(totals.proteinG() * factor),
        round1(totals.carbsG() * factor),
        round1(totals.fatG() * factor));
  }

  private static double round1(double value) {
    return Math.round(value * 10.0) / 10.0;
  }
}
