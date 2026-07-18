package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link ProfileBaseline} (FOR-149): the profile's plan reference values —
 * the "peso/grasa/IMC iniciales" (initial weight/body-fat/BMI) captured once from the *Perfil*
 * sheet, distinct from any {@code body_measurement} row (SEGUIMIENTO stays empty until FOR-155).
 * Plain JUnit 5 + AssertJ (ADR-007).
 */
class ProfileBaselineTest {

  @Test
  void emptyConstantHasNoBaselineSet() {
    assertThat(ProfileBaseline.EMPTY.weightKg()).isNull();
    assertThat(ProfileBaseline.EMPTY.bodyFatPct()).isNull();
    assertThat(ProfileBaseline.EMPTY.bmi()).isNull();
  }

  @Test
  void acceptsDiegosBaselineFromThePerfilSheet() {
    ProfileBaseline baseline = new ProfileBaseline(73.6, 14.7, 22.7);

    assertThat(baseline.weightKg()).isEqualTo(73.6);
    assertThat(baseline.bodyFatPct()).isEqualTo(14.7);
    assertThat(baseline.bmi()).isEqualTo(22.7);
  }

  @Test
  void rejectsNegativeWeight() {
    assertThatThrownBy(() -> new ProfileBaseline(-1.0, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("weightKg");
  }

  @Test
  void rejectsNegativeBodyFatPct() {
    assertThatThrownBy(() -> new ProfileBaseline(null, -1.0, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("bodyFatPct");
  }

  @Test
  void rejectsNegativeBmi() {
    assertThatThrownBy(() -> new ProfileBaseline(null, null, -1.0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("bmi");
  }

  @Test
  void partialBaselineIsValid() {
    ProfileBaseline baseline = new ProfileBaseline(73.6, null, null);

    assertThat(baseline.weightKg()).isEqualTo(73.6);
    assertThat(baseline.bodyFatPct()).isNull();
    assertThat(baseline.bmi()).isNull();
  }
}
