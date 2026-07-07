package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for the rule-based weekly summary (FOR-21). Plain JUnit 5 + AssertJ, no Spring
 * (ADR-007). Covers latest-value extraction, weekly change, and the insufficient-data paths.
 */
class WeeklyBodySummaryTest {

  private static BodyMeasurement measurement(String isoDate, double weightKg, double bodyFat) {
    return new BodyMeasurement(
        Instant.parse(isoDate + "T08:00:00Z"),
        MeasurementSource.MANUAL,
        weightKg,
        bodyFat,
        22.7,
        null);
  }

  @Test
  @DisplayName("no measurements: everything null with a clear message")
  void emptyHistory() {
    WeeklyBodySummary summary = WeeklyBodySummary.from(List.of());

    assertThat(summary.latestWeightKg()).isNull();
    assertThat(summary.weeklyWeightChangeKg()).isNull();
    assertThat(summary.comparisonDays()).isNull();
    assertThat(summary.message()).isEqualTo("Aún no hay mediciones para resumir.");
  }

  @Test
  @DisplayName("single measurement: latest values, no weekly change")
  void singleMeasurement() {
    WeeklyBodySummary summary =
        WeeklyBodySummary.from(List.of(measurement("2026-07-08", 73.6, 14.7)));

    assertThat(summary.latestWeightKg()).isEqualTo(73.6);
    assertThat(summary.latestBodyFatPercentage()).isEqualTo(14.7);
    assertThat(summary.latestLeanMassKg()).isNotNull();
    // No prior measurement → change fields are null, not 0.
    assertThat(summary.weeklyWeightChangeKg()).isNull();
    assertThat(summary.weeklyBodyFatChange()).isNull();
    assertThat(summary.comparisonDays()).isNull();
    assertThat(summary.message()).contains("Registra otra medición");
  }

  @Test
  @DisplayName("two measurements one week apart: change computed over the actual window")
  void weeklyChange() {
    // Newest-first, as the FOR-16 repository returns them.
    WeeklyBodySummary summary =
        WeeklyBodySummary.from(
            List.of(measurement("2026-07-08", 73.6, 14.7), measurement("2026-07-01", 74.1, 15.0)));

    assertThat(summary.latestWeightKg()).isEqualTo(73.6);
    assertThat(summary.weeklyWeightChangeKg()).isCloseTo(-0.5, within(1e-9));
    assertThat(summary.weeklyBodyFatChange()).isCloseTo(-0.3, within(1e-9));
    assertThat(summary.comparisonDays()).isEqualTo(7);
    assertThat(summary.message()).contains("-0.5 kg en 7 días");
  }

  @Test
  @DisplayName("gap longer than a week: window is stated honestly, not called a week")
  void irregularGapReportsActualDays() {
    WeeklyBodySummary summary =
        WeeklyBodySummary.from(
            List.of(measurement("2026-07-08", 73.6, 14.7), measurement("2026-06-24", 75.0, 16.0)));

    assertThat(summary.comparisonDays()).isEqualTo(14);
    assertThat(summary.message()).contains("en 14 días");
  }
}
