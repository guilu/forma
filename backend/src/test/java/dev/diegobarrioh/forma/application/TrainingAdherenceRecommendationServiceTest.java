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
 * Unit tests for {@link TrainingAdherenceRecommendationService} (FOR-43): it reads the FOR-40
 * check-in, evaluates the adherence rules and stamps recommendations from the injected clock.
 */
class TrainingAdherenceRecommendationServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");
  private static final Clock FIXED = Clock.fixed(NOW, ZoneOffset.UTC);
  private static final LocalDate WEEK = LocalDate.of(2026, 7, 6);

  private final WeeklyCheckInService checkInService = mock(WeeklyCheckInService.class);
  private final TrainingAdherenceRecommendationService service =
      new TrainingAdherenceRecommendationService(checkInService, FIXED);

  @Test
  void producesHighAdherenceRecommendationForAStrongWeek() {
    when(checkInService.currentCheckIn())
        .thenReturn(new WeeklyCheckIn(WEEK, 70.0, 18.0, 55.0, 3, 3, 3, 3, null));

    List<Recommendation> recs = service.currentRecommendations();

    assertThat(recs).hasSize(1);
    Recommendation rec = recs.get(0);
    assertThat(rec.category()).isEqualTo(RecommendationCategory.TRAINING);
    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.INFO);
    assertThat(rec.createdAt()).isEqualTo(NOW);
    assertThat(rec.message()).isNotBlank();
    assertThat(rec.reason()).isNotBlank();
  }

  @Test
  void producesSafeResultWhenNoTrainingPlanned() {
    when(checkInService.currentCheckIn())
        .thenReturn(new WeeklyCheckIn(WEEK, 70.0, 18.0, 55.0, 0, 0, 0, 0, null));

    List<Recommendation> recs = service.currentRecommendations();

    assertThat(recs).hasSize(1);
    assertThat(recs.get(0).message()).contains("No hay entrenamientos planificados");
  }
}
