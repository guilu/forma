package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Domain tests for {@link WeeklyHistoryBucket} (FOR-139): shape invariants only. */
class WeeklyHistoryBucketTest {

  @Test
  void holdsWeekStartPlannedAndCompleted() {
    WeeklyHistoryBucket bucket = new WeeklyHistoryBucket(LocalDate.of(2026, 6, 1), 7, 4);

    assertThat(bucket.weekStart()).isEqualTo(LocalDate.of(2026, 6, 1));
    assertThat(bucket.planned()).isEqualTo(7);
    assertThat(bucket.completed()).isEqualTo(4);
  }

  @Test
  void rejectsANegativePlanned() {
    assertThatThrownBy(() -> new WeeklyHistoryBucket(LocalDate.of(2026, 6, 1), -1, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsANegativeCompleted() {
    assertThatThrownBy(() -> new WeeklyHistoryBucket(LocalDate.of(2026, 6, 1), 7, -1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void completedMayExceedPlannedWithoutError() {
    WeeklyHistoryBucket bucket = new WeeklyHistoryBucket(LocalDate.of(2026, 6, 1), 7, 9);

    assertThat(bucket.completed()).isEqualTo(9);
  }
}
