package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link BodyMeasurement} (FOR-15).
 *
 * <p>Plain JUnit 5 + AssertJ, no Spring context (ADR-007). Covers the derived-value calculation and
 * the edge cases listed in the FOR-15 spec/test plan.
 */
class BodyMeasurementTest {

  private static final Instant MEASURED_AT = Instant.parse("2026-07-05T08:00:00Z");

  @Nested
  @DisplayName("derived values")
  class DerivedValues {

    @Test
    @DisplayName("computes fatMassKg and leanMassKg from weight and body fat")
    void computesDerivedMassesFromInputs() {
      BodyMeasurement measurement =
          new BodyMeasurement(
              MEASURED_AT, MeasurementSource.MANUAL, 80.0, 25.0, 24.7, null, null, null);

      // fatMass = 80 * 25 / 100 = 20; leanMass = 80 - 20 = 60
      assertThat(measurement.fatMassKg()).contains(20.0);
      assertThat(measurement.leanMassKg()).contains(60.0);
    }

    @Test
    @DisplayName("leanMassKg + fatMassKg reconstruct weightKg (no drift between formulas)")
    void derivedMassesRemainConsistentWithWeight() {
      BodyMeasurement measurement =
          new BodyMeasurement(
              MEASURED_AT, MeasurementSource.MANUAL, 73.4, 18.3, null, null, null, null);

      double fatMass = measurement.fatMassKg().orElseThrow();
      double leanMass = measurement.leanMassKg().orElseThrow();

      assertThat(fatMass + leanMass).isEqualTo(measurement.weightKg());
    }

    @Test
    @DisplayName("notes do not affect the calculation")
    void notesDoNotAffectCalculation() {
      BodyMeasurement withNotes =
          new BodyMeasurement(
              MEASURED_AT, MeasurementSource.MANUAL, 80.0, 25.0, null, null, null, "post-run");
      BodyMeasurement withoutNotes =
          new BodyMeasurement(
              MEASURED_AT, MeasurementSource.MANUAL, 80.0, 25.0, null, null, null, null);

      assertThat(withNotes.fatMassKg()).isEqualTo(withoutNotes.fatMassKg());
      assertThat(withNotes.leanMassKg()).isEqualTo(withoutNotes.leanMassKg());
    }
  }

  @Nested
  @DisplayName("edge cases")
  class EdgeCases {

    @Test
    @DisplayName("missing bodyFatPercentage yields no derived masses instead of zero")
    void missingBodyFatYieldsEmptyDerivedValues() {
      BodyMeasurement measurement =
          new BodyMeasurement(
              MEASURED_AT, MeasurementSource.MANUAL, 80.0, null, null, null, null, null);

      assertThat(measurement.fatMassKg()).isEmpty();
      assertThat(measurement.leanMassKg()).isEmpty();
    }

    @Test
    @DisplayName("bodyFatPercentage of 0 gives zero fat mass and full lean mass")
    void bodyFatAtLowerBoundary() {
      BodyMeasurement measurement =
          new BodyMeasurement(
              MEASURED_AT, MeasurementSource.MANUAL, 80.0, 0.0, null, null, null, null);

      assertThat(measurement.fatMassKg()).contains(0.0);
      assertThat(measurement.leanMassKg()).contains(80.0);
    }

    @Test
    @DisplayName("bodyFatPercentage of 100 gives full fat mass and zero lean mass")
    void bodyFatAtUpperBoundary() {
      BodyMeasurement measurement =
          new BodyMeasurement(
              MEASURED_AT, MeasurementSource.MANUAL, 80.0, 100.0, null, null, null, null);

      assertThat(measurement.fatMassKg()).contains(80.0);
      assertThat(measurement.leanMassKg()).contains(0.0);
    }

    @Test
    @DisplayName("zero weightKg is rejected at construction")
    void zeroWeightIsRejected() {
      assertThatThrownBy(
              () ->
                  new BodyMeasurement(
                      MEASURED_AT, MeasurementSource.MANUAL, 0.0, 20.0, null, null, null, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("weightKg");
    }

    @Test
    @DisplayName("negative weightKg is rejected at construction")
    void negativeWeightIsRejected() {
      assertThatThrownBy(
              () ->
                  new BodyMeasurement(
                      MEASURED_AT, MeasurementSource.MANUAL, -1.0, 20.0, null, null, null, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("weightKg");
    }

    @Test
    @DisplayName("bodyFatPercentage outside [0, 100] is rejected at construction")
    void outOfRangeBodyFatIsRejected() {
      assertThatThrownBy(
              () ->
                  new BodyMeasurement(
                      MEASURED_AT, MeasurementSource.MANUAL, 80.0, 150.0, null, null, null, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("bodyFatPercentage");
    }

    @Test
    @DisplayName("accepts muscleMassKg and waterPercentage and exposes them")
    void acceptsMuscleMassAndWaterPercentage() {
      BodyMeasurement measurement =
          new BodyMeasurement(
              MEASURED_AT, MeasurementSource.MANUAL, 80.0, 25.0, 24.7, 62.8, 58.0, null);

      assertThat(measurement.muscleMassKg()).isEqualTo(62.8);
      assertThat(measurement.waterPercentage()).isEqualTo(58.0);
    }

    @Test
    @DisplayName("both muscleMassKg and waterPercentage absent leaves the measurement valid")
    void muscleMassAndWaterPercentageAreOptional() {
      BodyMeasurement measurement =
          new BodyMeasurement(
              MEASURED_AT, MeasurementSource.MANUAL, 80.0, 25.0, 24.7, null, null, null);

      assertThat(measurement.muscleMassKg()).isNull();
      assertThat(measurement.waterPercentage()).isNull();
    }

    @Test
    @DisplayName("measured muscleMassKg is independent of derived leanMassKg")
    void measuredMuscleMassIsIndependentOfDerivedLeanMass() {
      // leanMass = 80 - (80 * 25 / 100) = 60, but the measured muscle mass differs on purpose.
      BodyMeasurement measurement =
          new BodyMeasurement(
              MEASURED_AT, MeasurementSource.MANUAL, 80.0, 25.0, 24.7, 52.0, null, null);

      assertThat(measurement.muscleMassKg()).isEqualTo(52.0);
      assertThat(measurement.leanMassKg()).contains(60.0);
      assertThat(measurement.muscleMassKg()).isNotEqualTo(measurement.leanMassKg().orElseThrow());
    }

    @Test
    @DisplayName("waterPercentage outside [0, 100] is rejected at construction")
    void outOfRangeWaterPercentageIsRejected() {
      assertThatThrownBy(
              () ->
                  new BodyMeasurement(
                      MEASURED_AT, MeasurementSource.MANUAL, 80.0, 25.0, null, null, 150.0, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("waterPercentage");
    }

    @Test
    @DisplayName("non-positive muscleMassKg is rejected at construction")
    void nonPositiveMuscleMassIsRejected() {
      assertThatThrownBy(
              () ->
                  new BodyMeasurement(
                      MEASURED_AT, MeasurementSource.MANUAL, 80.0, 25.0, null, 0.0, null, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("muscleMassKg");
    }
  }

  @Nested
  @DisplayName("required fields")
  class RequiredFields {

    @Test
    @DisplayName("measuredAt is required")
    void measuredAtIsRequired() {
      assertThatThrownBy(
              () ->
                  new BodyMeasurement(
                      null, MeasurementSource.MANUAL, 80.0, 20.0, null, null, null, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("measuredAt");
    }

    @Test
    @DisplayName("source is required")
    void sourceIsRequired() {
      assertThatThrownBy(
              () -> new BodyMeasurement(MEASURED_AT, null, 80.0, 20.0, null, null, null, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("source");
    }
  }
}
