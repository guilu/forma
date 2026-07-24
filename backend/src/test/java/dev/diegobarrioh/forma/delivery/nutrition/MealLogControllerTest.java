package dev.diegobarrioh.forma.delivery.nutrition;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.DayConsumption;
import dev.diegobarrioh.forma.application.HydrationService;
import dev.diegobarrioh.forma.application.MealLogService;
import dev.diegobarrioh.forma.application.NutritionCalculationService;
import dev.diegobarrioh.forma.application.NutritionDayCatalogService;
import dev.diegobarrioh.forma.application.StoredMealLogEntry;
import dev.diegobarrioh.forma.application.UserProfileService;
import dev.diegobarrioh.forma.application.ValidationException;
import dev.diegobarrioh.forma.domain.KeyNutrientTotals;
import dev.diegobarrioh.forma.domain.MealLogEntry;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.domain.NutritionDayCatalog;
import dev.diegobarrioh.forma.domain.NutritionDayTemplate;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import dev.diegobarrioh.forma.domain.TargetComparison;
import dev.diegobarrioh.forma.support.WebMvcAuthTestConfig;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link NutritionController}'s FOR-127 endpoints ({@code POST /nutrition/log},
 * {@code GET /nutrition/consumption}): routing, request validation and response shape per {@code
 * specs/FOR-127/api.md} and {@code tests.md}. {@link MealLogService} is mocked, like {@code
 * GoalControllerTest} (FOR-125).
 */
@WebMvcTest(NutritionController.class)
@Import(WebMvcAuthTestConfig.class)
class MealLogControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private MealLogService mealLogService;
  @MockBean private NutritionDayCatalogService nutritionDayCatalogService;
  @MockBean private NutritionCalculationService nutritionCalculationService;
  @MockBean private HydrationService hydrationService;
  @MockBean private UserProfileService profileService;

  @Test
  void logsACatalogEntryAndReturns201WithTheStoredMacros() throws Exception {
    MealLogEntry entry =
        new MealLogEntry(
            LocalDate.of(2026, 7, 15),
            MealType.LUNCH,
            "Pollo (pechuga)",
            "chicken",
            new NutritionTotals(600, 40.0, 60.0, 20.0));
    when(mealLogService.log(any())).thenReturn(new StoredMealLogEntry("entry-1", entry));

    mockMvc
        .perform(
            post("/api/v1/nutrition/log")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"date":"2026-07-15","mealType":"LUNCH","foodItemId":"chicken","portions":1.5}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("entry-1"))
        .andExpect(jsonPath("$.date").value("2026-07-15"))
        .andExpect(jsonPath("$.mealType").value("LUNCH"))
        .andExpect(jsonPath("$.name").value("Pollo (pechuga)"))
        .andExpect(jsonPath("$.kcal").value(600))
        .andExpect(jsonPath("$.proteinG").value(40.0))
        .andExpect(jsonPath("$.carbsG").value(60.0))
        .andExpect(jsonPath("$.fatG").value(20.0));
  }

  @Test
  void logsAFreeEntryAndReturns201() throws Exception {
    MealLogEntry entry =
        new MealLogEntry(
            LocalDate.of(2026, 7, 15),
            MealType.MID_MORNING,
            "Café con leche",
            null,
            new NutritionTotals(90, 5.0, 8.0, 3.0));
    when(mealLogService.log(any())).thenReturn(new StoredMealLogEntry("entry-2", entry));

    mockMvc
        .perform(
            post("/api/v1/nutrition/log")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"date":"2026-07-15","mealType":"MID_MORNING","name":"Café con leche",
                     "kcal":90,"proteinG":5,"carbsG":8,"fatG":3}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Café con leche"));
  }

  @Test
  void logsAFreeEntryWithOptionalKeyNutrientsAndReturns201() throws Exception {
    // FOR-134: free/ad-hoc entries may optionally carry key nutrients; the request must not be
    // rejected for including them, and the cross-field/macro validation is unaffected.
    MealLogEntry entry =
        new MealLogEntry(
            LocalDate.of(2026, 7, 15),
            MealType.MID_MORNING,
            "Barrita",
            null,
            new NutritionTotals(180, 6.0, 24.0, 7.0));
    when(mealLogService.log(any())).thenReturn(new StoredMealLogEntry("entry-3", entry));

    mockMvc
        .perform(
            post("/api/v1/nutrition/log")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"date":"2026-07-15","mealType":"MID_MORNING","name":"Barrita",
                     "kcal":180,"proteinG":6,"carbsG":24,"fatG":7,
                     "fiberG":3,"sugarsG":12,"sodiumMg":90,"saturatedFatG":2}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Barrita"));
  }

  @Test
  void rejectsANegativeFreeEntryKeyNutrientWithValidationError() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/nutrition/log")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"date":"2026-07-15","mealType":"MID_MORNING","name":"Barrita",
                     "kcal":180,"proteinG":6,"carbsG":24,"fatG":7,"fiberG":-1}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void rejectsAnUnknownMealTypeWithValidationError() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/nutrition/log")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"date\":\"2026-07-15\",\"mealType\":\"SNACK\",\"foodItemId\":\"oats\","
                        + "\"portions\":1}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("mealType"));
  }

  @Test
  void rejectsNegativePortionsWithValidationError() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/nutrition/log")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"date\":\"2026-07-15\",\"mealType\":\"LUNCH\",\"foodItemId\":\"oats\","
                        + "\"portions\":-1}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void rejectsAnEntryWithNeitherFoodItemIdNorMacrosViaTheApplicationLayer() throws Exception {
    when(mealLogService.log(any()))
        .thenThrow(new ValidationException("Provide either foodItemId+portions or free macros"));

    mockMvc
        .perform(
            post("/api/v1/nutrition/log")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2026-07-15\",\"mealType\":\"LUNCH\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void consumptionBeforeAnyLogReturns200WithZeroedConsumedNeverA404() throws Exception {
    // 2026-07-15 is a Wednesday -> STRENGTH day (FOR-128), but this test only asserts consumed.
    NutritionDayTemplate strengthTemplate =
        NutritionDayCatalog.findByType(NutritionDayType.STRENGTH).orElseThrow().template();
    NutritionTotals zeroed = new NutritionTotals(0, 0.0, 0.0, 0.0);
    when(mealLogService.consumption(eq(LocalDate.of(2026, 7, 15))))
        .thenReturn(
            new DayConsumption(
                LocalDate.of(2026, 7, 15),
                NutritionDayType.STRENGTH,
                zeroed,
                KeyNutrientTotals.zero(),
                strengthTemplate,
                TargetComparison.of(zeroed, strengthTemplate),
                List.of()));

    mockMvc
        .perform(get("/api/v1/nutrition/consumption").param("date", "2026-07-15"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.consumed.kcal").value(0))
        .andExpect(jsonPath("$.keyNutrients.fiberG").value(0.0))
        .andExpect(jsonPath("$.keyNutrients.sodiumMg").value(0))
        .andExpect(jsonPath("$.entries").isArray())
        .andExpect(jsonPath("$.entries").isEmpty());
  }

  @Test
  void consumptionOnAStrengthDayReturnsTheResolvedDayTypeTargetAndComparison() throws Exception {
    MealLogEntry entry =
        new MealLogEntry(
            LocalDate.of(2026, 7, 15),
            MealType.LUNCH,
            "Pollo (pechuga)",
            "chicken",
            new NutritionTotals(600, 40.0, 60.0, 20.0));
    NutritionDayTemplate strengthTemplate =
        NutritionDayCatalog.findByType(NutritionDayType.STRENGTH).orElseThrow().template();
    NutritionTotals consumed = new NutritionTotals(600, 40.0, 60.0, 20.0);
    KeyNutrientTotals keyNutrients = new KeyNutrientTotals(22.0, 40.0, 1800, 12.0);
    when(mealLogService.consumption(eq(LocalDate.of(2026, 7, 15))))
        .thenReturn(
            new DayConsumption(
                LocalDate.of(2026, 7, 15),
                NutritionDayType.STRENGTH,
                consumed,
                keyNutrients,
                strengthTemplate,
                TargetComparison.of(consumed, strengthTemplate),
                List.of(new StoredMealLogEntry("entry-1", entry))));

    mockMvc
        .perform(get("/api/v1/nutrition/consumption").param("date", "2026-07-15"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dayType").value("STRENGTH"))
        .andExpect(jsonPath("$.consumed.kcal").value(600))
        .andExpect(jsonPath("$.keyNutrients.fiberG").value(22.0))
        .andExpect(jsonPath("$.keyNutrients.sugarsG").value(40.0))
        .andExpect(jsonPath("$.keyNutrients.sodiumMg").value(1800))
        .andExpect(jsonPath("$.keyNutrients.saturatedFatG").value(12.0))
        .andExpect(jsonPath("$.target.kcal").value(strengthTemplate.targetCalories()))
        .andExpect(jsonPath("$.comparison.caloriesReached").value(false))
        .andExpect(jsonPath("$.entries[0].id").value("entry-1"))
        .andExpect(jsonPath("$.entries[0].mealType").value("LUNCH"))
        .andExpect(jsonPath("$.entries[0].name").value("Pollo (pechuga)"))
        .andExpect(jsonPath("$.entries[0].kcal").value(600));
  }

  @Test
  void consumptionOnARunningDayReturnsTheRunningTemplateTarget() throws Exception {
    NutritionDayTemplate runningTemplate =
        NutritionDayCatalog.findByType(NutritionDayType.RUNNING).orElseThrow().template();
    NutritionTotals zeroed = new NutritionTotals(0, 0.0, 0.0, 0.0);
    when(mealLogService.consumption(eq(LocalDate.of(2026, 7, 18)))) // Saturday
        .thenReturn(
            new DayConsumption(
                LocalDate.of(2026, 7, 18),
                NutritionDayType.RUNNING,
                zeroed,
                KeyNutrientTotals.zero(),
                runningTemplate,
                TargetComparison.of(zeroed, runningTemplate),
                List.of()));

    mockMvc
        .perform(get("/api/v1/nutrition/consumption").param("date", "2026-07-18"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dayType").value("RUNNING"))
        .andExpect(jsonPath("$.target.kcal").value(runningTemplate.targetCalories()));
  }

  @Test
  void consumptionOnARestDayReturnsTheRestTemplateTarget() throws Exception {
    NutritionDayTemplate restTemplate =
        NutritionDayCatalog.findByType(NutritionDayType.REST).orElseThrow().template();
    NutritionTotals zeroed = new NutritionTotals(0, 0.0, 0.0, 0.0);
    when(mealLogService.consumption(eq(LocalDate.of(2026, 7, 19)))) // Sunday
        .thenReturn(
            new DayConsumption(
                LocalDate.of(2026, 7, 19),
                NutritionDayType.REST,
                zeroed,
                KeyNutrientTotals.zero(),
                restTemplate,
                TargetComparison.of(zeroed, restTemplate),
                List.of()));

    mockMvc
        .perform(get("/api/v1/nutrition/consumption").param("date", "2026-07-19"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dayType").value("REST"))
        .andExpect(jsonPath("$.target.kcal").value(restTemplate.targetCalories()));
  }

  @Test
  void consumptionFailsSafeWithNullTargetAndComparisonWhenNoneResolvable() throws Exception {
    // Fail-safe path (spec FOR-128 edge case): the service reports no resolvable template for the
    // day type (should not happen for the closed/always-seeded enum) -> controller must not crash.
    MealLogEntry entry =
        new MealLogEntry(
            LocalDate.of(2026, 7, 15),
            MealType.LUNCH,
            "Pollo (pechuga)",
            "chicken",
            new NutritionTotals(600, 40.0, 60.0, 20.0));
    when(mealLogService.consumption(eq(LocalDate.of(2026, 7, 15))))
        .thenReturn(
            new DayConsumption(
                LocalDate.of(2026, 7, 15),
                NutritionDayType.STRENGTH,
                new NutritionTotals(600, 40.0, 60.0, 20.0),
                KeyNutrientTotals.zero(),
                null,
                null,
                List.of(new StoredMealLogEntry("entry-1", entry))));

    mockMvc
        .perform(get("/api/v1/nutrition/consumption").param("date", "2026-07-15"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.consumed.kcal").value(600))
        .andExpect(jsonPath("$.target").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.comparison").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.entries[0].id").value("entry-1"));
  }

  @Test
  void consumptionWithAMissingDateParamReturnsValidationError() throws Exception {
    mockMvc
        .perform(get("/api/v1/nutrition/consumption"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void consumptionWithAMalformedDateParamReturnsValidationError() throws Exception {
    mockMvc
        .perform(get("/api/v1/nutrition/consumption").param("date", "not-a-date"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }
}
