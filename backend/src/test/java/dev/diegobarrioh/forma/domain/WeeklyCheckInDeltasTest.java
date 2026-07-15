package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WeeklyCheckInDeltas} (FOR-110): week-over-week delta computation between
 * two {@link WeeklyCheckIn} snapshots, and the no-prior-period case.
 */
class WeeklyCheckInDeltasTest {

  private static final LocalDate CURRENT_WEEK = LocalDate.of(2026, 7, 13);
  private static final LocalDate PRIOR_WEEK = LocalDate.of(2026, 7, 6);

  @Test
  void computesDeltasBetweenCurrentAndPriorCheckIns() {
    WeeklyCheckIn current = new WeeklyCheckIn(CURRENT_WEEK, 71.0, 17.5, 55.5, 3, 3, 3, 2, null);
    WeeklyCheckIn prior = new WeeklyCheckIn(PRIOR_WEEK, 72.5, 18.0, 55.0, 3, 2, 3, 1, null);

    WeeklyCheckInDeltas deltas = WeeklyCheckInDeltas.between(current, prior);

    assertThat(deltas.weightDeltaKg()).isEqualTo(-1.5);
    assertThat(deltas.bodyFatPercentageDelta()).isEqualTo(-0.5);
    assertThat(deltas.leanMassDeltaKg()).isEqualTo(0.5);
    // current completed = 3 + 2 = 5, prior completed = 2 + 1 = 3 -> delta 2
    assertThat(deltas.trainingCompletionDelta()).isEqualTo(2);
  }

  @Test
  void yieldsNullDeltasNotZeroWhenThereIsNoPriorPeriod() {
    WeeklyCheckIn current = new WeeklyCheckIn(CURRENT_WEEK, 71.0, 17.5, 55.5, 3, 3, 3, 2, null);

    WeeklyCheckInDeltas deltas = WeeklyCheckInDeltas.between(current, null);

    assertThat(deltas.weightDeltaKg()).isNull();
    assertThat(deltas.bodyFatPercentageDelta()).isNull();
    assertThat(deltas.leanMassDeltaKg()).isNull();
    assertThat(deltas.trainingCompletionDelta()).isNull();
    assertThat(deltas).isEqualTo(WeeklyCheckInDeltas.NONE);
  }

  @Test
  void yieldsNullForIndividualFieldsMissingOnEitherSideWithoutFabrication() {
    WeeklyCheckIn current = new WeeklyCheckIn(CURRENT_WEEK, null, 17.5, null, 3, 3, 3, 2, null);
    WeeklyCheckIn prior = new WeeklyCheckIn(PRIOR_WEEK, 72.5, null, 55.0, 3, 2, 3, 1, null);

    WeeklyCheckInDeltas deltas = WeeklyCheckInDeltas.between(current, prior);

    assertThat(deltas.weightDeltaKg()).isNull();
    assertThat(deltas.bodyFatPercentageDelta()).isNull();
    assertThat(deltas.leanMassDeltaKg()).isNull();
    assertThat(deltas.trainingCompletionDelta()).isEqualTo(2);
  }

  @Test
  void rejectsNullCurrentCheckIn() {
    assertThatNullPointerException()
        .isThrownBy(() -> WeeklyCheckInDeltas.between(null, null))
        .withMessageContaining("current");
  }
}
