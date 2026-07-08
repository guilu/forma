package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.domain.WeeklyBodySummary;
import dev.diegobarrioh.forma.domain.WeeklyCheckIn;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WeeklyCheckInBuilder} (FOR-40): assembly from the FOR-21/FOR-28 summaries,
 * including the partial and empty-week cases.
 */
class WeeklyCheckInBuilderTest {

  private static final LocalDate WEEK_START = LocalDate.of(2026, 7, 6); // a Monday

  private static WeeklyBodySummary bodySummary() {
    return new WeeklyBodySummary(72.5, 18.0, 59.0, -0.3, -0.2, 7, "ok");
  }

  private static WeeklyTrainingSummary trainingSummary() {
    return new WeeklyTrainingSummary(3, 2, 3, 1, 8.6, 5.0, "ok");
  }

  @Test
  void assemblesFullWeekFromBothSummaries() {
    WeeklyCheckIn checkIn =
        WeeklyCheckInBuilder.build(WEEK_START, bodySummary(), trainingSummary(), "good week");

    assertThat(checkIn.weekStartDate()).isEqualTo(WEEK_START);
    assertThat(checkIn.latestWeightKg()).isEqualTo(72.5);
    assertThat(checkIn.latestBodyFatPercentage()).isEqualTo(18.0);
    assertThat(checkIn.latestLeanMassKg()).isEqualTo(59.0);
    assertThat(checkIn.plannedRunningSessions()).isEqualTo(3);
    assertThat(checkIn.completedRunningSessions()).isEqualTo(2);
    assertThat(checkIn.plannedStrengthSessions()).isEqualTo(3);
    assertThat(checkIn.completedStrengthSessions()).isEqualTo(1);
    assertThat(checkIn.notes()).isEqualTo("good week");
  }

  @Test
  void bodyOnlyWeekHasZeroTrainingCounts() {
    WeeklyCheckIn checkIn = WeeklyCheckInBuilder.build(WEEK_START, bodySummary(), null, null);

    assertThat(checkIn.latestWeightKg()).isEqualTo(72.5);
    assertThat(checkIn.plannedRunningSessions()).isZero();
    assertThat(checkIn.completedRunningSessions()).isZero();
    assertThat(checkIn.plannedStrengthSessions()).isZero();
    assertThat(checkIn.completedStrengthSessions()).isZero();
  }

  @Test
  void trainingOnlyWeekHasNullBodyValues() {
    WeeklyCheckIn checkIn = WeeklyCheckInBuilder.build(WEEK_START, null, trainingSummary(), null);

    assertThat(checkIn.latestWeightKg()).isNull();
    assertThat(checkIn.latestBodyFatPercentage()).isNull();
    assertThat(checkIn.latestLeanMassKg()).isNull();
    assertThat(checkIn.plannedRunningSessions()).isEqualTo(3);
    assertThat(checkIn.completedStrengthSessions()).isEqualTo(1);
  }

  @Test
  void emptyWeekHasNullBodyValuesAndZeroCounts() {
    WeeklyCheckIn checkIn = WeeklyCheckInBuilder.build(WEEK_START, null, null, null);

    assertThat(checkIn.latestWeightKg()).isNull();
    assertThat(checkIn.latestBodyFatPercentage()).isNull();
    assertThat(checkIn.latestLeanMassKg()).isNull();
    assertThat(checkIn.plannedRunningSessions()).isZero();
    assertThat(checkIn.completedRunningSessions()).isZero();
    assertThat(checkIn.plannedStrengthSessions()).isZero();
    assertThat(checkIn.completedStrengthSessions()).isZero();
    assertThat(checkIn.notes()).isNull();
  }

  @Test
  void carriesEmptyBodySummaryValuesAsNull() {
    // FOR-21 returns a summary with null body values when there are no measurements.
    WeeklyBodySummary empty = new WeeklyBodySummary(null, null, null, null, null, null, "no data");

    WeeklyCheckIn checkIn = WeeklyCheckInBuilder.build(WEEK_START, empty, trainingSummary(), null);

    assertThat(checkIn.latestWeightKg()).isNull();
    assertThat(checkIn.latestBodyFatPercentage()).isNull();
    assertThat(checkIn.latestLeanMassKg()).isNull();
  }
}
