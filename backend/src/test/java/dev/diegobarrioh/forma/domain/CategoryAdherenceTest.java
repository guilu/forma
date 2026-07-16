package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CategoryAdherence} (FOR-129): the pure planned/completed -&gt; rate
 * calculator, no Spring (ADR-007). Resolves the spec's Open Questions: {@code rate} is {@code null}
 * when {@code planned} is 0, and capped at {@code 1.0} when {@code completed} exceeds {@code
 * planned}.
 */
class CategoryAdherenceTest {

  @Test
  void computesTheRateAsCompletedOverPlanned() {
    CategoryAdherence adherence = CategoryAdherence.of(AdherenceCategory.TRAINING, 20, 17);

    assertThat(adherence.planned()).isEqualTo(20);
    assertThat(adherence.completed()).isEqualTo(17);
    assertThat(adherence.rate()).isEqualTo(0.85);
  }

  @Test
  void rateIsNullWhenPlannedIsZeroNeverADivideByZero() {
    CategoryAdherence adherence = CategoryAdherence.of(AdherenceCategory.TRAINING, 0, 0);

    assertThat(adherence.planned()).isZero();
    assertThat(adherence.completed()).isZero();
    assertThat(adherence.rate()).isNull();
  }

  @Test
  void rateIsZeroWhenNothingWasCompletedButSomethingWasPlanned() {
    CategoryAdherence adherence = CategoryAdherence.of(AdherenceCategory.NUTRITION, 30, 0);

    assertThat(adherence.rate()).isEqualTo(0.0);
  }

  @Test
  void rateIsCappedAtOneWhenCompletedExceedsPlannedButRawCountsAreUntouched() {
    CategoryAdherence adherence = CategoryAdherence.of(AdherenceCategory.MEASUREMENTS, 4, 6);

    // Raw counts are preserved as-is -- only the derived rate is capped (spec FOR-129 Open
    // Questions: "whether to cap rate at 1.0 when completed exceeds planned").
    assertThat(adherence.planned()).isEqualTo(4);
    assertThat(adherence.completed()).isEqualTo(6);
    assertThat(adherence.rate()).isEqualTo(1.0);
  }

  @Test
  void rejectsNegativePlanned() {
    assertThatThrownBy(() -> CategoryAdherence.of(AdherenceCategory.TRAINING, -1, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNegativeCompleted() {
    assertThatThrownBy(() -> CategoryAdherence.of(AdherenceCategory.TRAINING, 1, -1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
