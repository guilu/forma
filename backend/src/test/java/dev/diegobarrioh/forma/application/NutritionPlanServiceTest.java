package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.MacroTargets;
import dev.diegobarrioh.forma.domain.MainGoal;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import dev.diegobarrioh.forma.domain.PlanOrigin;
import dev.diegobarrioh.forma.domain.PlanStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Nutrition plans through the real database (V53): the four tables, the JDBC adapter and {@link
 * NutritionPlanService} together.
 *
 * <p>An integration test rather than one against a fake repository, because most of what is worth
 * proving here IS the database — the one-active-plan invariant, the whole-aggregate round trip, and
 * the fact that a plan of somebody else's is simply not found.
 */
@SpringBootTest
@ActiveProfiles("test")
class NutritionPlanServiceTest {

  /** The legacy single-user owner seeded by V26. */
  private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-000000000000");

  /** Nobody. Used to prove that another account's plan reads as absent, not as forbidden. */
  private static final UUID SOMEBODY_ELSE = UUID.fromString("99999999-9999-9999-9999-999999999999");

  @Autowired private NutritionPlanService service;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearPlans() {
    jdbcTemplate.update("DELETE FROM nutrition_plan_meal_item");
    jdbcTemplate.update("DELETE FROM nutrition_plan_meal");
    jdbcTemplate.update("DELETE FROM nutrition_plan_day");
    jdbcTemplate.update("DELETE FROM nutrition_plan");
  }

  @Test
  void writesAPlanWithItsDaysMealsAndItemsAndReadsItBackWhole() {
    NutritionPlan stored = service.create(weekOf(USER, "Semana base"));

    NutritionPlan read = service.findById(USER, stored.id());
    assertThat(read.name()).isEqualTo("Semana base");
    assertThat(read.days()).hasSize(2);
    assertThat(read.days().getFirst().meals()).hasSize(2);
    assertThat(read.days().getFirst().meals().getFirst().items())
        .extracting(PlanItem::foodId)
        .containsExactly("oats", "banana");
  }

  /** The order the plan was written in is the order it is read in; a day is not a set of meals. */
  @Test
  void keepsDaysMealsAndItemsInOrder() {
    NutritionPlan stored = service.create(weekOf(USER, "Orden"));

    NutritionPlan read = service.findById(USER, stored.id());
    assertThat(read.days()).extracting(PlanDay::dayNumber).containsExactly(1, 2);
    assertThat(read.days().getFirst().meals())
        .extracting(PlanMeal::mealType)
        .containsExactly(MealType.BREAKFAST, MealType.LUNCH);
  }

  @Test
  void carriesTargetsGenerationAndTheRestOfTheHeader() {
    NutritionPlan stored = service.create(weekOf(USER, "Con objetivos"));

    NutritionPlan read = service.findById(USER, stored.id());
    assertThat(read.targets().kcalMin()).isEqualTo(2200);
    assertThat(read.targets().kcalMax()).isEqualTo(2400);
    assertThat(read.objective()).isEqualTo(MainGoal.COMPOSICION);
    assertThat(read.generation().by()).isEqualTo(PlanOrigin.AI);
    assertThat(read.generation().metadata()).contains("claude-opus-5");
  }

  /** A target nobody set is null, not zero. Zero is a target of zero (FOR-134). */
  @Test
  void leavesUnsetTargetsNull() {
    NutritionPlan stored = service.create(weekOf(USER, "Sin objetivos de comida"));

    PlanMeal breakfast = service.findById(USER, stored.id()).days().getFirst().meals().getFirst();
    assertThat(breakfast.targets().unset()).isTrue();
    assertThat(breakfast.targets().calories()).isNull();
  }

  /**
   * Skippable is now a stored fact, not `mealType == POST_WORKOUT` written into the delivery layer.
   */
  @Test
  void remembersWhichMealsAreSkippable() {
    NutritionPlan stored = service.create(weekOf(USER, "Con opcional"));

    List<PlanMeal> meals = service.findById(USER, stored.id()).days().get(1).meals();
    assertThat(meals).extracting(PlanMeal::optional).containsExactly(false, true);
  }

  /** An amount counting portions keeps the portion it counts, so its grams stay derivable (V49). */
  @Test
  void keepsWhichPortionAnAmountCounts() {
    NutritionPlan stored = service.create(weekOf(USER, "Con raciones"));

    PlanItem banana =
        service.findById(USER, stored.id()).days().getFirst().meals().getFirst().items().get(1);
    assertThat(banana.servingId()).isEqualTo("banana");
    assertThat(banana.amount()).isEqualTo(1.0);
  }

  @Test
  void replacesTheStructureOnUpdateRatherThanAddingToIt() {
    NutritionPlan stored = service.create(weekOf(USER, "Semana base"));

    service.update(
        USER,
        stored.id(),
        new NutritionPlan(
            null,
            USER,
            "Semana recortada",
            null,
            null,
            PlanStatus.DRAFT,
            null,
            null,
            PlanTargets.none(),
            PlanGeneration.byHand(),
            List.of(dayOne())));

    NutritionPlan read = service.findById(USER, stored.id());
    assertThat(read.name()).isEqualTo("Semana recortada");
    assertThat(read.days()).hasSize(1);
  }

  /** Editing a plan is not activating it; the status is moved by the calls that own that rule. */
  @Test
  void leavesTheStatusAloneOnUpdate() {
    NutritionPlan stored = service.create(weekOf(USER, "Activo"));
    service.activate(USER, stored.id());

    NutritionPlan updated =
        service.update(
            USER,
            stored.id(),
            new NutritionPlan(
                null,
                USER,
                "Activo, editado",
                null,
                null,
                PlanStatus.DRAFT,
                null,
                null,
                PlanTargets.none(),
                PlanGeneration.byHand(),
                List.of(dayOne())));

    assertThat(updated.status()).isEqualTo(PlanStatus.ACTIVE);
  }

  /** Two plans a user could both be following is a question with two answers. */
  @Test
  void activatingAPlanStandsDownTheOneBefore() {
    NutritionPlan first = service.create(weekOf(USER, "Primero"));
    NutritionPlan second = service.create(weekOf(USER, "Segundo"));
    service.activate(USER, first.id());

    service.activate(USER, second.id());

    assertThat(service.findActive(USER)).map(NutritionPlan::id).contains(second.id());
    assertThat(service.findById(USER, first.id()).status()).isEqualTo(PlanStatus.COMPLETED);
  }

  /** A plan created asking to be active goes through the one path that knows the rule. */
  @Test
  void activatesAPlanThatAsksToBeCreatedActive() {
    NutritionPlan first = service.create(weekOf(USER, "Primero"));
    service.activate(USER, first.id());

    NutritionPlan born =
        service.create(
            new NutritionPlan(
                null,
                USER,
                "Nace activo",
                null,
                null,
                PlanStatus.ACTIVE,
                null,
                null,
                PlanTargets.none(),
                PlanGeneration.byHand(),
                List.of(dayOne())));

    assertThat(born.status()).isEqualTo(PlanStatus.ACTIVE);
    assertThat(service.findActive(USER)).map(NutritionPlan::id).contains(born.id());
    assertThat(service.findById(USER, first.id()).status()).isEqualTo(PlanStatus.COMPLETED);
  }

  @Test
  void refusesToActivateThroughThePlainStatusChange() {
    NutritionPlan stored = service.create(weekOf(USER, "Semana base"));

    assertThatThrownBy(() -> service.changeStatus(USER, stored.id(), PlanStatus.ACTIVE))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  void archivesAPlan() {
    NutritionPlan stored = service.create(weekOf(USER, "Semana base"));

    NutritionPlan archived = service.changeStatus(USER, stored.id(), PlanStatus.ARCHIVED);

    assertThat(archived.status()).isEqualTo(PlanStatus.ARCHIVED);
    assertThat(service.findActive(USER)).isEmpty();
  }

  /** Not "forbidden": an id somebody else owns answers exactly as an id nobody owns. */
  @Test
  void treatsAnotherAccountsPlanAsAbsent() {
    NutritionPlan stored = service.create(weekOf(USER, "Mío"));

    assertThatThrownBy(() -> service.findById(SOMEBODY_ELSE, stored.id()))
        .isInstanceOf(NotFoundException.class);
    assertThat(service.findAll(SOMEBODY_ELSE)).isEmpty();
  }

  /**
   * And it survives the attempt, which is the part a delete written back-to-front would get wrong.
   */
  @Test
  void refusesToDeleteAnotherAccountsPlan() {
    NutritionPlan stored = service.create(weekOf(USER, "Mío"));

    assertThatThrownBy(() -> service.delete(SOMEBODY_ELSE, stored.id()))
        .isInstanceOf(NotFoundException.class);
    assertThat(service.findById(USER, stored.id()).days()).hasSize(2);
  }

  @Test
  void removesAPlanAndEverythingUnderIt() {
    NutritionPlan stored = service.create(weekOf(USER, "Semana base"));

    service.delete(USER, stored.id());

    assertThat(service.findAll(USER)).isEmpty();
    assertThat(
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM nutrition_plan_day", Integer.class))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM nutrition_plan_meal_item", Integer.class))
        .isZero();
  }

  @Test
  void refusesAnItemNamingAFoodTheCatalogDoesNotHave() {
    assertThatThrownBy(() -> service.create(planWithItem(item("unicornio", null, 60))))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("unicornio");
  }

  @Test
  void refusesAnItemNamingARecipeNobodyWrote() {
    assertThatThrownBy(
            () ->
                service.create(
                    planWithItem(new PlanItem(null, null, "guiso-fantasma", null, 1, null, false))))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("guiso-fantasma");
  }

  /**
   * The rule V53 could not express as a constraint: a portion counts portions of ITS food. Two
   * slices of olive oil is arithmetically fine and means nothing.
   */
  @Test
  void refusesAPortionThatBelongsToAnotherFood() {
    assertThatThrownBy(() -> service.create(planWithItem(item("oats", "banana", 1))))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("no es de oats");
  }

  /** Nothing is written when one line is wrong, however far into the plan it sits. */
  @Test
  void writesNothingWhenOneLineIsUnusable() {
    assertThatThrownBy(() -> service.create(planWithItem(item("unicornio", null, 60))))
        .isInstanceOf(ValidationException.class);

    assertThat(service.findAll(USER)).isEmpty();
  }

  private static NutritionPlan planWithItem(PlanItem item) {
    return new NutritionPlan(
        null,
        USER,
        "Con una línea mala",
        null,
        null,
        PlanStatus.DRAFT,
        null,
        null,
        PlanTargets.none(),
        PlanGeneration.byHand(),
        List.of(
            new PlanDay(
                null,
                1,
                1,
                null,
                MacroTargets.none(),
                null,
                List.of(
                    new PlanMeal(
                        null,
                        MealType.BREAKFAST,
                        "Desayuno",
                        null,
                        MacroTargets.none(),
                        null,
                        false,
                        List.of(item))))));
  }

  private static NutritionPlan weekOf(UUID userId, String name) {
    return new NutritionPlan(
        null,
        userId,
        name,
        "Dos días, para probar el conjunto.",
        MainGoal.COMPOSICION,
        PlanStatus.DRAFT,
        LocalDate.of(2026, 8, 3),
        LocalDate.of(2026, 8, 30),
        new PlanTargets(2200, 2400, 165.0, 250.0, 65.0),
        new PlanGeneration(PlanOrigin.AI, "Genera una semana", "{\"model\":\"claude-opus-5\"}"),
        List.of(dayOne(), dayTwo()));
  }

  private static PlanDay dayOne() {
    return new PlanDay(
        null,
        1,
        1,
        NutritionDayType.RUNNING,
        new MacroTargets(2320, 165.0, 270.0, 65.0),
        "Running 4-5 km",
        List.of(
            new PlanMeal(
                null,
                MealType.BREAKFAST,
                "Desayuno",
                LocalTime.of(8, 0),
                MacroTargets.none(),
                null,
                false,
                List.of(item("oats", null, 60), item("banana", "banana", 1))),
            new PlanMeal(
                null,
                MealType.LUNCH,
                "Comida",
                LocalTime.of(14, 0),
                new MacroTargets(800, null, null, null),
                null,
                false,
                List.of(item("rice", null, 80)))));
  }

  private static PlanDay dayTwo() {
    return new PlanDay(
        null,
        1,
        2,
        NutritionDayType.STRENGTH,
        MacroTargets.none(),
        null,
        List.of(
            new PlanMeal(
                null,
                MealType.LUNCH,
                "Comida",
                null,
                MacroTargets.none(),
                null,
                false,
                List.of(item("chicken", null, 150))),
            new PlanMeal(
                null,
                MealType.POST_WORKOUT,
                "Recuperación",
                null,
                MacroTargets.none(),
                null,
                true,
                List.of(item("whey-protein", null, 30)))));
  }

  private static PlanItem item(String foodId, String servingId, double amount) {
    return new PlanItem(null, foodId, null, servingId, amount, null, false);
  }
}
