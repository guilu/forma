package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link TargetComparison} (FOR-32): per-macro reached/short against day
 * targets.
 */
class TargetComparisonTest {

  private static final NutritionDayTemplate TARGET =
      new NutritionDayTemplate(NutritionDayType.RUNNING, 2600, 160, 320, 70, null);

  @Test
  void reportsAllReachedWhenTotalsMeetTargets() {
    TargetComparison comparison =
        TargetComparison.of(new NutritionTotals(2600, 165.0, 330.0, 72.0), TARGET);

    assertThat(comparison.caloriesReached()).isTrue();
    assertThat(comparison.proteinReached()).isTrue();
    assertThat(comparison.carbsReached()).isTrue();
    assertThat(comparison.fatReached()).isTrue();
  }

  @Test
  void reportsShortMacrosWhenBelowTargets() {
    TargetComparison comparison =
        TargetComparison.of(new NutritionTotals(2400, 140.0, 320.0, 70.0), TARGET);

    assertThat(comparison.caloriesReached()).isFalse(); // 2400 < 2600
    assertThat(comparison.proteinReached()).isFalse(); // 140 < 160
    assertThat(comparison.carbsReached()).isTrue(); // 320 == 320
    assertThat(comparison.fatReached()).isTrue(); // 70 == 70
  }
}
