package dev.diegobarrioh.forma.delivery.progress;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.AchievementService;
import dev.diegobarrioh.forma.application.AchievementView;
import dev.diegobarrioh.forma.application.AchievementsView;
import dev.diegobarrioh.forma.application.Adherence;
import dev.diegobarrioh.forma.application.AdherenceService;
import dev.diegobarrioh.forma.application.ValidationException;
import dev.diegobarrioh.forma.domain.AdherenceCategory;
import dev.diegobarrioh.forma.domain.CategoryAdherence;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link ProgressController} (FOR-129 adherence + FOR-135 achievements):
 * routing and response shape per {@code specs/FOR-129/api.md} / {@code specs/FOR-135/api.md}.
 * {@link AdherenceService}/{@link AchievementService} are mocked, like {@code
 * MealLogControllerTest} (FOR-127).
 */
@WebMvcTest(ProgressController.class)
class ProgressControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private AdherenceService service;
  @MockBean private AchievementService achievementService;

  private static Adherence adherenceView() {
    return new Adherence(
        30,
        LocalDate.of(2026, 6, 17),
        LocalDate.of(2026, 7, 16),
        List.of(
            CategoryAdherence.of(AdherenceCategory.TRAINING, 20, 17),
            CategoryAdherence.of(AdherenceCategory.NUTRITION, 30, 24),
            CategoryAdherence.of(AdherenceCategory.MEASUREMENTS, 4, 4)));
  }

  @Test
  void defaultRequestUsesA30DayWindowAndReturnsTheResponseShapeFromApiMd() throws Exception {
    when(service.compute(30)).thenReturn(adherenceView());

    mockMvc
        .perform(get("/api/v1/progress/adherence"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.windowDays").value(30))
        .andExpect(jsonPath("$.from").value("2026-06-17"))
        .andExpect(jsonPath("$.to").value("2026-07-16"))
        .andExpect(jsonPath("$.categories[0].category").value("TRAINING"))
        .andExpect(jsonPath("$.categories[0].planned").value(20))
        .andExpect(jsonPath("$.categories[0].completed").value(17))
        .andExpect(jsonPath("$.categories[0].rate").value(0.85))
        .andExpect(jsonPath("$.categories[1].category").value("NUTRITION"))
        .andExpect(jsonPath("$.categories[2].category").value("MEASUREMENTS"))
        .andExpect(jsonPath("$.categories[2].rate").value(1.0));

    verify(service).compute(30);
  }

  @Test
  void explicitDaysParamIsForwardedToTheService() throws Exception {
    when(service.compute(7))
        .thenReturn(
            new Adherence(
                7,
                LocalDate.of(2026, 7, 10),
                LocalDate.of(2026, 7, 16),
                List.of(
                    CategoryAdherence.of(AdherenceCategory.TRAINING, 0, 0),
                    CategoryAdherence.of(AdherenceCategory.NUTRITION, 7, 0),
                    CategoryAdherence.of(AdherenceCategory.MEASUREMENTS, 1, 0))));

    mockMvc
        .perform(get("/api/v1/progress/adherence").param("days", "7"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.windowDays").value(7))
        .andExpect(jsonPath("$.categories[0].rate").doesNotExist());

    verify(service).compute(eq(7));
  }

  @Test
  void aDaysValueOutOfRangeReturns400ValidationErrorViaTheApplicationLayer() throws Exception {
    when(service.compute(0)).thenThrow(new ValidationException("days must be between 1 and 365"));

    mockMvc
        .perform(get("/api/v1/progress/adherence").param("days", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void aNonNumericDaysValueReturns400ValidationError() throws Exception {
    mockMvc
        .perform(get("/api/v1/progress/adherence").param("days", "not-a-number"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void emptyDataReturns200WithZeroedCategoriesNeverA404() throws Exception {
    when(service.compute(30))
        .thenReturn(
            new Adherence(
                30,
                LocalDate.of(2026, 6, 17),
                LocalDate.of(2026, 7, 16),
                List.of(
                    CategoryAdherence.of(AdherenceCategory.TRAINING, 0, 0),
                    CategoryAdherence.of(AdherenceCategory.NUTRITION, 30, 0),
                    CategoryAdherence.of(AdherenceCategory.MEASUREMENTS, 5, 0))));

    mockMvc
        .perform(get("/api/v1/progress/adherence"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categories[1].completed").value(0))
        .andExpect(jsonPath("$.categories[1].rate").value(0.0));
  }

  @Test
  void achievementsResponseShapeMatchesApiMdWithEarnedAndAvailable() throws Exception {
    when(achievementService.evaluate())
        .thenReturn(
            new AchievementsView(
                List.of(
                    new AchievementView(
                        "FIRST_MEASUREMENT",
                        "Primera medición",
                        "Registra tu primera medición corporal.",
                        Instant.parse("2026-07-10T08:00:00Z"))),
                List.of(
                    new AchievementView(
                        "TEN_MEASUREMENTS_LOGGED",
                        "10 mediciones registradas",
                        "Registra 10 mediciones corporales.",
                        null))));

    mockMvc
        .perform(get("/api/v1/progress/achievements"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.earned[0].id").value("FIRST_MEASUREMENT"))
        .andExpect(jsonPath("$.earned[0].title").value("Primera medición"))
        .andExpect(jsonPath("$.earned[0].earnedAt").value("2026-07-10T08:00:00Z"))
        .andExpect(jsonPath("$.available[0].id").value("TEN_MEASUREMENTS_LOGGED"))
        .andExpect(jsonPath("$.available[0].earnedAt").doesNotExist());

    verify(achievementService).evaluate();
  }

  @Test
  void achievementsOnEmptyDataReturns200WithEmptyEarnedAndTheFullAvailableCatalogNeverA404()
      throws Exception {
    when(achievementService.evaluate())
        .thenReturn(
            new AchievementsView(
                List.of(),
                List.of(
                    new AchievementView("FIRST_MEASUREMENT", "Primera medición", "…", null),
                    new AchievementView(
                        "FIRST_GOAL_CREATED", "Primer objetivo creado", "…", null))));

    mockMvc
        .perform(get("/api/v1/progress/achievements"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.earned").isEmpty())
        .andExpect(jsonPath("$.available.length()").value(2));
  }
}
