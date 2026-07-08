package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link WeeklyCheckIn} domain value (FOR-40). */
class WeeklyCheckInTest {

  private static final LocalDate WEEK_START = LocalDate.of(2026, 7, 6); // a Monday

  @Test
  void holdsBodyValuesAndSessionCounts() {
    WeeklyCheckIn checkIn =
        new WeeklyCheckIn(WEEK_START, 72.5, 18.0, 59.0, 3, 2, 3, 1, "felt strong");

    assertThat(checkIn.weekStartDate()).isEqualTo(WEEK_START);
    assertThat(checkIn.latestWeightKg()).isEqualTo(72.5);
    assertThat(checkIn.latestBodyFatPercentage()).isEqualTo(18.0);
    assertThat(checkIn.latestLeanMassKg()).isEqualTo(59.0);
    assertThat(checkIn.plannedRunningSessions()).isEqualTo(3);
    assertThat(checkIn.completedRunningSessions()).isEqualTo(2);
    assertThat(checkIn.plannedStrengthSessions()).isEqualTo(3);
    assertThat(checkIn.completedStrengthSessions()).isEqualTo(1);
    assertThat(checkIn.notes()).isEqualTo("felt strong");
  }

  @Test
  void allowsNullBodyValuesAndNotesWithoutFabrication() {
    WeeklyCheckIn checkIn = new WeeklyCheckIn(WEEK_START, null, null, null, 0, 0, 0, 0, null);

    assertThat(checkIn.latestWeightKg()).isNull();
    assertThat(checkIn.latestBodyFatPercentage()).isNull();
    assertThat(checkIn.latestLeanMassKg()).isNull();
    assertThat(checkIn.notes()).isNull();
    assertThat(checkIn.plannedRunningSessions()).isZero();
  }

  @Test
  void rejectsNullWeekStartDate() {
    assertThatNullPointerException()
        .isThrownBy(() -> new WeeklyCheckIn(null, 72.5, 18.0, 59.0, 3, 2, 3, 1, null))
        .withMessageContaining("weekStartDate");
  }
}
