package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.diegobarrioh.forma.domain.WeeklyBodySummary;
import dev.diegobarrioh.forma.domain.WeeklyCheckIn;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WeeklyCheckInService} (FOR-40): it wires the FOR-21/FOR-28 services and
 * derives the current week's Monday from the injected clock.
 */
class WeeklyCheckInServiceTest {

  // A Wednesday; the week's Monday is 2026-07-06.
  private static final Clock WEDNESDAY =
      Clock.fixed(Instant.parse("2026-07-08T12:00:00Z"), ZoneOffset.UTC);

  private final WeeklyBodySummaryService bodySummaryService = mock(WeeklyBodySummaryService.class);
  private final WeeklyTrainingSummaryService trainingSummaryService =
      mock(WeeklyTrainingSummaryService.class);
  private final WeeklyCheckInService service =
      new WeeklyCheckInService(bodySummaryService, trainingSummaryService, WEDNESDAY);

  @Test
  void assemblesCurrentCheckInFromBothServicesForThisWeeksMonday() {
    when(bodySummaryService.currentSummary())
        .thenReturn(new WeeklyBodySummary(72.5, 18.0, 59.0, -0.3, -0.2, 7, "ok"));
    when(trainingSummaryService.currentSummary())
        .thenReturn(new WeeklyTrainingSummary(3, 2, 3, 1, 8.6, 5.0, "ok"));

    WeeklyCheckIn checkIn = service.currentCheckIn();

    assertThat(checkIn.weekStartDate()).isEqualTo(LocalDate.of(2026, 7, 6));
    assertThat(checkIn.latestWeightKg()).isEqualTo(72.5);
    assertThat(checkIn.completedRunningSessions()).isEqualTo(2);
    assertThat(checkIn.plannedStrengthSessions()).isEqualTo(3);
    assertThat(checkIn.notes()).isNull();
  }

  @Test
  void degradesGracefullyWhenBothSummariesAreEmpty() {
    when(bodySummaryService.currentSummary())
        .thenReturn(new WeeklyBodySummary(null, null, null, null, null, null, "no data"));
    when(trainingSummaryService.currentSummary())
        .thenReturn(new WeeklyTrainingSummary(0, 0, 0, 0, 0.0, 0.0, "empty"));

    WeeklyCheckIn checkIn = service.currentCheckIn();

    assertThat(checkIn.weekStartDate()).isEqualTo(LocalDate.of(2026, 7, 6));
    assertThat(checkIn.latestWeightKg()).isNull();
    assertThat(checkIn.plannedRunningSessions()).isZero();
    assertThat(checkIn.completedStrengthSessions()).isZero();
  }
}
