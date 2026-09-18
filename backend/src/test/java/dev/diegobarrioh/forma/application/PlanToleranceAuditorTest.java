package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

import dev.diegobarrioh.forma.domain.FoodItem;
import dev.diegobarrioh.forma.domain.MacroTargets;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import dev.diegobarrioh.forma.domain.PlanStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PlanToleranceAuditor} (ADR-015 slice 4, section 11): does the plan's
 * claimed {@code target_*} match what its items actually add up to, at both scopes, within the
 * documented tolerances (±5&nbsp;% kcal, ±5&nbsp;g protein, ±10&nbsp;g carbs, ±5&nbsp;g fat)?
 *
 * <p>Plain JUnit 5 + AssertJ (ADR-007), against fixture {@link
 * dev.diegobarrioh.forma.domain.FoodLookup} / {@link ServingLookup} / {@link RecipeLookup} maps —
 * no Spring context. The regression test against the real V56 seeded plan lives separately in
 * {@code PlanToleranceAuditorExcelDietPlanTest} because it needs the real catalog.
 *
 * <p>Fixture food "x": 200 kcal / 20 g protein / 20 g carbs / 10 g fat per 100 g, chosen so that
 * 100 g of it produces round numbers that are easy to reason about at the tolerance edges.
 */
class PlanToleranceAuditorTest {

  private static final FoodItem FOOD_X =
      new FoodItem("x", "Fixture food", 200, 20.0, 20.0, 10.0, null);

  private static final Map<String, FoodItem> FOODS_BY_ID = Map.of("x", FOOD_X);

  private static final PlanToleranceAuditor AUDITOR =
      new PlanToleranceAuditor(
          id -> Optional.ofNullable(FOODS_BY_ID.get(id)),
          id -> Optional.empty(),
          id -> Optional.empty());

  @Test
  void rejectsANullPlan() {
    assertThatThrownBy(() -> AUDITOR.audit(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("plan");
  }

  /**
   * 100 g of the fixture food is exactly 200 kcal / 20 g protein / 20 g carbs / 10 g fat, and every
   * target below claims exactly that — day and plan alike. Nothing should be flagged, and
   * everything claimed should be on record as having been checked.
   */
  @Test
  void reportsNoDriftWhenClaimedMatchesWhatTheFoodAddsUpTo() {
    PlanDay day = dayOf(1, new MacroTargets(200, 20.0, 20.0, 10.0), foodItem("x", 100));
    NutritionPlan plan = planOf(new PlanTargets(200, 200, 20.0, 20.0, 10.0), day);

    ToleranceAuditReport report = AUDITOR.audit(plan);

    assertThat(report.hasDrift()).isFalse();
    assertThat(report.anythingClaimed()).isTrue();
    assertThat(report.entries()).hasSize(8); // 4 metrics x (1 day + plan)
    assertThat(report.entries())
        .allSatisfy(
            entry -> assertThat(entry.verdict()).isEqualTo(ToleranceVerdict.WITHIN_TOLERANCE));
  }

  /**
   * The day claims 300 kcal; the food adds up to 200. 100 kcal short is far past ±5&nbsp;% of 300
   * (15 kcal), which is the exact shape of V56's real drift (decision 9's own example).
   */
  @Test
  void flagsCaloriesThatDriftBeyondTolerance() {
    PlanDay day = dayOf(1, new MacroTargets(300, 20.0, 20.0, 10.0), foodItem("x", 100));
    NutritionPlan plan = planOf(PlanTargets.none(), day);

    ToleranceAuditReport report = AUDITOR.audit(plan);

    assertThat(report.hasDrift()).isTrue();
    List<ToleranceEntry> drifts = report.drifts();
    assertThat(drifts)
        .extracting(ToleranceEntry::scope, ToleranceEntry::dayNumber, ToleranceEntry::metric)
        .containsExactly(tuple(ToleranceScope.DAY, 1, ToleranceMetric.KCAL));
    ToleranceEntry kcal = drifts.getFirst();
    assertThat(kcal.claimed()).isEqualTo(300.0);
    assertThat(kcal.calculated()).isEqualTo(200.0);
    assertThat(kcal.difference()).isEqualTo(-100.0);
    assertThat(kcal.toleranceAbs()).isCloseTo(15.0, within(1e-9)); // 300 * 0.05
  }

  /**
   * A difference exactly equal to the tolerance is still claimed and checked out — the band is
   * inclusive, so "right at the edge" is not treated as drift.
   */
  @Test
  void treatsADifferenceExactlyAtToleranceAsWithinTolerance() {
    // protein claimed 25.0, food gives 20.0: |20 - 25| = 5.0 == proteinGrams tolerance (5.0).
    PlanDay day = dayOf(1, new MacroTargets(200, 25.0, 20.0, 10.0), foodItem("x", 100));
    NutritionPlan plan = planOf(PlanTargets.none(), day);

    ToleranceEntry protein = proteinDayEntry(AUDITOR.audit(plan));

    assertThat(protein.difference()).isEqualTo(-5.0);
    assertThat(protein.verdict()).isEqualTo(ToleranceVerdict.WITHIN_TOLERANCE);
  }

  /** One tenth of a gram past the same edge tips it into a finding. */
  @Test
  void treatsADifferenceJustBeyondToleranceAsExceeding() {
    PlanDay day = dayOf(1, new MacroTargets(200, 25.1, 20.0, 10.0), foodItem("x", 100));
    NutritionPlan plan = planOf(PlanTargets.none(), day);

    ToleranceEntry protein = proteinDayEntry(AUDITOR.audit(plan));

    assertThat(protein.difference()).isCloseTo(-5.1, within(1e-9));
    assertThat(protein.verdict()).isEqualTo(ToleranceVerdict.EXCEEDS_TOLERANCE);
  }

  /**
   * A plan with no {@code target_*} anywhere — day or plan level — has nothing to compare. That is
   * {@link ToleranceVerdict#NOT_CLAIMED}, deliberately distinct from a clean {@link
   * ToleranceVerdict#WITHIN_TOLERANCE} audit: nothing was verified, which is a different claim from
   * "verified and correct" (FOR-134's null-vs-zero argument, applied to the audit itself).
   */
  @Test
  void reportsNotClaimedRatherThanASilentPassWhenNoTargetsAreClaimedAtAll() {
    PlanDay day = dayOf(1, MacroTargets.none(), foodItem("x", 100));
    NutritionPlan plan = planOf(PlanTargets.none(), day);

    ToleranceAuditReport report = AUDITOR.audit(plan);

    assertThat(report.hasDrift()).isFalse();
    assertThat(report.anythingClaimed()).isFalse();
    assertThat(report.entries())
        .allSatisfy(
            entry -> {
              assertThat(entry.verdict()).isEqualTo(ToleranceVerdict.NOT_CLAIMED);
              assertThat(entry.claimed()).isNull();
              assertThat(entry.difference()).isNull();
              assertThat(entry.toleranceAbs()).isNull();
            });
    // The recomputed totals are still there, even with nothing to check them against.
    ToleranceEntry kcalDay =
        report.entries().stream()
            .filter(e -> e.scope() == ToleranceScope.DAY && e.metric() == ToleranceMetric.KCAL)
            .findFirst()
            .orElseThrow();
    assertThat(kcalDay.calculated()).isEqualTo(200.0);
  }

  /**
   * The plan-level target is a per-day band (V53's {@code target_kcal_min}/{@code target_kcal_max}
   * — confirmed by {@code NutritionPlanReader#effectiveTargets}, which uses that same band as the
   * FALLBACK for a single day lacking its own target, so it has to be a per-day figure and not a
   * whole-plan total). The claimed kcal is therefore the band's midpoint, and what it is checked
   * against is the AVERAGE daily total, not the week's sum — two 200 kcal days average to 200, not
   * 400.
   */
  @Test
  void checksThePlanLevelBandMidpointAgainstTheAverageDailyTotal() {
    PlanDay day1 = dayOf(1, MacroTargets.none(), foodItem("x", 100)); // 200 kcal
    PlanDay day2 = dayOf(2, MacroTargets.none(), foodItem("x", 100)); // 200 kcal
    NutritionPlan plan = planOf(new PlanTargets(150, 250, null, null, null), day1, day2);

    ToleranceEntry planKcal = planKcalEntry(AUDITOR.audit(plan));

    assertThat(planKcal.claimed()).isEqualTo(200.0); // (150 + 250) / 2
    assertThat(planKcal.calculated()).isEqualTo(200.0); // mean(200, 200), not 200 + 200
    assertThat(planKcal.verdict()).isEqualTo(ToleranceVerdict.WITHIN_TOLERANCE);
  }

  /**
   * The same fixture as above but with a band that would only match a SUMMED week (350-450) proves
   * the auditor is comparing against the average and not silently falling back to a sum: the sum
   * (400) would sit inside that band, but the average (200) is nowhere near it.
   */
  @Test
  void doesNotMistakeTheWeeklySumForThePlanLevelClaim() {
    PlanDay day1 = dayOf(1, MacroTargets.none(), foodItem("x", 100)); // 200 kcal
    PlanDay day2 = dayOf(2, MacroTargets.none(), foodItem("x", 100)); // 200 kcal
    NutritionPlan plan = planOf(new PlanTargets(350, 450, null, null, null), day1, day2);

    ToleranceEntry planKcal = planKcalEntry(AUDITOR.audit(plan));

    assertThat(planKcal.calculated()).isEqualTo(200.0);
    assertThat(planKcal.verdict()).isEqualTo(ToleranceVerdict.EXCEEDS_TOLERANCE);
  }

  /**
   * A recipe line counts servings of the dish (V52), not grams of a food, so it is resolved through
   * {@link ResolvedRecipe#perServing()} scaled by the amount rather than through {@link
   * dev.diegobarrioh.forma.domain.NutritionCalculator} directly.
   */
  @Test
  void resolvesRecipeItemsThroughPerServingTotals() {
    ResolvedRecipe dish =
        new ResolvedRecipe(
            new Recipe("dish", "Guiso", 4, null, true, List.of(), null, null),
            new NutritionTotals(600, 40.0, 60.0, 20.0),
            new NutritionTotals(150, 10.0, 15.0, 5.0),
            List.of());
    PlanToleranceAuditor auditor =
        new PlanToleranceAuditor(
            id -> Optional.empty(),
            id -> Optional.empty(),
            id -> Optional.of(dish).filter(r -> id.equals("dish")));
    // 2 servings of the dish: 2 x (150, 10, 15, 5) = (300, 20, 30, 10).
    PlanDay day = dayOf(1, new MacroTargets(300, 20.0, 30.0, 10.0), recipeItem("dish", 2));
    NutritionPlan plan = planOf(PlanTargets.none(), day);

    ToleranceAuditReport report = auditor.audit(plan);

    assertThat(report.hasDrift()).isFalse();
  }

  @Test
  void failsLoudlyWhenAnItemNamesAnUnknownFood() {
    PlanDay day = dayOf(1, MacroTargets.none(), foodItem("ghost", 100));
    NutritionPlan plan = planOf(PlanTargets.none(), day);

    assertThatThrownBy(() -> AUDITOR.audit(plan))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ghost");
  }

  @Test
  void failsLoudlyWhenAnItemNamesAnUnknownServing() {
    PlanItem item = new PlanItem(null, "x", null, "ghost-serving", 1.0, null, false);
    PlanDay day = dayOf(1, MacroTargets.none(), item);
    NutritionPlan plan = planOf(PlanTargets.none(), day);

    assertThatThrownBy(() -> AUDITOR.audit(plan))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ghost-serving");
  }

  @Test
  void failsLoudlyWhenAnItemNamesAnUnknownRecipe() {
    PlanDay day = dayOf(1, MacroTargets.none(), recipeItem("ghost-recipe", 1));
    NutritionPlan plan = planOf(PlanTargets.none(), day);

    assertThatThrownBy(() -> AUDITOR.audit(plan))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ghost-recipe");
  }

  private static ToleranceEntry proteinDayEntry(ToleranceAuditReport report) {
    return report.entries().stream()
        .filter(e -> e.scope() == ToleranceScope.DAY && e.metric() == ToleranceMetric.PROTEIN_G)
        .findFirst()
        .orElseThrow();
  }

  private static ToleranceEntry planKcalEntry(ToleranceAuditReport report) {
    return report.entries().stream()
        .filter(e -> e.scope() == ToleranceScope.PLAN && e.metric() == ToleranceMetric.KCAL)
        .findFirst()
        .orElseThrow();
  }

  private static NutritionPlan planOf(PlanTargets targets, PlanDay... days) {
    return new NutritionPlan(
        null,
        UUID.randomUUID(),
        "Fixture plan",
        null,
        null,
        PlanStatus.DRAFT,
        null,
        null,
        targets,
        null,
        List.of(days));
  }

  private static PlanDay dayOf(int dayNumber, MacroTargets targets, PlanItem... items) {
    PlanMeal meal =
        new PlanMeal(
            null, MealType.LUNCH, "Comida", null, MacroTargets.none(), null, false, List.of(items));
    return new PlanDay(null, 1, dayNumber, null, targets, null, List.of(meal));
  }

  private static PlanItem foodItem(String foodId, double grams) {
    return new PlanItem(null, foodId, null, null, grams, null, false);
  }

  private static PlanItem recipeItem(String recipeId, double servings) {
    return new PlanItem(null, null, recipeId, null, servings, null, false);
  }
}
