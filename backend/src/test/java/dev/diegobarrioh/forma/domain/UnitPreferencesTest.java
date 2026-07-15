package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Domain unit tests for {@link UnitPreferences} (FOR-107). Plain JUnit 5 + AssertJ (ADR-007). */
class UnitPreferencesTest {

  @Test
  void defaultConstantIsTheMetricSet() {
    assertThat(UnitPreferences.DEFAULT.weightUnit()).isEqualTo(WeightUnit.KG);
    assertThat(UnitPreferences.DEFAULT.heightUnit()).isEqualTo(HeightUnit.CM);
    assertThat(UnitPreferences.DEFAULT.distanceUnit()).isEqualTo(DistanceUnit.KM);
    assertThat(UnitPreferences.DEFAULT.energyUnit()).isEqualTo(EnergyUnit.KCAL);
  }

  @Test
  void nullFieldsIndividuallyDefaultToMetric() {
    UnitPreferences prefs = new UnitPreferences(null, null, null, null);

    assertThat(prefs).isEqualTo(UnitPreferences.DEFAULT);
  }

  @Test
  void explicitValueIsPreserved() {
    UnitPreferences prefs = new UnitPreferences(WeightUnit.KG, null, null, null);

    assertThat(prefs.weightUnit()).isEqualTo(WeightUnit.KG);
    assertThat(prefs.heightUnit()).isEqualTo(HeightUnit.CM);
  }
}
