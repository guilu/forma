package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link PersonalTargets} (FOR-149): the personal plan targets from the
 * *Perfil* sheet — base kcal, body-fat/weight target ranges and fat/carb macro targets. Protein
 * stays on {@link DefaultObjectives#proteinTargetG()} (reused column/field, spec FOR-149). Plain
 * JUnit 5 + AssertJ (ADR-007).
 */
class PersonalTargetsTest {

  @Test
  void emptyConstantHasNoTargetsSet() {
    assertThat(PersonalTargets.EMPTY.baseCaloriesKcal()).isNull();
    assertThat(PersonalTargets.EMPTY.bodyFatTargetMinPct()).isNull();
    assertThat(PersonalTargets.EMPTY.bodyFatTargetMaxPct()).isNull();
    assertThat(PersonalTargets.EMPTY.weightTargetMinKg()).isNull();
    assertThat(PersonalTargets.EMPTY.weightTargetMaxKg()).isNull();
    assertThat(PersonalTargets.EMPTY.fatTargetG()).isNull();
    assertThat(PersonalTargets.EMPTY.carbsTargetG()).isNull();
  }

  @Test
  void acceptsDiegosTargetsFromThePerfilSheet() {
    PersonalTargets targets = new PersonalTargets(2300.0, 12.0, 13.0, 73.0, 75.0, 70.0, 260.0);

    assertThat(targets.baseCaloriesKcal()).isEqualTo(2300.0);
    assertThat(targets.bodyFatTargetMinPct()).isEqualTo(12.0);
    assertThat(targets.bodyFatTargetMaxPct()).isEqualTo(13.0);
    assertThat(targets.weightTargetMinKg()).isEqualTo(73.0);
    assertThat(targets.weightTargetMaxKg()).isEqualTo(75.0);
    assertThat(targets.fatTargetG()).isEqualTo(70.0);
    assertThat(targets.carbsTargetG()).isEqualTo(260.0);
  }

  @Test
  void rejectsNegativeBaseCalories() {
    assertThatThrownBy(() -> new PersonalTargets(-1.0, null, null, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("baseCaloriesKcal");
  }

  @Test
  void rejectsNegativeFatTarget() {
    assertThatThrownBy(() -> new PersonalTargets(null, null, null, null, null, -1.0, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("fatTargetG");
  }

  @Test
  void rejectsNegativeCarbsTarget() {
    assertThatThrownBy(() -> new PersonalTargets(null, null, null, null, null, null, -1.0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("carbsTargetG");
  }

  @Test
  void rejectsBodyFatRangeWhereMinExceedsMax() {
    assertThatThrownBy(() -> new PersonalTargets(null, 13.0, 12.0, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("bodyFatTarget");
  }

  @Test
  void rejectsWeightRangeWhereMinExceedsMax() {
    assertThatThrownBy(() -> new PersonalTargets(null, null, null, 75.0, 73.0, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("weightTarget");
  }

  @Test
  void allowsEqualMinAndMaxRangeBounds() {
    PersonalTargets targets = new PersonalTargets(null, 12.0, 12.0, 73.0, 73.0, null, null);

    assertThat(targets.bodyFatTargetMinPct()).isEqualTo(12.0);
    assertThat(targets.weightTargetMinKg()).isEqualTo(73.0);
  }

  @Test
  void partialTargetsAreValid() {
    PersonalTargets targets = new PersonalTargets(2300.0, null, null, null, null, null, null);

    assertThat(targets.baseCaloriesKcal()).isEqualTo(2300.0);
    assertThat(targets.bodyFatTargetMinPct()).isNull();
  }
}
