package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.FoodLookup;
import dev.diegobarrioh.forma.domain.MacroTargets;
import dev.diegobarrioh.forma.domain.MealItem;
import dev.diegobarrioh.forma.domain.NutritionCalculator;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Section 11's tolerance audit (ADR-015 slice 4): does a plan's claimed {@code target_*} match what
 * its items actually add up to, at both the day and the plan level?
 *
 * <p><b>Why this lives here and not in {@code domain/}.</b> {@code docs/FORMA_Spec_Modelo_Datos_
 * Plan_Alimentacion.md} section 11 puts the rule in general terms, but what it operates ON — {@link
 * NutritionPlan}, {@link PlanDay}, {@link PlanMeal}, {@link PlanItem}, {@link PlanTargets} — are
 * all {@code application} types, not {@code domain} ones (see their own package). A class in {@code
 * domain/} cannot depend on {@code application/} without inverting the layering ADR-001
 * establishes, so this has to sit beside the types it reads. What IS framework-free about it: no
 * Spring annotation, no repository, no database access of its own. It takes three narrow lookups —
 * {@link FoodLookup}, {@link ServingLookup}, {@link RecipeLookup} — in the same shape {@link
 * NutritionCalculator} already takes {@link FoodLookup}, so a test can hand it a map and the
 * application wires it to the real catalog. That is what "framework-free component" means for a
 * class that necessarily reads application-layer records: the class itself is a plain, stateless,
 * constructor-injected Java object, never a bean.
 *
 * <p><b>Why it duplicates a little of {@link NutritionPlanReader}'s resolution instead of calling
 * it.</b> {@code NutritionPlanReader.days(userId, planId)} only resolves a plan that is already
 * persisted and owned by a user. Slice 6's ingest needs to audit a plan BEFORE it decides whether
 * to write it, which has no {@code planId} yet — so this class resolves items itself rather than
 * depending on a reader that requires one. What is NOT duplicated is the arithmetic: every gram
 * this class computes is summed through {@link NutritionCalculator#itemTotals}, the same entry
 * point {@link NutritionPlanReader} uses, which is what guarantees this audit agrees with what the
 * screens show (ADR-015 decision 9: "reusing that is the only way the audit can agree with what the
 * screens will later show").
 *
 * <p><b>Report, never correct.</b> {@link #audit} returns a {@link ToleranceAuditReport}; nothing
 * about the input plan is touched. ADR-015 and V53 both argue the gap between claimed and computed
 * IS the evidence that the model was wrong — overwriting {@code target_*} with the recomputed sum
 * would destroy the only proof of that.
 *
 * <p><b>Distinct from {@link dev.diegobarrioh.forma.domain.TargetComparison}.</b> That type asks
 * "did the day reach its target" (four {@code >=} booleans, no tolerance, built for a screen
 * showing progress toward a goal). This asks "does the claimed number match the real one" (a
 * tolerance band around a claimed figure, built to catch an AI's bad arithmetic). A plan can be
 * short of its target (a legitimate deficit, day one of a cut) while still being ARITHMETICALLY
 * CORRECT about what it claims — those are different questions, and {@link TargetComparison} does
 * not become this audit no matter how it is extended, because "reached" and "matches what was
 * claimed" are not the same predicate even when they happen to agree on some rows.
 *
 * <p><b>Structural vs. arithmetic (ADR-015 decision 9).</b> This class assumes every item already
 * passed {@link NutritionPlanService}'s structural checks ({@code problemsIn}/{@code
 * requireUsableItems}): a real food or recipe, a serving that belongs to its food. An item that
 * fails that precondition makes {@link #audit} throw rather than silently produce a wrong number —
 * see {@link #foodTotals}, {@link #gramsOf} and {@link #recipeTotals}. Calling this before the
 * structural check has run is a programming error, not a case this audit is meant to report.
 *
 * <p><b>Not wired into anything (yet).</b> No endpoint calls this. Ingest is ADR-015 slice 6; this
 * class exists to be consumed by it and by nothing else today, deliberately, so that {@code POST
 * /api/v1/nutrition/plans} keeps behaving exactly as it does for a human-authored plan.
 */
public final class PlanToleranceAuditor {

  private final FoodLookup foods;
  private final ServingLookup servings;
  private final RecipeLookup recipes;
  private final ToleranceThresholds thresholds;

  /** Audits against {@link ToleranceThresholds#SECTION_11_DEFAULTS}. */
  public PlanToleranceAuditor(FoodLookup foods, ServingLookup servings, RecipeLookup recipes) {
    this(foods, servings, recipes, ToleranceThresholds.SECTION_11_DEFAULTS);
  }

  public PlanToleranceAuditor(
      FoodLookup foods,
      ServingLookup servings,
      RecipeLookup recipes,
      ToleranceThresholds thresholds) {
    this.foods = Objects.requireNonNull(foods, "foods must not be null");
    this.servings = Objects.requireNonNull(servings, "servings must not be null");
    this.recipes = Objects.requireNonNull(recipes, "recipes must not be null");
    this.thresholds = Objects.requireNonNull(thresholds, "thresholds must not be null");
  }

  /**
   * Recomputes every day of {@code plan} from its items and the food catalog, and compares each
   * recomputed figure — and their AVERAGE across the plan's days, at the plan level — against what
   * the plan claims.
   *
   * <p><b>Average, not sum, at the plan level — a deliberate correction over the first draft of
   * this class.</b> {@code nutrition_plan.target_kcal_min}/{@code target_kcal_max} reads like "the
   * whole plan should total this many kcal", but it is not: {@code NutritionPlanReader
   * #effectiveTargets} already establishes what it actually means, by using it as the FALLBACK for
   * a single day's target when that day sets none —{@code firstOf(day.targets().calories(),
   * middleOf(plan.targets().kcalMin(), plan.targets().kcalMax()), profileKcal)}. That is only
   * sensible if the plan-level band is a PER-DAY figure, the same grain as {@code
   * PlanDay#targets()}, not a whole-week total. V56 confirms it: its plan row claims 2200-2400 and
   * every one of its seven days ALSO claims a target_kcal inside that same 2200-2400 band — a
   * week's worth of days could never each be a whole week's target. Summing the days and comparing
   * against that band would therefore always overshoot by roughly 7x and report drift that has
   * nothing to do with section 11's question, on every plan, regardless of whether the plan's
   * arithmetic is actually right.
   *
   * @throws NullPointerException when {@code plan} is null
   * @throws IllegalArgumentException when an item names a food the catalog does not have ({@link
   *     NutritionCalculator} rejects it)
   * @throws IllegalStateException when an item names a serving or a recipe this audit's lookups do
   *     not have — see the class javadoc on the structural precondition
   */
  public ToleranceAuditReport audit(NutritionPlan plan) {
    Objects.requireNonNull(plan, "plan must not be null");
    List<ToleranceEntry> entries = new ArrayList<>();
    List<NutritionTotals> dayTotals = new ArrayList<>();
    for (PlanDay day : plan.days()) {
      NutritionTotals total = totalsOf(day);
      dayTotals.add(total);
      entries.addAll(dayEntries(day, total));
    }
    entries.addAll(planEntries(plan.targets(), dayTotals));
    return new ToleranceAuditReport(entries);
  }

  private List<ToleranceEntry> dayEntries(PlanDay day, NutritionTotals total) {
    MacroTargets targets = day.targets();
    List<ToleranceEntry> entries = new ArrayList<>(4);
    entries.add(
        kcalEntry(
            ToleranceScope.DAY,
            day.dayNumber(),
            targets.calories() == null ? null : targets.calories().doubleValue(),
            total.calories()));
    entries.add(
        gramsEntry(
            ToleranceScope.DAY,
            day.dayNumber(),
            ToleranceMetric.PROTEIN_G,
            targets.proteinG(),
            total.proteinG(),
            thresholds.proteinGrams()));
    entries.add(
        gramsEntry(
            ToleranceScope.DAY,
            day.dayNumber(),
            ToleranceMetric.CARBS_G,
            targets.carbsG(),
            total.carbsG(),
            thresholds.carbsGrams()));
    entries.add(
        gramsEntry(
            ToleranceScope.DAY,
            day.dayNumber(),
            ToleranceMetric.FAT_G,
            targets.fatG(),
            total.fatG(),
            thresholds.fatGrams()));
    return entries;
  }

  private List<ToleranceEntry> planEntries(PlanTargets targets, List<NutritionTotals> dayTotals) {
    List<ToleranceEntry> entries = new ArrayList<>(4);
    entries.add(
        kcalEntry(
            ToleranceScope.PLAN,
            null,
            midpoint(targets.kcalMin(), targets.kcalMax()),
            average(dayTotals, NutritionTotals::calories)));
    entries.add(
        gramsEntry(
            ToleranceScope.PLAN,
            null,
            ToleranceMetric.PROTEIN_G,
            targets.proteinG(),
            average(dayTotals, NutritionTotals::proteinG),
            thresholds.proteinGrams()));
    entries.add(
        gramsEntry(
            ToleranceScope.PLAN,
            null,
            ToleranceMetric.CARBS_G,
            targets.carbsG(),
            average(dayTotals, NutritionTotals::carbsG),
            thresholds.carbsGrams()));
    entries.add(
        gramsEntry(
            ToleranceScope.PLAN,
            null,
            ToleranceMetric.FAT_G,
            targets.fatG(),
            average(dayTotals, NutritionTotals::fatG),
            thresholds.fatGrams()));
    return entries;
  }

  /**
   * The plan-level figure to compare against: the mean of the days, since {@link PlanTargets} is a
   * per-day band/target, not a whole-plan total — see {@link #audit}'s javadoc. Zero days (a plan
   * nobody has written yet) averages to zero rather than dividing by zero.
   */
  private static double average(
      List<NutritionTotals> dayTotals,
      java.util.function.ToDoubleFunction<NutritionTotals> metric) {
    return dayTotals.isEmpty() ? 0.0 : dayTotals.stream().mapToDouble(metric).average().orElse(0.0);
  }

  private ToleranceEntry kcalEntry(
      ToleranceScope scope, Integer dayNumber, Double claimed, double calculated) {
    if (claimed == null) {
      return new ToleranceEntry(
          scope,
          dayNumber,
          ToleranceMetric.KCAL,
          null,
          calculated,
          null,
          null,
          ToleranceVerdict.NOT_CLAIMED);
    }
    double difference = calculated - claimed;
    double tolerance = Math.abs(claimed) * thresholds.kcalPercentage();
    return new ToleranceEntry(
        scope,
        dayNumber,
        ToleranceMetric.KCAL,
        claimed,
        calculated,
        difference,
        tolerance,
        verdictFor(difference, tolerance));
  }

  private ToleranceEntry gramsEntry(
      ToleranceScope scope,
      Integer dayNumber,
      ToleranceMetric metric,
      Double claimed,
      double calculated,
      double toleranceGrams) {
    if (claimed == null) {
      return new ToleranceEntry(
          scope, dayNumber, metric, null, calculated, null, null, ToleranceVerdict.NOT_CLAIMED);
    }
    double difference = calculated - claimed;
    return new ToleranceEntry(
        scope,
        dayNumber,
        metric,
        claimed,
        calculated,
        difference,
        toleranceGrams,
        verdictFor(difference, toleranceGrams));
  }

  /** Inclusive: a difference exactly at the tolerance's edge still counts as within it. */
  private static ToleranceVerdict verdictFor(double difference, double tolerance) {
    return Math.abs(difference) <= tolerance
        ? ToleranceVerdict.WITHIN_TOLERANCE
        : ToleranceVerdict.EXCEEDS_TOLERANCE;
  }

  /** A band collapses to its midpoint, mirroring {@code NutritionPlanReader#middleOf}. */
  private static Double midpoint(Integer min, Integer max) {
    if (min == null && max == null) {
      return null;
    }
    if (min == null) {
      return max.doubleValue();
    }
    if (max == null) {
      return min.doubleValue();
    }
    return (min + max) / 2.0;
  }

  private NutritionTotals totalsOf(PlanDay day) {
    List<NutritionTotals> mealTotals = new ArrayList<>();
    for (PlanMeal meal : day.meals()) {
      for (PlanItem item : meal.items()) {
        mealTotals.add(totalsOf(item));
      }
    }
    return sum(mealTotals);
  }

  private NutritionTotals totalsOf(PlanItem item) {
    return item.isRecipe() ? recipeTotals(item) : foodTotals(item);
  }

  private NutritionTotals foodTotals(PlanItem item) {
    double grams = gramsOf(item);
    // Floored the same way NutritionPlanReader floors it: a tenth of a gram of oats is not
    // something anybody weighs, and a very small portion should still contribute rather than
    // vanish to zero grams.
    return NutritionCalculator.itemTotals(
        new MealItem(item.foodId(), Math.max(1, (int) Math.round(grams))), foods);
  }

  private NutritionTotals recipeTotals(PlanItem item) {
    ResolvedRecipe dish =
        recipes
            .findById(item.recipeId())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No existe la receta en la auditoría de tolerancias: " + item.recipeId()));
    return scale(dish.perServing(), item.amount());
  }

  private double gramsOf(PlanItem item) {
    if (item.servingId() == null) {
      return item.amount();
    }
    return servings
        .find(item.servingId())
        .map(serving -> item.amount() * serving.grams().doubleValue())
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "No existe la ración en la auditoría de tolerancias: " + item.servingId()));
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
