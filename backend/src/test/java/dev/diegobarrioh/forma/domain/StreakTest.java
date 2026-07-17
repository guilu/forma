package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Domain tests for {@link Streak#of} (FOR-139, slice 3 of FOR-104): the pure streak algorithm
 * against hand-computed fixtures (spec FOR-139 tests.md), independent of any repository.
 */
class StreakTest {

  private static final LocalDate ASOF = LocalDate.of(2026, 7, 15);

  @Test
  void consecutiveConsistentDaysEndingAtAsOfCountTheCurrentStreak() {
    Set<LocalDate> activeDates =
        Set.of(LocalDate.of(2026, 7, 13), LocalDate.of(2026, 7, 14), LocalDate.of(2026, 7, 15));

    Streak streak = Streak.of(activeDates, LocalDate.of(2026, 7, 1), ASOF);

    assertThat(streak.currentStreakDays()).isEqualTo(3);
    assertThat(streak.asOf()).isEqualTo(ASOF);
  }

  @Test
  void aGapDayResetsTheCurrentStreakToZero() {
    // 2026-07-14 is missing -> asOf (07-15) is isolated, current streak is 1 even though
    // 07-11..07-13 form an earlier run.
    Set<LocalDate> activeDates =
        Set.of(
            LocalDate.of(2026, 7, 11),
            LocalDate.of(2026, 7, 12),
            LocalDate.of(2026, 7, 13),
            LocalDate.of(2026, 7, 15));

    Streak streak = Streak.of(activeDates, LocalDate.of(2026, 7, 1), ASOF);

    assertThat(streak.currentStreakDays()).isEqualTo(1);
  }

  @Test
  void asOfNotConsistentMeansCurrentStreakIsZeroStrictNoGrace() {
    Set<LocalDate> activeDates = Set.of(LocalDate.of(2026, 7, 13), LocalDate.of(2026, 7, 14));

    Streak streak = Streak.of(activeDates, LocalDate.of(2026, 7, 1), ASOF);

    assertThat(streak.currentStreakDays()).isZero();
  }

  @Test
  void longestStreakIsTheLongestRunInTheWindowIndependentOfTheCurrentRun() {
    // Two runs: 07-01..07-05 (5 days) and 07-14..07-15 (2 days, the current run).
    Set<LocalDate> activeDates =
        Set.of(
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 2),
            LocalDate.of(2026, 7, 3),
            LocalDate.of(2026, 7, 4),
            LocalDate.of(2026, 7, 5),
            LocalDate.of(2026, 7, 14),
            LocalDate.of(2026, 7, 15));

    Streak streak = Streak.of(activeDates, LocalDate.of(2026, 7, 1), ASOF);

    assertThat(streak.currentStreakDays()).isEqualTo(2);
    assertThat(streak.longestStreakDays()).isEqualTo(5);
  }

  @Test
  void singleActiveDayTodayGivesACurrentStreakOfOne() {
    Streak streak = Streak.of(Set.of(ASOF), LocalDate.of(2026, 7, 1), ASOF);

    assertThat(streak.currentStreakDays()).isEqualTo(1);
    assertThat(streak.longestStreakDays()).isEqualTo(1);
  }

  @Test
  void emptyHistoryYieldsZeroZeroNotAnError() {
    Streak streak = Streak.of(Set.of(), LocalDate.of(2026, 7, 1), ASOF);

    assertThat(streak.currentStreakDays()).isZero();
    assertThat(streak.longestStreakDays()).isZero();
  }

  @Test
  void aRunStartingBeforeTheWindowIsTruncatedAtTheWindowBoundary() {
    // Active every day from 06-25 through 07-15, but the window only opens on 07-10.
    Set<LocalDate> activeDates = new java.util.HashSet<>();
    for (LocalDate d = LocalDate.of(2026, 6, 25); !d.isAfter(ASOF); d = d.plusDays(1)) {
      activeDates.add(d);
    }

    Streak streak = Streak.of(activeDates, LocalDate.of(2026, 7, 10), ASOF);

    assertThat(streak.currentStreakDays()).isEqualTo(6); // 07-10..07-15
    assertThat(streak.longestStreakDays()).isEqualTo(6);
  }

  @Test
  void rejectsAWindowStartAfterAsOf() {
    assertThatThrownBy(() -> Streak.of(Set.of(), ASOF.plusDays(1), ASOF))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
