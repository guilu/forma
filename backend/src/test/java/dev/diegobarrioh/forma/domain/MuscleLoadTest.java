package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MuscleLoad} (FOR-136): the frequency-only load threshold rule at its exact
 * boundaries (spec FOR-136 tests.md "assert exact boundaries").
 */
class MuscleLoadTest {

  @Test
  void oneHitMapsToMediumTheBaseLevel() {
    assertThat(MuscleLoad.fromFrequency(1)).isEqualTo(MuscleLoad.MEDIUM);
  }

  @Test
  void twoHitsMapToHigh() {
    assertThat(MuscleLoad.fromFrequency(2)).isEqualTo(MuscleLoad.HIGH);
  }

  @Test
  void moreThanTwoHitsStillMapsToHigh() {
    assertThat(MuscleLoad.fromFrequency(3)).isEqualTo(MuscleLoad.HIGH);
  }

  @Test
  void zeroOrNegativeFrequencyIsRejected() {
    assertThatThrownBy(() -> MuscleLoad.fromFrequency(0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> MuscleLoad.fromFrequency(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
