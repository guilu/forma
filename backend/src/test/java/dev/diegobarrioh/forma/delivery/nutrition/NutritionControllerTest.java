package dev.diegobarrioh.forma.delivery.nutrition;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.CurrentUserProvider;
import dev.diegobarrioh.forma.application.HydrationService;
import dev.diegobarrioh.forma.application.MealLogService;
import dev.diegobarrioh.forma.application.NutritionPlanReader;
import dev.diegobarrioh.forma.application.ResolvedDay;
import dev.diegobarrioh.forma.application.ResolvedItem;
import dev.diegobarrioh.forma.application.ResolvedMeal;
import dev.diegobarrioh.forma.domain.MacroTargets;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import dev.diegobarrioh.forma.domain.TargetComparison;
import dev.diegobarrioh.forma.support.WebMvcAuthTestConfig;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link NutritionController} (FOR-34, enriched by FOR-105, moved onto real
 * plans by V53/V54): the day response shape and how an absent plan is answered.
 *
 * <p>A pure mapping test now, with {@link NutritionPlanReader} mocked. The macro arithmetic moved
 * out of the delivery layer when the plan moved into the database — it is exercised against a real
 * plan and a real catalog in {@code NutritionPlanReaderTest}, where the numbers can be checked
 * against rows rather than against a hand-built fixture.
 */
@WebMvcTest(NutritionController.class)
@Import(WebMvcAuthTestConfig.class)
class NutritionControllerTest {

  private static final UUID SOMEBODY = UUID.randomUUID();

  @Autowired private MockMvc mockMvc;
  @MockBean private NutritionPlanReader planReader;
  @MockBean private CurrentUserProvider currentUserProvider;
  @MockBean private MealLogService mealLogService;
  @MockBean private HydrationService hydrationService;

  @BeforeEach
  void callerIsKnown() {
    when(currentUserProvider.currentUserId()).thenReturn(SOMEBODY);
  }

  /**
   * An account with no plan is not a missing resource. Before plans were owned, every account
   * shared three constants and the only way to reach this path was an unknown day type; now "nobody
   * has made a plan yet" is an ordinary state of the app.
   *
   * <p>This is also the path a plan nobody has STARTED takes (V58): it is a DRAFT, a DRAFT is not
   * the active plan, and the empty day falls out of the data. There used to be a second condition
   * here — the onboarding flag — which let this endpoint contradict {@code /consumption} about the
   * very same plan.
   */
  @Test
  void returnsAnEmptyDayWhenTheAccountHasNoActivePlan() throws Exception {
    when(planReader.findDayByType(any(), eq(NutritionDayType.RUNNING)))
        .thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/v1/nutrition/days/running"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("RUNNING"))
        .andExpect(jsonPath("$.meals").isEmpty())
        // No hay ResolvedDay del que leer una nota, así que no hay campo que mostrar como
        // cadena vacía (FOR-728 D2/D1).
        .andExpect(jsonPath("$.notes").doesNotExist());
  }

  @Test
  void returnsTheDayWithTargetsMealsAndTheSkippableOne() throws Exception {
    when(planReader.findDayByType(any(), eq(NutritionDayType.RUNNING)))
        .thenReturn(Optional.of(runningDay()));

    mockMvc
        .perform(get("/api/v1/nutrition/days/running"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("RUNNING"))
        .andExpect(jsonPath("$.targets.proteinG").value(162))
        // El id de la comida planificada viaja para que la pantalla pueda casar cada comida con su
        // estado; sin él solo quedaría emparejar por nombre y tipo, que dos comidas pueden repetir.
        .andExpect(jsonPath("$.meals[0].id").isNotEmpty())
        .andExpect(jsonPath("$.meals[0].mealType").value("BREAKFAST"))
        .andExpect(jsonPath("$.meals[0].name").value("Desayuno"))
        .andExpect(jsonPath("$.meals[0].preferredTime").value("08:00"))
        // The food's name, resolved by the reader; the plan itself only holds its id.
        .andExpect(jsonPath("$.meals[0].items[0].food").value("Copos de avena"))
        .andExpect(jsonPath("$.meals[0].items[0].quantityG").value(120))
        .andExpect(jsonPath("$.meals[0].optional").value(false))
        // Read from the plan, not from `mealType == POST_WORKOUT` decided here.
        .andExpect(jsonPath("$.meals[1].optional").value(true))
        // La nota del día viaja tal cual (FOR-728 D1).
        .andExpect(jsonPath("$.notes").value("note"))
        // La instrucción de la comida viaja cuando existe (FOR-728 D1).
        .andExpect(jsonPath("$.meals[0].instructions").value("avena remojada la noche anterior"))
        // Nadie fijó objetivo para esta comida: no se rellena con `Targets.from()` (FOR-728 D3).
        .andExpect(jsonPath("$.meals[0].targets").doesNotExist())
        // La comida sin instrucciones no publica ni etiqueta ni hueco (FOR-728 D2).
        .andExpect(jsonPath("$.meals[1].instructions").doesNotExist())
        // La nota de preparación del ítem viaja cuando existe (FOR-728 D1).
        .andExpect(jsonPath("$.meals[0].items[0].preparationNotes").value("a fuego lento"))
        // Un ítem no resuelto transporta el id ausente en vez de fingir cero gramos (FOR-728 D5).
        .andExpect(jsonPath("$.meals[0].items[0].unresolved").value("oats-legacy-id"))
        // El ítem resuelto de la segunda comida no lleva marca de no-resuelto.
        .andExpect(jsonPath("$.meals[1].items[0].unresolved").doesNotExist());
  }

  @Test
  void carriesPerMealAndPerDayTotalsAndTheTargetComparison() throws Exception {
    when(planReader.findDayByType(any(), eq(NutritionDayType.RUNNING)))
        .thenReturn(Optional.of(runningDay()));

    mockMvc
        .perform(get("/api/v1/nutrition/days/running"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meals[0].totals.calories").value(444))
        .andExpect(jsonPath("$.meals[1].totals.calories").value(78))
        .andExpect(jsonPath("$.totals.calories").value(522))
        .andExpect(jsonPath("$.totals.proteinG").value(31.2))
        .andExpect(jsonPath("$.targetComparison.caloriesReached").value(false))
        .andExpect(jsonPath("$.targetComparison.proteinReached").value(false));
  }

  /** A day whose targets nobody completed reports no comparison rather than a made-up one. */
  @Test
  void reportsNoComparisonWhenThereIsNoWholeTargetToReach() throws Exception {
    ResolvedDay day = runningDay();
    when(planReader.findDayByType(any(), eq(NutritionDayType.RUNNING)))
        .thenReturn(
            Optional.of(
                new ResolvedDay(
                    day.dayType(),
                    day.weekNumber(),
                    day.dayNumber(),
                    day.date(),
                    day.notes(),
                    MacroTargets.none(),
                    day.totals(),
                    null,
                    day.meals())));

    mockMvc
        .perform(get("/api/v1/nutrition/days/running"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.targets.calories").value(0))
        .andExpect(jsonPath("$.targetComparison.caloriesReached").value(false));
  }

  /**
   * The non-null branch of {@code meal.targets().unset() ? null : ...} had no coverage: both
   * fixture meals in {@link #runningDay()} use {@code MacroTargets.none()} (review finding #4).
   */
  @Test
  void publishesAMealsFullySetTargetsPerMacro() throws Exception {
    when(planReader.findDayByType(any(), eq(NutritionDayType.RUNNING)))
        .thenReturn(Optional.of(dayWithMealTargets(new MacroTargets(300, 20.0, 30.0, 8.0))));

    mockMvc
        .perform(get("/api/v1/nutrition/days/running"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meals[0].targets.calories").value(300))
        .andExpect(jsonPath("$.meals[0].targets.proteinG").value(20.0))
        .andExpect(jsonPath("$.meals[0].targets.carbsG").value(30.0))
        .andExpect(jsonPath("$.meals[0].targets.fatG").value(8.0));
  }

  /**
   * FOR-728 D3 forbids zero-filling a meal's unset macros — {@code null} means nobody decided,
   * which is not the same as zero. A meal with only {@code calories} set still went through {@code
   * Targets.from()} and published {@code proteinG:0,carbsG:0,fatG:0} anyway, because {@code
   * MacroTargets.unset()} only short-circuits when all four macros are null (review finding #2).
   */
  @Test
  void doesNotZeroFillAMealsUnsetMacrosWhenOnlyOneIsSet() throws Exception {
    when(planReader.findDayByType(any(), eq(NutritionDayType.RUNNING)))
        .thenReturn(Optional.of(dayWithMealTargets(new MacroTargets(300, null, null, null))));

    mockMvc
        .perform(get("/api/v1/nutrition/days/running"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meals[0].targets.calories").value(300))
        .andExpect(jsonPath("$.meals[0].targets.proteinG").doesNotExist())
        .andExpect(jsonPath("$.meals[0].targets.carbsG").doesNotExist())
        .andExpect(jsonPath("$.meals[0].targets.fatG").doesNotExist());
  }

  /**
   * {@link dev.diegobarrioh.forma.application.PlanMeal} defends against a null {@code targets} with
   * a compact-constructor default; {@link ResolvedMeal} did not, and {@code
   * NutritionDayResponse.meal()} calls {@code meal.targets().unset()} unconditionally, which NPEs
   * the whole endpoint the moment one meal's targets is null (review finding #3).
   */
  @Test
  void doesNotNpeWhenAResolvedMealsTargetsIsNull() throws Exception {
    when(planReader.findDayByType(any(), eq(NutritionDayType.RUNNING)))
        .thenReturn(Optional.of(dayWithMealTargets(null)));

    mockMvc
        .perform(get("/api/v1/nutrition/days/running"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meals[0].targets").doesNotExist());
  }

  @Test
  void unknownDayTypeReturnsNotFound() throws Exception {
    mockMvc
        .perform(get("/api/v1/nutrition/days/does-not-exist"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  /**
   * A single-meal day built fresh for each meal-targets test (review findings #2, #3, #4), rather
   * than reusing {@link #runningDay()}: that fixture's other assertions depend on its exact meal
   * count, indices and totals, which a third meal or a targets change would perturb.
   */
  private static ResolvedDay dayWithMealTargets(MacroTargets mealTargets) {
    ResolvedMeal meal =
        new ResolvedMeal(
            UUID.randomUUID(),
            MealType.SNACK,
            "Snack",
            null,
            false,
            null,
            mealTargets,
            new NutritionTotals(280, 18.0, 28.0, 7.0),
            List.of());
    return new ResolvedDay(
        NutritionDayType.RUNNING,
        1,
        1,
        null,
        null,
        MacroTargets.none(),
        meal.totals(),
        null,
        List.of(meal));
  }

  /** 120 g of oats and 20 g of whey, already worked out — the reader's job, mocked here. */
  private static ResolvedDay runningDay() {
    ResolvedMeal breakfast =
        new ResolvedMeal(
            UUID.randomUUID(),
            MealType.BREAKFAST,
            "Desayuno",
            LocalTime.of(8, 0),
            false,
            "avena remojada la noche anterior",
            MacroTargets.none(),
            new NutritionTotals(444, 15.6, 72.0, 8.4),
            List.of(
                // `unresolved` set alongside a real label/grams/totals is a state the real
                // resolver never produces (`NutritionPlanReader.unresolved()` always pairs it with
                // `label = id` and zero totals) — this fixture tests the DTO's own mapping in
                // isolation, not the resolver's invariant, so the two fields are combined on
                // purpose to prove `unresolved` alone drives the response's unresolved item
                // (review finding #13).
                new ResolvedItem(
                    "oats",
                    "Copos de avena",
                    120,
                    new NutritionTotals(444, 15.6, 72.0, 8.4),
                    false,
                    "a fuego lento",
                    "oats-legacy-id")));
    ResolvedMeal recovery =
        new ResolvedMeal(
            UUID.randomUUID(),
            MealType.POST_WORKOUT,
            "Recuperación (opcional)",
            LocalTime.of(20, 0),
            true,
            null,
            MacroTargets.none(),
            new NutritionTotals(78, 15.6, 1.6, 1.2),
            List.of(
                new ResolvedItem(
                    "whey-protein",
                    "Proteína whey",
                    20,
                    new NutritionTotals(78, 15.6, 1.6, 1.2),
                    false,
                    null,
                    null)));
    NutritionTotals totals = new NutritionTotals(522, 31.2, 73.6, 9.6);
    MacroTargets targets = new MacroTargets(1940, 162.0, 271.0, 25.0);
    return new ResolvedDay(
        NutritionDayType.RUNNING,
        1,
        1,
        null,
        "note",
        targets,
        totals,
        TargetComparison.of(totals, targets),
        List.of(breakfast, recovery));
  }
}
