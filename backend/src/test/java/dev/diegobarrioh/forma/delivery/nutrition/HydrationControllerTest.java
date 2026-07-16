package dev.diegobarrioh.forma.delivery.nutrition;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.HydrationProgress;
import dev.diegobarrioh.forma.application.HydrationService;
import dev.diegobarrioh.forma.application.MealLogService;
import dev.diegobarrioh.forma.application.NutritionCalculationService;
import dev.diegobarrioh.forma.application.NutritionDayCatalogService;
import dev.diegobarrioh.forma.application.StoredWaterIntakeEntry;
import dev.diegobarrioh.forma.application.ValidationException;
import dev.diegobarrioh.forma.domain.WaterIntakeEntry;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link NutritionController}'s FOR-130 endpoints ({@code POST
 * /nutrition/hydration}, {@code GET /nutrition/hydration}): routing, request validation and
 * response shape per {@code specs/FOR-130/api.md} and {@code tests.md}. {@link HydrationService} is
 * mocked, mirroring {@code MealLogControllerTest} (FOR-127).
 */
@WebMvcTest(NutritionController.class)
class HydrationControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private HydrationService hydrationService;
  @MockBean private MealLogService mealLogService;
  @MockBean private NutritionDayCatalogService nutritionDayCatalogService;
  @MockBean private NutritionCalculationService nutritionCalculationService;

  @Test
  void logsAVolumeAndReturns201WithTheStoredEntry() throws Exception {
    WaterIntakeEntry entry = new WaterIntakeEntry(LocalDate.of(2026, 7, 16), 500.0);
    when(hydrationService.log(any())).thenReturn(new StoredWaterIntakeEntry("entry-1", entry));

    mockMvc
        .perform(
            post("/api/v1/nutrition/hydration")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2026-07-16\",\"volumeMl\":500}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("entry-1"))
        .andExpect(jsonPath("$.date").value("2026-07-16"))
        .andExpect(jsonPath("$.volumeMl").value(500.0));
  }

  @Test
  void rejectsAZeroVolumeWithValidationError() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/nutrition/hydration")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2026-07-16\",\"volumeMl\":0}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void rejectsANegativeVolumeWithValidationError() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/nutrition/hydration")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2026-07-16\",\"volumeMl\":-50}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void rejectsAMissingDateWithValidationError() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/nutrition/hydration")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"volumeMl\":500}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void rejectsAFarFutureDateViaTheApplicationLayer() throws Exception {
    when(hydrationService.log(any()))
        .thenThrow(new ValidationException("date must not be in the far future"));

    mockMvc
        .perform(
            post("/api/v1/nutrition/hydration")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2099-07-16\",\"volumeMl\":500}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void hydrationBeforeAnyLogReturns200WithZeroedTotalNeverA404() throws Exception {
    when(hydrationService.hydrationProgress(eq(LocalDate.of(2026, 7, 16))))
        .thenReturn(new HydrationProgress(LocalDate.of(2026, 7, 16), 0.0, 2000.0, 0.0, List.of()));

    mockMvc
        .perform(get("/api/v1/nutrition/hydration").param("date", "2026-07-16"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalMl").value(0.0))
        .andExpect(jsonPath("$.goalMl").value(2000.0))
        .andExpect(jsonPath("$.progress").value(0.0));
  }

  @Test
  void hydrationWithLoggedEntriesReturnsTotalGoalAndProgress() throws Exception {
    when(hydrationService.hydrationProgress(eq(LocalDate.of(2026, 7, 16))))
        .thenReturn(
            new HydrationProgress(LocalDate.of(2026, 7, 16), 1500.0, 2000.0, 0.75, List.of()));

    mockMvc
        .perform(get("/api/v1/nutrition/hydration").param("date", "2026-07-16"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.date").value("2026-07-16"))
        .andExpect(jsonPath("$.totalMl").value(1500.0))
        .andExpect(jsonPath("$.goalMl").value(2000.0))
        .andExpect(jsonPath("$.progress").value(0.75));
  }

  @Test
  void hydrationWithAnUnresolvableGoalReturnsNullGoalAndProgress() throws Exception {
    when(hydrationService.hydrationProgress(eq(LocalDate.of(2026, 7, 16))))
        .thenReturn(new HydrationProgress(LocalDate.of(2026, 7, 16), 500.0, null, null, List.of()));

    mockMvc
        .perform(get("/api/v1/nutrition/hydration").param("date", "2026-07-16"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalMl").value(500.0))
        .andExpect(jsonPath("$.goalMl").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.progress").value(org.hamcrest.Matchers.nullValue()));
  }

  @Test
  void hydrationWithAMissingDateParamReturnsValidationError() throws Exception {
    mockMvc
        .perform(get("/api/v1/nutrition/hydration"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void hydrationWithAMalformedDateParamReturnsValidationError() throws Exception {
    mockMvc
        .perform(get("/api/v1/nutrition/hydration").param("date", "not-a-date"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }
}
