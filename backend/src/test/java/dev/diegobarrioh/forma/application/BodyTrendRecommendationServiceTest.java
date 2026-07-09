package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.diegobarrioh.forma.domain.Recommendation;
import dev.diegobarrioh.forma.domain.RecommendationCategory;
import dev.diegobarrioh.forma.domain.RecommendationSeverity;
import dev.diegobarrioh.forma.domain.WeeklyBodySummary;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BodyTrendRecommendationService} (FOR-42): it reads the FOR-21 summary,
 * evaluates the body-trend rules and stamps recommendations from the injected clock.
 */
class BodyTrendRecommendationServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");
  private static final Clock FIXED = Clock.fixed(NOW, ZoneOffset.UTC);

  private final WeeklyBodySummaryService bodySummaryService = mock(WeeklyBodySummaryService.class);
  private final BodyTrendRecommendationService service =
      new BodyTrendRecommendationService(bodySummaryService, FIXED);

  @Test
  void producesPositiveRecommendationForAStableFatLossWeek() {
    when(bodySummaryService.currentSummary())
        .thenReturn(new WeeklyBodySummary(70.0, 18.0, 55.0, -0.2, -0.3, 7, "ok"));

    List<Recommendation> recs = service.currentRecommendations();

    assertThat(recs).hasSize(1);
    Recommendation rec = recs.get(0);
    assertThat(rec.category()).isEqualTo(RecommendationCategory.BODY);
    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.INFO);
    assertThat(rec.createdAt()).isEqualTo(NOW);
    assertThat(rec.message()).isNotBlank();
    assertThat(rec.reason()).isNotBlank();
  }

  @Test
  void producesInsufficientDataWhenSummaryHasNoTrend() {
    when(bodySummaryService.currentSummary())
        .thenReturn(new WeeklyBodySummary(70.0, 18.0, 55.0, null, null, null, "one measurement"));

    List<Recommendation> recs = service.currentRecommendations();

    assertThat(recs).hasSize(1);
    assertThat(recs.get(0).reason()).contains("dos mediciones");
  }
}
