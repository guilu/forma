package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.diegobarrioh.forma.domain.Recommendation;
import dev.diegobarrioh.forma.domain.RecommendationCategory;
import dev.diegobarrioh.forma.domain.RecommendationSeverity;
import dev.diegobarrioh.forma.domain.WeeklyCheckIn;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WeeklyInsightsService} (FOR-45): check-in assembly, priority-based main
 * selection ({@code ACTION > WARNING > INFO}) and the empty-data path.
 */
class WeeklyInsightsServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-10T08:00:00Z");
  private static final Clock FIXED = Clock.fixed(NOW, ZoneOffset.UTC);
  private static final LocalDate WEEK = LocalDate.of(2026, 7, 6);

  private final WeeklyCheckInService checkInService = mock(WeeklyCheckInService.class);
  private final BodyTrendRecommendationService bodyTrendService =
      mock(BodyTrendRecommendationService.class);
  private final TrainingAdherenceRecommendationService trainingAdherenceService =
      mock(TrainingAdherenceRecommendationService.class);
  private final RecoveryWarningRecommendationService recoveryWarningService =
      mock(RecoveryWarningRecommendationService.class);

  private final WeeklyInsightsService service =
      new WeeklyInsightsService(
          checkInService,
          bodyTrendService,
          trainingAdherenceService,
          recoveryWarningService,
          FIXED);

  private static Recommendation rec(
      RecommendationCategory category, RecommendationSeverity severity) {
    return new Recommendation(NOW, category, severity, "message", "reason", null);
  }

  private void checkIn() {
    when(checkInService.currentCheckIn())
        .thenReturn(new WeeklyCheckIn(WEEK, 70.0, 18.0, 55.0, 3, 3, 3, 2, null));
  }

  @Test
  void selectsHighestSeverityAsMainAndOrdersSecondariesByPriority() {
    checkIn();
    when(bodyTrendService.currentRecommendations())
        .thenReturn(List.of(rec(RecommendationCategory.BODY, RecommendationSeverity.ACTION)));
    when(trainingAdherenceService.currentRecommendations())
        .thenReturn(List.of(rec(RecommendationCategory.TRAINING, RecommendationSeverity.INFO)));
    when(recoveryWarningService.currentRecommendations())
        .thenReturn(List.of(rec(RecommendationCategory.RECOVERY, RecommendationSeverity.WARNING)));

    WeeklyInsights insights = service.currentInsights();

    assertThat(insights.checkIn().weekStartDate()).isEqualTo(WEEK);
    assertThat(insights.main().severity()).isEqualTo(RecommendationSeverity.ACTION);
    assertThat(insights.secondary())
        .extracting(Recommendation::severity)
        .containsExactly(RecommendationSeverity.WARNING, RecommendationSeverity.INFO);
    assertThat(insights.generatedAt()).isEqualTo(NOW);
  }

  @Test
  void tiesKeepStableProductionOrderBodyThenTraining() {
    checkIn();
    when(bodyTrendService.currentRecommendations())
        .thenReturn(List.of(rec(RecommendationCategory.BODY, RecommendationSeverity.INFO)));
    when(trainingAdherenceService.currentRecommendations())
        .thenReturn(List.of(rec(RecommendationCategory.TRAINING, RecommendationSeverity.INFO)));
    when(recoveryWarningService.currentRecommendations()).thenReturn(List.of());

    WeeklyInsights insights = service.currentInsights();

    assertThat(insights.main().category()).isEqualTo(RecommendationCategory.BODY);
    assertThat(insights.secondary())
        .extracting(Recommendation::category)
        .containsExactly(RecommendationCategory.TRAINING);
  }

  @Test
  void emptyDataStillYieldsAValidResultWithInsufficientDataMain() {
    checkIn();
    Recommendation insufficient =
        new Recommendation(
            NOW,
            RecommendationCategory.BODY,
            RecommendationSeverity.INFO,
            "Registra otra medición para analizar tu tendencia corporal.",
            "Hacen falta al menos dos mediciones para comparar la evolución.",
            null);
    when(bodyTrendService.currentRecommendations()).thenReturn(List.of(insufficient));
    when(trainingAdherenceService.currentRecommendations())
        .thenReturn(List.of(rec(RecommendationCategory.TRAINING, RecommendationSeverity.INFO)));
    when(recoveryWarningService.currentRecommendations()).thenReturn(List.of());

    WeeklyInsights insights = service.currentInsights();

    assertThat(insights.main().message()).contains("Registra otra medición");
    assertThat(insights.secondary()).hasSize(1);
  }
}
