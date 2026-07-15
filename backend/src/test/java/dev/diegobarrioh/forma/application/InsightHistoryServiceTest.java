package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.diegobarrioh.forma.domain.Recommendation;
import dev.diegobarrioh.forma.domain.RecommendationCategory;
import dev.diegobarrioh.forma.domain.RecommendationSeverity;
import dev.diegobarrioh.forma.domain.WeeklyCheckIn;
import dev.diegobarrioh.forma.domain.WeeklyCheckInDeltas;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link InsightHistoryService} (FOR-110): the history listing use case and the
 * week-over-week delta computation against the immediately prior persisted period.
 */
class InsightHistoryServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-13T08:00:00Z");
  private static final LocalDate CURRENT_WEEK = LocalDate.of(2026, 7, 13);
  private static final LocalDate PRIOR_WEEK = LocalDate.of(2026, 7, 6);

  private final InsightHistoryRepository repository = mock(InsightHistoryRepository.class);
  private final InsightHistoryService service = new InsightHistoryService(repository);

  private static Recommendation rec() {
    return new Recommendation(
        NOW, RecommendationCategory.BODY, RecommendationSeverity.INFO, "message", "reason", null);
  }

  @Test
  void historyDelegatesToRepositoryListAll() {
    WeeklyCheckIn checkIn = new WeeklyCheckIn(CURRENT_WEEK, 71.0, 17.5, 55.5, 3, 3, 3, 2, null);
    WeeklyInsights insights = new WeeklyInsights(checkIn, rec(), List.of(), NOW);
    when(repository.listAll()).thenReturn(List.of(insights));

    List<WeeklyInsights> history = service.history();

    assertThat(history).containsExactly(insights);
  }

  @Test
  void historyReturnsEmptyListBeforeAnyInsightsHaveBeenGenerated() {
    when(repository.listAll()).thenReturn(List.of());

    assertThat(service.history()).isEmpty();
  }

  @Test
  void deltasForComputesAgainstTheMostRecentPriorPersistedCheckIn() {
    WeeklyCheckIn current = new WeeklyCheckIn(CURRENT_WEEK, 71.0, 17.5, 55.5, 3, 3, 3, 2, null);
    WeeklyCheckIn prior = new WeeklyCheckIn(PRIOR_WEEK, 72.5, 18.0, 55.0, 3, 2, 3, 1, null);
    when(repository.findMostRecentCheckInBefore(CURRENT_WEEK)).thenReturn(Optional.of(prior));

    WeeklyCheckInDeltas deltas = service.deltasFor(current);

    assertThat(deltas.weightDeltaKg()).isEqualTo(-1.5);
    assertThat(deltas.trainingCompletionDelta()).isEqualTo(2);
  }

  @Test
  void deltasForReturnsNoneWhenThereIsNoPriorPeriod() {
    WeeklyCheckIn current = new WeeklyCheckIn(CURRENT_WEEK, 71.0, 17.5, 55.5, 3, 3, 3, 2, null);
    when(repository.findMostRecentCheckInBefore(CURRENT_WEEK)).thenReturn(Optional.empty());

    WeeklyCheckInDeltas deltas = service.deltasFor(current);

    assertThat(deltas).isEqualTo(WeeklyCheckInDeltas.NONE);
  }
}
