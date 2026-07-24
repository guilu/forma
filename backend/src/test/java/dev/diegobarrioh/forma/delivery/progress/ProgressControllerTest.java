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
import dev.diegobarrioh.forma.application.StreakService;
import dev.diegobarrioh.forma.application.ValidationException;
import dev.diegobarrioh.forma.application.WeeklyHistory;
import dev.diegobarrioh.forma.application.WeeklyHistoryService;
import dev.diegobarrioh.forma.domain.AdherenceCategory;
import dev.diegobarrioh.forma.domain.CategoryAdherence;
import dev.diegobarrioh.forma.domain.Streak;
import dev.diegobarrioh.forma.domain.WeeklyHistoryBucket;
import dev.diegobarrioh.forma.support.WebMvcAuthTestConfig;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link ProgressController} (FOR-129 adherence, FOR-135 achievements, FOR-139
 * streak + weekly-history): routing and response shape per {@code specs/FOR-129/api.md} / {@code
 * specs/FOR-135/api.md} / {@code specs/FOR-139/api.md}. Services are mocked, like {@code
 * MealLogControllerTest} (FOR-127).
 */
@WebMvcTest(ProgressController.class)
@Import(WebMvcAuthTestConfig.class)
class ProgressControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private AdherenceService service;
  @MockBean private AchievementService achievementService;
  @MockBean private StreakService streakService;
  @MockBean private WeeklyHistoryService weeklyHistoryService;

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

  @Test
  void defaultStreakRequestUsesA90DayWindowAndReturnsTheResponseShapeFromApiMd() throws Exception {
    when(streakService.compute(90)).thenReturn(new Streak(6, 21, LocalDate.of(2026, 7, 18)));

    mockMvc
        .perform(get("/api/v1/progress/streak"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentStreakDays").value(6))
        .andExpect(jsonPath("$.longestStreakDays").value(21))
        .andExpect(jsonPath("$.asOf").value("2026-07-18"));

    verify(streakService).compute(90);
  }

  @Test
  void explicitStreakDaysParamIsForwardedToTheService() throws Exception {
    when(streakService.compute(7)).thenReturn(new Streak(0, 0, LocalDate.of(2026, 7, 18)));

    mockMvc.perform(get("/api/v1/progress/streak").param("days", "7")).andExpect(status().isOk());

    verify(streakService).compute(eq(7));
  }

  @Test
  void aStreakDaysValueOutOfRangeReturns400ValidationErrorViaTheApplicationLayer()
      throws Exception {
    when(streakService.compute(0))
        .thenThrow(new ValidationException("days must be between 1 and 365"));

    mockMvc
        .perform(get("/api/v1/progress/streak").param("days", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void streakOnEmptyHistoryReturns200WithAZeroedStreakNeverA404() throws Exception {
    when(streakService.compute(90)).thenReturn(new Streak(0, 0, LocalDate.of(2026, 7, 18)));

    mockMvc
        .perform(get("/api/v1/progress/streak"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentStreakDays").value(0))
        .andExpect(jsonPath("$.longestStreakDays").value(0));
  }

  @Test
  void defaultWeeklyHistoryRequestUsesAnEightWeekWindowAndReturnsTheResponseShapeFromApiMd()
      throws Exception {
    when(weeklyHistoryService.compute(8))
        .thenReturn(
            new WeeklyHistory(
                List.of(
                    new WeeklyHistoryBucket(LocalDate.of(2026, 5, 25), 7, 5),
                    new WeeklyHistoryBucket(LocalDate.of(2026, 6, 1), 7, 7))));

    mockMvc
        .perform(get("/api/v1/progress/weekly-history"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.weeks[0].weekStart").value("2026-05-25"))
        .andExpect(jsonPath("$.weeks[0].planned").value(7))
        .andExpect(jsonPath("$.weeks[0].completed").value(5))
        .andExpect(jsonPath("$.weeks[1].weekStart").value("2026-06-01"));

    verify(weeklyHistoryService).compute(8);
  }

  @Test
  void explicitWeeksParamIsForwardedToTheService() throws Exception {
    when(weeklyHistoryService.compute(4)).thenReturn(new WeeklyHistory(List.of()));

    mockMvc
        .perform(get("/api/v1/progress/weekly-history").param("weeks", "4"))
        .andExpect(status().isOk());

    verify(weeklyHistoryService).compute(eq(4));
  }

  @Test
  void aWeeksValueOutOfRangeReturns400ValidationErrorViaTheApplicationLayer() throws Exception {
    when(weeklyHistoryService.compute(0))
        .thenThrow(new ValidationException("weeks must be between 1 and 52"));

    mockMvc
        .perform(get("/api/v1/progress/weekly-history").param("weeks", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void weeklyHistoryOnEmptyDataReturns200WithZeroBucketsStillPresentNeverA404() throws Exception {
    when(weeklyHistoryService.compute(8))
        .thenReturn(
            new WeeklyHistory(List.of(new WeeklyHistoryBucket(LocalDate.of(2026, 7, 13), 7, 0))));

    mockMvc
        .perform(get("/api/v1/progress/weekly-history"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.weeks.length()").value(1))
        .andExpect(jsonPath("$.weeks[0].completed").value(0));
  }
}
