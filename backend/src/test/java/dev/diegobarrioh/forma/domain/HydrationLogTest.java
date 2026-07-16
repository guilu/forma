package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link HydrationLog} (FOR-130): the per-day hydration aggregate that
 * accumulates {@link WaterIntakeEntry} volumes and computes progress toward a daily goal.
 * Derived-on-read (spec FOR-130 Open Questions: the total is never stored separately, so it can
 * never drift from the entries) — {@link HydrationLog#totalMl()} is always a fresh sum of the
 * current entries, mirroring {@code MealLogTest} (FOR-127). Plain JUnit 5 + AssertJ (ADR-007).
 */
class HydrationLogTest {

  private static final LocalDate DAY = LocalDate.of(2026, 7, 15);

  @Test
  void emptyLogHasZeroTotal() {
    HydrationLog log = HydrationLog.empty(DAY);

    assertThat(log.totalMl()).isZero();
    assertThat(log.entries()).isEmpty();
  }

  @Test
  void multipleEntriesSameDaySumNeverOverwritten() {
    HydrationLog log =
        HydrationLog.empty(DAY)
            .withEntry(new WaterIntakeEntry(DAY, 500.0))
            .withEntry(new WaterIntakeEntry(DAY, 300.0))
            .withEntry(new WaterIntakeEntry(DAY, 200.0));

    assertThat(log.entries()).hasSize(3);
    assertThat(log.totalMl()).isEqualTo(1000.0);
  }

  @Test
  void progressIsTotalDividedByGoal() {
    HydrationLog log = HydrationLog.empty(DAY).withEntry(new WaterIntakeEntry(DAY, 1500.0));

    assertThat(log.progressToward(2000.0)).isEqualTo(0.75);
  }

  @Test
  void progressIsNullWhenGoalIsNull() {
    HydrationLog log = HydrationLog.empty(DAY).withEntry(new WaterIntakeEntry(DAY, 1500.0));

    assertThat(log.progressToward(null)).isNull();
  }

  @Test
  void progressIsNullWhenGoalIsZeroOrNegative() {
    HydrationLog log = HydrationLog.empty(DAY).withEntry(new WaterIntakeEntry(DAY, 1500.0));

    assertThat(log.progressToward(0.0)).isNull();
    assertThat(log.progressToward(-100.0)).isNull();
  }

  @Test
  void progressIsUncappedWhenTotalExceedsGoal() {
    // Documented decision (spec FOR-130 api.md: "Cap at 1.0 or report raw — document"): progress
    // is reported raw/uncapped so the UI can distinguish "met the goal" from "doubled the goal".
    HydrationLog log = HydrationLog.empty(DAY).withEntry(new WaterIntakeEntry(DAY, 3000.0));

    assertThat(log.progressToward(2000.0)).isEqualTo(1.5);
  }

  @Test
  void emptyDayHasZeroTotalButProgressStillResolvesAgainstTheGoal() {
    HydrationLog log = HydrationLog.empty(DAY);

    assertThat(log.progressToward(2000.0)).isEqualTo(0.0);
  }
}
