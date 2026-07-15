package dev.diegobarrioh.forma.delivery.nutrition;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.DayConsumption;
import dev.diegobarrioh.forma.application.MealLogService;
import dev.diegobarrioh.forma.application.NutritionCalculationService;
import dev.diegobarrioh.forma.application.NutritionDayCatalogService;
import dev.diegobarrioh.forma.application.StoredMealLogEntry;
import dev.diegobarrioh.forma.application.ValidationException;
import dev.diegobarrioh.forma.domain.MealLogEntry;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link NutritionController}'s FOR-127 endpoints ({@code POST /nutrition/log},
 * {@code GET /nutrition/consumption}): routing, request validation and response shape per {@code
 * specs/FOR-127/api.md} and {@code tests.md}. {@link MealLogService} is mocked, like {@code
 * GoalControllerTest} (FOR-125).
 */
@WebMvcTest(NutritionController.class)
class MealLogControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private MealLogService mealLogService;
  @MockBean private NutritionDayCatalogService nutritionDayCatalogService;
  @MockBean private NutritionCalculationService nutritionCalculationService;

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
    when(mealLogService.consumption(eq(LocalDate.of(2026, 7, 15))))
        .thenReturn(
            new DayConsumption(
                LocalDate.of(2026, 7, 15),
                new NutritionTotals(0, 0.0, 0.0, 0.0),
                null,
                null,
                List.of()));

    mockMvc
        .perform(get("/api/v1/nutrition/consumption").param("date", "2026-07-15"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.consumed.kcal").value(0))
        .andExpect(jsonPath("$.entries").isArray())
        .andExpect(jsonPath("$.entries").isEmpty());
  }

  @Test
  void consumptionReturnsConsumedTotalsAndEntriesAndNullTargetWhenNoneResolvable()
      throws Exception {
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
                new NutritionTotals(600, 40.0, 60.0, 20.0),
                null,
                null,
                List.of(new StoredMealLogEntry("entry-1", entry))));

    mockMvc
        .perform(get("/api/v1/nutrition/consumption").param("date", "2026-07-15"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.consumed.kcal").value(600))
        .andExpect(jsonPath("$.target").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.comparison").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.entries[0].id").value("entry-1"))
        .andExpect(jsonPath("$.entries[0].mealType").value("LUNCH"))
        .andExpect(jsonPath("$.entries[0].name").value("Pollo (pechuga)"))
        .andExpect(jsonPath("$.entries[0].kcal").value(600));
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
