package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.WeeklyCheckIn;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import org.springframework.stereotype.Service;

/**
 * Application use case that assembles the current week's {@link WeeklyCheckIn} (FOR-40) from the
 * FOR-21 {@link WeeklyBodySummaryService} and FOR-28 {@link WeeklyTrainingSummaryService}.
 *
 * <p>Reads the existing summaries — it does not recompute body or training metrics — and delegates
 * the mapping (including missing-data handling) to {@link WeeklyCheckInBuilder}. Computed on
 * demand; no persisted entity and no HTTP endpoint (FOR-45 exposes it), mirroring the FOR-21/FOR-28
 * services. The injected {@link Clock} keeps the week-start derivation deterministic and testable.
 */
@Service
public class WeeklyCheckInService {

  private final WeeklyBodySummaryService bodySummaryService;
  private final WeeklyTrainingSummaryService trainingSummaryService;
  private final Clock clock;

  public WeeklyCheckInService(
      WeeklyBodySummaryService bodySummaryService,
      WeeklyTrainingSummaryService trainingSummaryService,
      Clock clock) {
    this.bodySummaryService = bodySummaryService;
    this.trainingSummaryService = trainingSummaryService;
    this.clock = clock;
  }

  /**
   * Assembles the check-in for the current week (Monday through Sunday). {@code notes} are left
   * null — they are optional/manual and supplied elsewhere.
   */
  public WeeklyCheckIn currentCheckIn() {
    LocalDate weekStart =
        LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    return WeeklyCheckInBuilder.build(
        weekStart,
        bodySummaryService.currentSummary(),
        trainingSummaryService.currentSummary(),
        null);
  }
}
