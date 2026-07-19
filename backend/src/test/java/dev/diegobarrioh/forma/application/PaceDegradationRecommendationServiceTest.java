package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.diegobarrioh.forma.domain.Recommendation;
import dev.diegobarrioh.forma.domain.RecommendationCategory;
import dev.diegobarrioh.forma.domain.WeeklyTrackingRecord;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PaceDegradationRecommendationService} (FOR-150 rule 4): reads the FOR-155
 * {@link WeeklyTrackingRecordService} history and delegates to the pure {@link
 * dev.diegobarrioh.forma.domain.PaceDegradationRules} evaluator.
 */
class PaceDegradationRecommendationServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");
  private static final Clock FIXED = Clock.fixed(NOW, ZoneOffset.UTC);

  private final WeeklyTrackingRecordService trackingRecordService =
      mock(WeeklyTrackingRecordService.class);
  private final PaceDegradationRecommendationService service =
      new PaceDegradationRecommendationService(trackingRecordService, FIXED);

  private static WeeklyTrackingRecord record(int week, String pace) {
    return new WeeklyTrackingRecord(
        week, LocalDate.of(2026, 1, 1).plusWeeks(week), null, null, null, null, pace, null, null);
  }

  @Test
  void producesAWarningWhenPaceSlowsDownWeekOverWeek() {
    when(trackingRecordService.list()).thenReturn(List.of(record(2, "5:45"), record(1, "5:30")));

    List<Recommendation> recs = service.currentRecommendations();

    assertThat(recs).hasSize(1);
    assertThat(recs.get(0).category()).isEqualTo(RecommendationCategory.RECOVERY);
    assertThat(recs.get(0).createdAt()).isEqualTo(NOW);
  }

  @Test
  void producesNothingWhenPaceDoesNotDegrade() {
    when(trackingRecordService.list()).thenReturn(List.of(record(2, "5:30"), record(1, "5:45")));

    assertThat(service.currentRecommendations()).isEmpty();
  }
}
