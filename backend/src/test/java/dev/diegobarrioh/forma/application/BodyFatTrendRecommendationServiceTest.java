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
 * Unit tests for {@link BodyFatTrendRecommendationService} (FOR-150 rule 2): reads the FOR-155
 * {@link WeeklyTrackingRecordService} history and delegates to the pure {@link
 * dev.diegobarrioh.forma.domain.BodyFatTrendRules} evaluator.
 */
class BodyFatTrendRecommendationServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");
  private static final Clock FIXED = Clock.fixed(NOW, ZoneOffset.UTC);

  private final WeeklyTrackingRecordService trackingRecordService =
      mock(WeeklyTrackingRecordService.class);
  private final BodyFatTrendRecommendationService service =
      new BodyFatTrendRecommendationService(trackingRecordService, FIXED);

  private static WeeklyTrackingRecord record(int week, Double bodyFatPercentage) {
    return new WeeklyTrackingRecord(
        week,
        LocalDate.of(2026, 1, 1).plusWeeks(week),
        null,
        bodyFatPercentage,
        null,
        null,
        null,
        null,
        null);
  }

  @Test
  void producesARecommendationWhenTheHistoryShowsASustainedRise() {
    when(trackingRecordService.list())
        .thenReturn(List.of(record(3, 20.0), record(2, 19.0), record(1, 18.0)));

    List<Recommendation> recs = service.currentRecommendations();

    assertThat(recs).hasSize(1);
    assertThat(recs.get(0).category()).isEqualTo(RecommendationCategory.BODY);
    assertThat(recs.get(0).createdAt()).isEqualTo(NOW);
  }

  @Test
  void producesNothingWhenThereIsNoSustainedTrend() {
    when(trackingRecordService.list()).thenReturn(List.of(record(1, 18.0)));

    assertThat(service.currentRecommendations()).isEmpty();
  }
}
