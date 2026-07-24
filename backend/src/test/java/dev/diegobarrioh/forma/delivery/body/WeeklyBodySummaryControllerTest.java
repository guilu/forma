package dev.diegobarrioh.forma.delivery.body;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.WeeklyBodySummaryService;
import dev.diegobarrioh.forma.domain.WeeklyBodySummary;
import dev.diegobarrioh.forma.support.WebMvcAuthTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link WeeklyBodySummaryController} (FOR-97). Mocks the FOR-21 {@link
 * WeeklyBodySummaryService} so the test focuses on the delivery contract: routing, the response DTO
 * shape and the null-delta honesty rules (following BodyMeasurementControllerTest / ADR-007).
 */
@WebMvcTest(WeeklyBodySummaryController.class)
@Import(WebMvcAuthTestConfig.class)
class WeeklyBodySummaryControllerTest {

  private static final String PATH = "/api/v1/body/weekly-summary";

  @Autowired private MockMvc mockMvc;
  @MockBean private WeeklyBodySummaryService service;

  @Test
  void returnsLatestValuesAndDeltasForAPopulatedWeek() throws Exception {
    when(service.currentSummary())
        .thenReturn(
            new WeeklyBodySummary(
                73.6,
                14.7,
                62.8,
                -0.3,
                -0.6,
                7,
                "Peso 73.6 kg (-0.3 kg en 7 días). Grasa corporal 14.7% (-0.6%). Masa magra 62.8"
                    + " kg."));

    mockMvc
        .perform(get(PATH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.latestWeightKg").value(73.6))
        .andExpect(jsonPath("$.latestBodyFatPercentage").value(14.7))
        .andExpect(jsonPath("$.latestLeanMassKg").value(62.8))
        .andExpect(jsonPath("$.weeklyWeightChangeKg").value(-0.3))
        .andExpect(jsonPath("$.weeklyBodyFatChange").value(-0.6))
        .andExpect(jsonPath("$.comparisonDays").value(7))
        .andExpect(
            jsonPath("$.message")
                .value(
                    "Peso 73.6 kg (-0.3 kg en 7 días). Grasa corporal 14.7% (-0.6%). Masa magra"
                        + " 62.8 kg."));
  }

  @Test
  void omitsDeltasAndComparisonDaysWithASingleMeasurement() throws Exception {
    when(service.currentSummary())
        .thenReturn(
            new WeeklyBodySummary(
                73.6,
                14.7,
                62.8,
                null,
                null,
                null,
                "Última medición — Peso 73.6 kg, grasa 14.7%, masa magra 62.8 kg. Registra otra"
                    + " medición para ver el cambio."));

    mockMvc
        .perform(get(PATH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.latestWeightKg").value(73.6))
        .andExpect(jsonPath("$.weeklyWeightChangeKg").doesNotExist())
        .andExpect(jsonPath("$.weeklyBodyFatChange").doesNotExist())
        .andExpect(jsonPath("$.comparisonDays").doesNotExist());
  }

  @Test
  void omitsAllNumericFieldsWhenThereAreNoMeasurements() throws Exception {
    when(service.currentSummary())
        .thenReturn(
            new WeeklyBodySummary(
                null, null, null, null, null, null, "Aún no hay mediciones para resumir."));

    mockMvc
        .perform(get(PATH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.latestWeightKg").doesNotExist())
        .andExpect(jsonPath("$.latestBodyFatPercentage").doesNotExist())
        .andExpect(jsonPath("$.latestLeanMassKg").doesNotExist())
        .andExpect(jsonPath("$.weeklyWeightChangeKg").doesNotExist())
        .andExpect(jsonPath("$.weeklyBodyFatChange").doesNotExist())
        .andExpect(jsonPath("$.comparisonDays").doesNotExist())
        .andExpect(jsonPath("$.message").value("Aún no hay mediciones para resumir."));
  }
}
