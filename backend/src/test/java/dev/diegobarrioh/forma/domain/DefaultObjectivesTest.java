package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link DefaultObjectives} (FOR-107): the user's default caloric deficit,
 * protein target and daily water objectives. Plain JUnit 5 + AssertJ (ADR-007).
 */
class DefaultObjectivesTest {

  @Test
  void emptyConstantHasNoTargetsSet() {
    assertThat(DefaultObjectives.EMPTY.caloricDeficitKcal()).isNull();
    assertThat(DefaultObjectives.EMPTY.proteinTargetG()).isNull();
    assertThat(DefaultObjectives.EMPTY.dailyWaterMl()).isNull();
  }

  @Test
  void acceptsPositiveTargets() {
    DefaultObjectives objectives = new DefaultObjectives(500.0, 140.0, 2500.0);

    assertThat(objectives.caloricDeficitKcal()).isEqualTo(500.0);
    assertThat(objectives.proteinTargetG()).isEqualTo(140.0);
    assertThat(objectives.dailyWaterMl()).isEqualTo(2500.0);
  }

  @Test
  void rejectsNegativeCaloricDeficit() {
    assertThatThrownBy(() -> new DefaultObjectives(-1.0, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("caloricDeficitKcal");
  }

  @Test
  void rejectsNegativeProteinTarget() {
    assertThatThrownBy(() -> new DefaultObjectives(null, -1.0, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("proteinTargetG");
  }

  @Test
  void rejectsNegativeDailyWater() {
    assertThatThrownBy(() -> new DefaultObjectives(null, null, -1.0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("dailyWaterMl");
  }
}
