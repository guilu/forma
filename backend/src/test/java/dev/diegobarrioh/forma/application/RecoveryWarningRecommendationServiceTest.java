package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.diegobarrioh.forma.domain.Recommendation;
import dev.diegobarrioh.forma.domain.RecommendationCategory;
import dev.diegobarrioh.forma.domain.RecommendationSeverity;
import dev.diegobarrioh.forma.domain.WeeklyBodySummary;
import dev.diegobarrioh.forma.domain.WeeklyCheckIn;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RecoveryWarningRecommendationService} (FOR-44): it combines the FOR-40
 * check-in and FOR-21 body summary, evaluates the recovery rules and stamps any warning from the
 * injected clock.
 */
class RecoveryWarningRecommendationServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");
  private static final Clock FIXED = Clock.fixed(NOW, ZoneOffset.UTC);
  private static final LocalDate WEEK = LocalDate.of(2026, 7, 6);

  private final WeeklyCheckInService checkInService = mock(WeeklyCheckInService.class);
  private final WeeklyBodySummaryService bodySummaryService = mock(WeeklyBodySummaryService.class);
  private final RecoveryWarningRecommendationService service =
      new RecoveryWarningRecommendationService(checkInService, bodySummaryService, FIXED);

  @Test
  void warnsOnAFatiguedWeek() {
    when(checkInService.currentCheckIn())
        .thenReturn(new WeeklyCheckIn(WEEK, 70.0, 18.0, 55.0, 3, 1, 3, 0, null));
    when(bodySummaryService.currentSummary())
        .thenReturn(new WeeklyBodySummary(70.0, 18.0, 55.0, -0.1, 0.4, 7, "x"));

    List<Recommendation> recs = service.currentRecommendations();

    assertThat(recs).hasSize(1);
    Recommendation rec = recs.get(0);
    assertThat(rec.category()).isEqualTo(RecommendationCategory.RECOVERY);
    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.WARNING);
    assertThat(rec.createdAt()).isEqualTo(NOW);
    assertThat(rec.reason()).isNotBlank();
  }

  @Test
  void doesNotWarnOnAHealthyWeek() {
    when(checkInService.currentCheckIn())
        .thenReturn(new WeeklyCheckIn(WEEK, 70.0, 18.0, 55.0, 3, 3, 3, 3, null));
    when(bodySummaryService.currentSummary())
        .thenReturn(new WeeklyBodySummary(70.0, 18.0, 55.0, -0.1, -0.2, 7, "x"));

    assertThat(service.currentRecommendations()).isEmpty();
  }
}
