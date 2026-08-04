package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.domain.NutritionDayType;
import dev.diegobarrioh.forma.support.AuthTestSupport;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

/**
 * The seeded plan (V54), worked out against the real database and the real food catalog.
 *
 * <p>Two things under test. That V54 moved the week out of {@code NutritionDayCatalog.java} without
 * changing a single food or amount — the meals below are checked against what that class held. And
 * that the numbers are computed on read rather than stored, which is what makes the plan follow a
 * correction to the catalog instead of freezing what it said the day it was written.
 */
@SpringBootTest
@ActiveProfiles("test")
class NutritionPlanReaderTest {

  /** The legacy single-user owner V26 seeded, and the one V54 seeded the plan for. */
  private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-000000000000");

  @Autowired private NutritionPlanReader reader;
  @Autowired private NutritionPlanService plans;
  @Autowired private UserProfileService profiles;

  @BeforeEach
  void authenticate() {
    AuthTestSupport.authenticateThreadAsPlaceholderUser();
  }

  @AfterEach
  void clearAuth() {
    SecurityContextHolder.clearContext();
  }

  /**
   * The plan V54 seeded still exists, and is no longer the one being followed.
   *
   * <p>It was the active plan until V56 loaded the real diet from the spreadsheet and stood it
   * down. That is the model working, so these tests read it by name rather than by asking what is
   * active — which is a different question with a different answer now.
   */
  @Test
  void theSeededPlanIsStillThereAfterBeingStoodDown() {
    assertThat(plans.findAll(USER))
        .filteredOn(plan -> plan.name().equals("Plan base"))
        .singleElement()
        .extracting(NutritionPlan::status)
        .isEqualTo(dev.diegobarrioh.forma.domain.PlanStatus.COMPLETED);
  }

  /** The days of the V54 plan, whichever plan happens to be active. */
  private List<ResolvedDay> basePlanDays() {
    NutritionPlan base =
        plans.findAll(USER).stream()
            .filter(plan -> plan.name().equals("Plan base"))
            .findFirst()
            .orElseThrow();
    return reader.days(USER, base.id());
  }

  private ResolvedDay baseDayOfType(NutritionDayType type) {
    return basePlanDays().stream().filter(day -> day.dayType() == type).findFirst().orElseThrow();
  }

  /**
   * A real week, not three archetypes.
   *
   * <p>The kinds follow WeeklyTrainingDayPolicy — running Mon/Wed/Sat, strength Tue/Thu/Sun, rest
   * Fri — read from the same policy the training calendar runs on rather than decided again here.
   */
  @Test
  void holdsSevenDaysOfTheRightKinds() {
    List<ResolvedDay> days = basePlanDays();

    assertThat(days).hasSize(7);
    assertThat(days)
        .extracting(ResolvedDay::dayType)
        .containsExactly(
            NutritionDayType.RUNNING,
            NutritionDayType.STRENGTH,
            NutritionDayType.RUNNING,
            NutritionDayType.STRENGTH,
            NutritionDayType.REST,
            NutritionDayType.RUNNING,
            NutritionDayType.STRENGTH);
  }

  /** The running day's meals, transcribed from the class V54 deleted. */
  @Test
  void keepsTheRunningDayExactlyAsTheOldCatalogHadIt() {
    ResolvedDay day = baseDayOfType(NutritionDayType.RUNNING);

    assertThat(day.meals())
        .extracting(ResolvedMeal::name)
        .containsExactly(
            "Desayuno", "Comida", "Snack pre-carrera", "Recuperación (opcional)", "Cena ligera");
    assertThat(day.meals().getFirst().items())
        .extracting(ResolvedItem::label)
        .containsExactly("Copos de avena", "Plátano", "Whey proteína");
    assertThat(day.meals().getFirst().items())
        .extracting(ResolvedItem::grams)
        .containsExactly(120.0, 120.0, 30.0);
  }

  /**
   * Skippable is a stored fact now, not `mealType == POST_WORKOUT` decided in the delivery layer.
   */
  @Test
  void marksOnlyTheRecoveryMealSkippable() {
    ResolvedDay day = baseDayOfType(NutritionDayType.RUNNING);

    assertThat(day.meals())
        .extracting(ResolvedMeal::optional)
        .containsExactly(false, false, false, true, false);
  }

  /** 120 g of oats at 370 kcal/100 g is 444, and nothing in the plan says so. */
  @Test
  void worksOutMacrosFromTheCatalogRatherThanFromStoredNumbers() {
    ResolvedDay day = baseDayOfType(NutritionDayType.RUNNING);

    ResolvedItem oats = day.meals().getFirst().items().getFirst();
    assertThat(oats.totals().calories()).isEqualTo(444);
    assertThat(day.meals().getFirst().totals().calories())
        .isEqualTo(
            day.meals().getFirst().items().stream()
                .mapToInt(item -> item.totals().calories())
                .sum());
    assertThat(day.totals().calories())
        .isEqualTo(day.meals().stream().mapToInt(meal -> meal.totals().calories()).sum());
  }

  /**
   * The seeded plan sets no target of its own, so its days show whatever the profile says — which
   * is nothing at all on an install where nobody has set one.
   *
   * <p>Asserted against the profile rather than against a literal, because that is the claim: the
   * figure is READ from where it lives instead of copied onto the plan. V54 deliberately seeds no
   * target (the old catalog set each day's target to that day's own total, which made every
   * comparison come out yes), and on a fresh install V23 removes the V20 profile seed too — so
   * {@code null} here is "nobody has decided", reported rather than invented (FOR-134).
   */
  @Test
  void takesTheSeededPlansTargetsFromTheProfile() {
    Double profileCalories = profiles.get().personalTargets().baseCaloriesKcal();

    ResolvedDay day = baseDayOfType(NutritionDayType.REST);

    assertThat(day.targets().calories())
        .isEqualTo(profileCalories == null ? null : (int) Math.round(profileCalories));
    if (profileCalories == null) {
      assertThat(day.comparison()).isNull();
    }
  }

  /** A target on the plan applies to every day that does not fix its own. */
  @Test
  void fallsBackFromTheDayToThePlansTarget() {
    UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000000");
    NutritionPlan withTargets =
        plans.create(
            new NutritionPlan(
                null,
                owner,
                "Con objetivo de plan",
                null,
                null,
                dev.diegobarrioh.forma.domain.PlanStatus.DRAFT,
                null,
                null,
                new PlanTargets(2200, 2400, 165.0, 250.0, 65.0),
                PlanGeneration.byHand(),
                List.of(
                    new PlanDay(
                        null,
                        1,
                        1,
                        NutritionDayType.RUNNING,
                        dev.diegobarrioh.forma.domain.MacroTargets.none(),
                        null,
                        List.of(
                            new PlanMeal(
                                null,
                                dev.diegobarrioh.forma.domain.MealType.BREAKFAST,
                                "Desayuno",
                                null,
                                dev.diegobarrioh.forma.domain.MacroTargets.none(),
                                null,
                                false,
                                List.of(
                                    new PlanItem(null, "oats", null, null, 60, null, false))))))));

    ResolvedDay day = reader.days(owner, withTargets.id()).getFirst();

    // The band collapses to its midpoint when one number is what is being asked for.
    assertThat(day.targets().calories()).isEqualTo(2300);
    assertThat(day.targets().proteinG()).isEqualTo(165.0);

    plans.delete(owner, withTargets.id());
  }

  /**
   * A day that fixes its own target keeps it, and the comparison can answer no.
   *
   * <p>Worth its own test because under the model this replaces it could not: the old catalog set
   * each day's target to that day's own computed total, so every seeded day reached its target by
   * construction and the comparison was a tautology.
   */
  @Test
  void canReportADayThatFallsShortOfItsOwnTarget() {
    UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000000");
    NutritionPlan demanding =
        plans.create(
            new NutritionPlan(
                null,
                owner,
                "Exigente",
                null,
                null,
                dev.diegobarrioh.forma.domain.PlanStatus.DRAFT,
                null,
                null,
                PlanTargets.none(),
                PlanGeneration.byHand(),
                List.of(
                    new PlanDay(
                        null,
                        1,
                        1,
                        NutritionDayType.RUNNING,
                        new dev.diegobarrioh.forma.domain.MacroTargets(3000, 200.0, 300.0, 80.0),
                        null,
                        List.of(
                            new PlanMeal(
                                null,
                                dev.diegobarrioh.forma.domain.MealType.BREAKFAST,
                                "Desayuno",
                                null,
                                dev.diegobarrioh.forma.domain.MacroTargets.none(),
                                null,
                                false,
                                List.of(
                                    new PlanItem(null, "oats", null, null, 60, null, false))))))));

    ResolvedDay day = reader.days(owner, demanding.id()).getFirst();

    assertThat(day.totals().calories()).isEqualTo(222);
    assertThat(day.targets().calories()).isEqualTo(3000);
    assertThat(day.comparison()).isNotNull();
    assertThat(day.comparison().caloriesReached()).isFalse();

    plans.delete(owner, demanding.id());
  }

  @Test
  void reportsNoDayForAnAccountWithoutAPlan() {
    UUID nobody = UUID.fromString("99999999-9999-9999-9999-999999999999");

    assertThat(reader.findDayByType(nobody, NutritionDayType.RUNNING)).isEmpty();
    assertThat(reader.activePlanDays(nobody)).isEmpty();
  }

  /** What the consumption read model asks for, through the port rather than the whole reader. */
  @Test
  void answersThroughThePort() {
    PlannedDaySource source = reader;

    assertThat(source.dayOfType(USER, NutritionDayType.STRENGTH)).isPresent();
    assertThat(
            source.dayOfType(
                UUID.fromString("99999999-9999-9999-9999-999999999999"), NutritionDayType.STRENGTH))
        .isEmpty();
  }

  /**
   * A planned meal is only the caller's to log against if it sits under one of their own plans.
   *
   * <p>The foreign key cannot say this: it knows the row exists, not whose it is (V55).
   */
  @Test
  void saysWhetherAPlannedMealIsTheCallersOwn() {
    ResolvedDay day = baseDayOfType(NutritionDayType.RUNNING);
    UUID mealId = day.meals().getFirst().id();

    assertThat(reader.ownsPlannedMeal(USER, mealId)).isTrue();
    assertThat(
            reader.ownsPlannedMeal(UUID.fromString("99999999-9999-9999-9999-999999999999"), mealId))
        .isFalse();
    assertThat(reader.ownsPlannedMeal(USER, UUID.randomUUID())).isFalse();
  }
}
