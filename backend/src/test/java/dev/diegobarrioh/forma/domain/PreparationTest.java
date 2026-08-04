package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link Preparation} (V51): when two foods' portions are comparing like with
 * like. Plain JUnit 5 + AssertJ (ADR-007).
 */
class PreparationTest {

  /** The case this exists for: dry rice against boiled pasta is two different questions. */
  @Test
  void refusesToCompareAcrossTheKitchen() {
    assertThat(Preparation.comparable(Preparation.CRUDO, Preparation.COCINADO)).isFalse();
    assertThat(Preparation.comparable(Preparation.COCINADO, Preparation.CRUDO)).isFalse();
  }

  @Test
  void comparesTwoFoodsInTheSameState() {
    assertThat(Preparation.comparable(Preparation.CRUDO, Preparation.CRUDO)).isTrue();
    assertThat(Preparation.comparable(Preparation.COCINADO, Preparation.COCINADO)).isTrue();
  }

  /** Oil is oil whether the thing beside it was cooked or not. */
  @Test
  void aFoodThatNeverCooksAgreesWithEverything() {
    assertThat(Preparation.comparable(Preparation.TAL_CUAL, Preparation.CRUDO)).isTrue();
    assertThat(Preparation.comparable(Preparation.COCINADO, Preparation.TAL_CUAL)).isTrue();
  }

  /**
   * Silence is not disagreement. Refusing to compare until somebody classified both foods would
   * make an unfilled column block work rather than inform it — and the column starts empty on
   * purpose, because only two of the twenty-three seeded foods are deducible without guessing.
   */
  @Test
  void anUnknownStateAgreesWithEverything() {
    assertThat(Preparation.comparable(null, Preparation.CRUDO)).isTrue();
    assertThat(Preparation.comparable(Preparation.COCINADO, null)).isTrue();
    assertThat(Preparation.comparable(null, null)).isTrue();
  }
}
