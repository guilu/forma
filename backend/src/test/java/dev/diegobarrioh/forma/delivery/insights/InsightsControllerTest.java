package dev.diegobarrioh.forma.delivery.insights;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.WeeklyInsights;
import dev.diegobarrioh.forma.application.WeeklyInsightsService;
import dev.diegobarrioh.forma.domain.Recommendation;
import dev.diegobarrioh.forma.domain.RecommendationCategory;
import dev.diegobarrioh.forma.domain.RecommendationSeverity;
import dev.diegobarrioh.forma.domain.WeeklyCheckIn;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link InsightsController} (FOR-45): the weekly response shape and the
 * empty-data path (still {@code 200} with an insufficient-data main recommendation).
 */
@WebMvcTest(InsightsController.class)
class InsightsControllerTest {

  private static final Instant NOW = Instant.parse("2026-07-10T08:00:00Z");
  private static final LocalDate WEEK = LocalDate.of(2026, 7, 6);

  @Autowired private MockMvc mockMvc;
  @MockBean private WeeklyInsightsService insightsService;

  @Test
  void returnsCheckInMainAndSecondaryRecommendations() throws Exception {
    WeeklyCheckIn checkIn = new WeeklyCheckIn(WEEK, 70.0, 18.0, 55.0, 3, 3, 3, 2, null);
    Recommendation main =
        new Recommendation(
            NOW,
            RecommendationCategory.BODY,
            RecommendationSeverity.ACTION,
            "El peso baja rápido; revisa las calorías.",
            "El peso baja 1.5 kg en 7 días.",
            "weeklyWeightChangeKg");
    Recommendation secondary =
        new Recommendation(
            NOW,
            RecommendationCategory.TRAINING,
            RecommendationSeverity.INFO,
            "Semana muy constante; mantén este ritmo.",
            "Se completaron 5 de 6 sesiones planificadas.",
            null);
    when(insightsService.currentInsights())
        .thenReturn(new WeeklyInsights(checkIn, main, List.of(secondary), NOW));

    mockMvc
        .perform(get("/api/v1/insights/weekly"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.checkIn.weekStartDate").value("2026-07-06"))
        .andExpect(jsonPath("$.checkIn.plannedRunningSessions").value(3))
        .andExpect(jsonPath("$.main.category").value("BODY"))
        .andExpect(jsonPath("$.main.severity").value("ACTION"))
        .andExpect(jsonPath("$.main.message").value("El peso baja rápido; revisa las calorías."))
        .andExpect(jsonPath("$.main.reason").value("El peso baja 1.5 kg en 7 días."))
        .andExpect(jsonPath("$.main.relatedMetric").value("weeklyWeightChangeKg"))
        .andExpect(jsonPath("$.secondary[0].severity").value("INFO"))
        .andExpect(jsonPath("$.secondary[0].relatedMetric").doesNotExist())
        .andExpect(jsonPath("$.generatedAt").exists());
  }

  @Test
  void returnsOkWithInsufficientDataMainWhenNoData() throws Exception {
    WeeklyCheckIn checkIn = new WeeklyCheckIn(WEEK, null, null, null, 0, 0, 0, 0, null);
    Recommendation main =
        new Recommendation(
            NOW,
            RecommendationCategory.BODY,
            RecommendationSeverity.INFO,
            "Registra otra medición para analizar tu tendencia corporal.",
            "Hacen falta al menos dos mediciones para comparar la evolución.",
            null);
    when(insightsService.currentInsights())
        .thenReturn(new WeeklyInsights(checkIn, main, List.of(), NOW));

    mockMvc
        .perform(get("/api/v1/insights/weekly"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.main.severity").value("INFO"))
        .andExpect(
            jsonPath("$.main.message")
                .value("Registra otra medición para analizar tu tendencia corporal."))
        .andExpect(jsonPath("$.checkIn.latestWeightKg").doesNotExist())
        .andExpect(jsonPath("$.secondary").isEmpty());
  }
}
