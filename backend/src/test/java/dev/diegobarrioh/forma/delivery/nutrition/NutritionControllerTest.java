package dev.diegobarrioh.forma.delivery.nutrition;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.HydrationService;
import dev.diegobarrioh.forma.application.MealLogService;
import dev.diegobarrioh.forma.application.NutritionCalculationService;
import dev.diegobarrioh.forma.application.NutritionDayCatalogService;
import dev.diegobarrioh.forma.application.UserProfileService;
import dev.diegobarrioh.forma.domain.MealItem;
import dev.diegobarrioh.forma.domain.MealTemplate;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.domain.NutritionDay;
import dev.diegobarrioh.forma.domain.NutritionDayTemplate;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link NutritionController} (FOR-34, enriched by FOR-105): the day response
 * shape (targets, ordered meals, optional post-run, resolved food names, macro totals, target
 * comparison) and not-found handling.
 *
 * <p>The real {@link NutritionCalculationService} is loaded (not mocked, FOR-105) since it is a
 * plain, dependency-free {@code @Service} — this exercises the FOR-32 calculation end-to-end
 * through the controller rather than stubbing its output.
 */
@WebMvcTest(NutritionController.class)
@Import(NutritionCalculationService.class)
class NutritionControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private NutritionDayCatalogService service;
  @MockBean private MealLogService mealLogService;
  @MockBean private HydrationService hydrationService;
  @MockBean private UserProfileService profileService;

  /** Default to a completed first run so the plan is served; individual tests override. */
  @org.junit.jupiter.api.BeforeEach
  void onboardingCompletedByDefault() {
    when(profileService.firstRunCompleted()).thenReturn(true);
  }

  @Test
  void returnsAnEmptyDayBeforeOnboardingFirstRunGate() throws Exception {
    when(profileService.firstRunCompleted()).thenReturn(false);

    mockMvc
        .perform(get("/api/v1/nutrition/days/running"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("RUNNING"))
        .andExpect(jsonPath("$.meals").isEmpty());
  }

  private static NutritionDay runningDay() {
    NutritionDayTemplate template =
        new NutritionDayTemplate(NutritionDayType.RUNNING, 1940, 162, 271, 25, "note");
    MealTemplate breakfast =
        new MealTemplate(
            NutritionDayType.RUNNING,
            MealType.BREAKFAST,
            "Desayuno",
            LocalTime.of(8, 0),
            List.of(new MealItem("oats", 120)),
            null);
    MealTemplate postRun =
        new MealTemplate(
            NutritionDayType.RUNNING,
            MealType.POST_WORKOUT,
            "Recuperación (opcional)",
            LocalTime.of(20, 0),
            List.of(new MealItem("whey-protein", 20)),
            null);
    return new NutritionDay(template, List.of(breakfast, postRun));
  }

  @Test
  void returnsTheDayWithTargetsMealsAndOptionalPostRun() throws Exception {
    when(service.findByType(eq(NutritionDayType.RUNNING))).thenReturn(Optional.of(runningDay()));

    mockMvc
        .perform(get("/api/v1/nutrition/days/running"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("RUNNING"))
        .andExpect(jsonPath("$.targets.proteinG").value(162))
        .andExpect(jsonPath("$.meals[0].mealType").value("BREAKFAST"))
        // Food id resolved to its catalog name.
        .andExpect(jsonPath("$.meals[0].items[0].food").value("Copos de avena"))
        .andExpect(jsonPath("$.meals[0].optional").value(false))
        .andExpect(jsonPath("$.meals[1].mealType").value("POST_WORKOUT"))
        .andExpect(jsonPath("$.meals[1].optional").value(true));
  }

  @Test
  void returnsPerMealAndDayMacroTotalsAndTargetComparison() throws Exception {
    when(service.findByType(eq(NutritionDayType.RUNNING))).thenReturn(Optional.of(runningDay()));

    mockMvc
        .perform(get("/api/v1/nutrition/days/running"))
        .andExpect(status().isOk())
        // Breakfast: 120g oats (370 kcal/13.0P/60.0C/7.0F per 100g, FOR-152) -> FOR-32 mealTotals.
        .andExpect(jsonPath("$.meals[0].totals.calories").value(444))
        .andExpect(jsonPath("$.meals[0].totals.proteinG").value(15.6))
        .andExpect(jsonPath("$.meals[0].totals.carbsG").value(72.0))
        .andExpect(jsonPath("$.meals[0].totals.fatG").value(8.4))
        // Post-run: 20g whey protein (390 kcal/78P/8C/6F per 100g, FOR-152).
        .andExpect(jsonPath("$.meals[1].totals.calories").value(78))
        .andExpect(jsonPath("$.meals[1].totals.proteinG").value(15.6))
        .andExpect(jsonPath("$.meals[1].totals.carbsG").value(1.6))
        .andExpect(jsonPath("$.meals[1].totals.fatG").value(1.2))
        // Day totals: sum of raw contributions, rounded once (FOR-32 dayTotals).
        .andExpect(jsonPath("$.totals.calories").value(522))
        .andExpect(jsonPath("$.totals.proteinG").value(31.2))
        .andExpect(jsonPath("$.totals.carbsG").value(73.6))
        .andExpect(jsonPath("$.totals.fatG").value(9.6))
        // Day totals fall short of the 1940/162/271/25 targets on every macro.
        .andExpect(jsonPath("$.targetComparison.caloriesReached").value(false))
        .andExpect(jsonPath("$.targetComparison.proteinReached").value(false))
        .andExpect(jsonPath("$.targetComparison.carbsReached").value(false))
        .andExpect(jsonPath("$.targetComparison.fatReached").value(false));
  }

  @Test
  void unknownDayTypeReturnsNotFound() throws Exception {
    mockMvc
        .perform(get("/api/v1/nutrition/days/does-not-exist"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }
}
