package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.domain.NutritionDayType;
import dev.diegobarrioh.forma.domain.PlanStatus;
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
 * The diet from the spreadsheet (V56), worked out against the real catalog.
 *
 * <p>Two things under test, and the second is the interesting one. That the transcription is
 * faithful — the right foods on the right days, in the amounts the sheet gives. And that what the
 * model CLAIMED each day adds up to can be checked against what the food actually comes to, which
 * is the validation section 11 of the source document asks for and the reason those figures went in
 * as targets rather than as totals.
 */
@SpringBootTest
@ActiveProfiles("test")
class ExcelDietPlanTest {

  private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-000000000000");

  @Autowired private NutritionPlanReader reader;
  @Autowired private NutritionPlanService plans;

  @BeforeEach
  void authenticate() {
    AuthTestSupport.authenticateThreadAsPlaceholderUser();
  }

  @AfterEach
  void clearAuth() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void isThePlanTheUserIsFollowing() {
    assertThat(plans.findActive(USER))
        .map(NutritionPlan::name)
        .contains("Dieta semanal — recomposición");
  }

  /** The plan it replaced is kept, not deleted: it is what the app followed before. */
  @Test
  void standsDownThePlanSeededByV54() {
    assertThat(plans.findAll(USER))
        .filteredOn(plan -> plan.name().equals("Plan base"))
        .singleElement()
        .extracting(NutritionPlan::status)
        .isEqualTo(PlanStatus.COMPLETED);
  }

  @Test
  void carriesTheCalorieBandFromTheSheetsTitle() {
    NutritionPlan plan = plans.findActive(USER).orElseThrow();

    assertThat(plan.targets().kcalMin()).isEqualTo(2200);
    assertThat(plan.targets().kcalMax()).isEqualTo(2400);
  }

  /**
   * The sheet's day kinds and the training policy agree, down to the push/pull/legs split.
   *
   * <p>Asserted rather than assumed: the diet and the training plan came from the same source, and
   * this is what would catch one of them being edited without the other.
   */
  @Test
  void classifiesTheWeekTheSameWayTheTrainingPolicyDoes() {
    List<ResolvedDay> days = reader.activePlanDays(USER);

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

  /** Monday, the row that states every amount, transcribed cell by cell. */
  @Test
  void transcribesMondayExactly() {
    ResolvedDay monday = day(1);

    assertThat(monday.meals())
        .extracting(ResolvedMeal::name)
        .containsExactly("Desayuno", "Media mañana", "Comida", "Merienda", "Cena");
    assertThat(monday.meals().getFirst().items())
        .extracting(ResolvedItem::label)
        .containsExactly("Copos de avena", "Whey proteína", "Plátano");
    assertThat(monday.meals().getFirst().items())
        .extracting(ResolvedItem::grams)
        .containsExactly(60.0, 30.0, 120.0);
    assertThat(monday.meals().get(4).items())
        .extracting(ResolvedItem::label)
        .containsExactly("Merluza", "Patata", "Ensalada preparada");
  }

  /**
   * A food the sheet names without a number counts ONE of its own portions.
   *
   * <p>Not grams, though it resolves to the same figure today: the sheet says "un plátano", and a
   * portion keeps saying that if somebody corrects what one weighs.
   */
  @Test
  void countsPortionsForTheFoodsTheSheetNamesWithoutANumber() {
    ResolvedDay monday = day(1);

    ResolvedItem banana = monday.meals().getFirst().items().get(2);
    assertThat(banana.label()).isEqualTo("Plátano");
    assertThat(banana.grams()).isEqualTo(120.0);
  }

  /** Turkey is the one place the sheet disagrees with the catalog's portion: 200 g, not 150. */
  @Test
  void writesGramsWhereTheSheetDiffersFromThePortion() {
    ResolvedItem turkey = day(2).meals().get(2).items().getFirst();

    assertThat(turkey.label()).isEqualTo("Pavo lonchas/corte");
    assertThat(turkey.grams()).isEqualTo(200.0);
  }

  /** Thursday's breakfast names the same dish as Monday's and gives no amounts. */
  @Test
  void carriesAmountsToTheDaysThatRepeatADishWithoutThem() {
    assertThat(day(4).meals().getFirst().items())
        .extracting(ResolvedItem::label, ResolvedItem::grams)
        .containsExactly(
            org.assertj.core.api.Assertions.tuple("Copos de avena", 60.0),
            org.assertj.core.api.Assertions.tuple("Whey proteína", 30.0),
            org.assertj.core.api.Assertions.tuple("Plátano", 120.0));
  }

  /** «Comida libre controlada» is a rule, not a list — section 8's own example. */
  @Test
  void keepsSundaysFreeLunchAsARuleWithNoFood() {
    ResolvedMeal lunch = day(7).meals().get(2);

    assertThat(lunch.items()).isEmpty();
    assertThat(lunch.instructions()).contains("Comida libre controlada");
    assertThat(lunch.totals().calories()).isZero();
  }

  /** «Whey opcional» — the sheet says optional, so the column says optional. */
  @Test
  void marksSundaysWheyOptional() {
    assertThat(day(7).meals().get(3).optional()).isTrue();
  }

  /** «Fruta», named five times without saying which, is never guessed at. */
  @Test
  void leavesTheUnnamedFruitAsAnInstruction() {
    ResolvedMeal snack = day(1).meals().get(3);

    assertThat(snack.instructions()).contains("fruta");
    assertThat(snack.items()).extracting(ResolvedItem::label).containsExactly("Yogur proteína");
  }

  /** The sheet gives no times, so none is invented. */
  @Test
  void setsNoMealTimes() {
    assertThat(reader.activePlanDays(USER))
        .flatExtracting(ResolvedDay::meals)
        .extracting(ResolvedMeal::scheduledTime)
        .containsOnlyNulls();
  }

  /** Every line resolves: no food id in the sheet is missing from the catalog. */
  @Test
  void namesOnlyFoodsTheCatalogHas() {
    assertThat(reader.activePlanDays(USER))
        .flatExtracting(ResolvedDay::meals)
        .flatExtracting(ResolvedMeal::items)
        .extracting(ResolvedItem::unresolved)
        .containsOnlyNulls();
  }

  /**
   * WHAT THE MODEL CLAIMED IS NOT THE SUM OF THE FOOD IT LISTED, and every day falls short.
   *
   * <p>This is the test the whole design was for, and it does not pass by being generous. The
   * per-day figures in the spreadsheet are the model's own estimate; the source document says
   * plainly that "la IA puede indicar un total incorrecto", which is why they went in as targets
   * and the app sums the food itself. Measured, the gap runs from 379 to 702 kcal a day:
   *
   * <pre>
   *   día   dicho   real      día   dicho   real
   *     1    2320   1798        4    2280   1662
   *     2    2350   1971        5    2300   1643
   *     3    2250   1730        6    2400   1698
   * </pre>
   *
   * <p>Part of it is honestly uncounted — an unnamed piece of fruit on five days, and Sunday's free
   * lunch. But a piece of fruit is a hundred calories and Monday is five hundred short, so most of
   * it is not that. Asserted rather than corrected: adding food until the numbers agreed would be
   * inventing a diet nobody wrote.
   */
  @Test
  void reportsEveryDayComingInUnderWhatTheModelClaimed() {
    for (ResolvedDay day : reader.activePlanDays(USER)) {
      assertThat(day.totals().calories())
          .describedAs("día %d: el modelo dijo %d kcal", day.dayNumber(), day.targets().calories())
          .isLessThan(day.targets().calories());
      assertThat(day.comparison()).isNotNull();
      assertThat(day.comparison().caloriesReached()).isFalse();
    }
  }

  /**
   * The model got the PROTEIN right and everything else wrong, which is worth pinning down.
   *
   * <p>On every day the protein it promised sits closer to the food than the calories do — usually
   * within a few grams, against calorie gaps of twenty per cent and more. Whatever it was doing
   * when it wrote those totals, it was tracking protein and estimating the rest.
   */
  @Test
  void landsFarCloserOnProteinThanOnCalories() {
    for (ResolvedDay day : reader.activePlanDays(USER)) {
      double proteinError =
          Math.abs(day.totals().proteinG() - day.targets().proteinG()) / day.targets().proteinG();
      double calorieError =
          Math.abs(day.totals().calories() - (double) day.targets().calories())
              / day.targets().calories();

      assertThat(proteinError)
          .describedAs(
              "día %d: proteína %.0f de %.0f, calorías %d de %d",
              day.dayNumber(),
              day.totals().proteinG(),
              day.targets().proteinG(),
              day.totals().calories(),
              day.targets().calories())
          .isLessThan(calorieError);
    }
  }

  private ResolvedDay day(int dayNumber) {
    return reader.activePlanDays(USER).stream()
        .filter(candidate -> candidate.dayNumber() == dayNumber)
        .findFirst()
        .orElseThrow();
  }
}
