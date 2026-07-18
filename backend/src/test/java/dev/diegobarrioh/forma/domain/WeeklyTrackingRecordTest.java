package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link WeeklyTrackingRecord} (FOR-155, epic FOR-148 slice 7): the weekly
 * *Seguimiento* row (km running, ritmo 4 km, kcal recomendadas, comentario, alongside body
 * metrics). Plain JUnit 5 + AssertJ (ADR-007), framework-free (ADR-001).
 */
class WeeklyTrackingRecordTest {

  private static final LocalDate WEEK_1_DATE = LocalDate.parse("2026-07-06");

  @Test
  void acceptsWeek1FromTheSeguimientoSheet() {
    WeeklyTrackingRecord record =
        new WeeklyTrackingRecord(
            1, WEEK_1_DATE, 73.6, 14.7, 22.7, 13.0, "6:00", 2300.0, "Primera semana");

    assertThat(record.week()).isEqualTo(1);
    assertThat(record.date()).isEqualTo(WEEK_1_DATE);
    assertThat(record.weightKg()).isEqualTo(73.6);
    assertThat(record.bodyFatPercentage()).isEqualTo(14.7);
    assertThat(record.bmi()).isEqualTo(22.7);
    assertThat(record.runningKm()).isEqualTo(13.0);
    assertThat(record.pace4kmMinPerKm()).isEqualTo("6:00");
    assertThat(record.recommendedKcal()).isEqualTo(2300.0);
    assertThat(record.comment()).isEqualTo("Primera semana");
  }

  @Test
  void derivesFatAndLeanMassFromWeightAndBodyFatPercentage() {
    // Mirrors BodyMeasurement (FOR-15): single source of truth, no stored-vs-derived drift.
    WeeklyTrackingRecord record =
        new WeeklyTrackingRecord(1, WEEK_1_DATE, 73.6, 14.7, 22.7, 13.0, "6:00", 2300.0, null);

    assertThat(record.fatMassKg()).isPresent();
    assertThat(record.fatMassKg().orElseThrow())
        .isCloseTo(10.8192, org.assertj.core.data.Offset.offset(0.001));
    assertThat(record.leanMassKg()).isPresent();
    assertThat(record.leanMassKg().orElseThrow())
        .isCloseTo(62.7808, org.assertj.core.data.Offset.offset(0.001));
  }

  @Test
  void fatAndLeanMassAreEmptyWhenBodyFatPercentageIsAbsent() {
    WeeklyTrackingRecord record =
        new WeeklyTrackingRecord(1, WEEK_1_DATE, 73.6, null, null, null, null, null, null);

    assertThat(record.fatMassKg()).isEmpty();
    assertThat(record.leanMassKg()).isEmpty();
  }

  @Test
  void fatAndLeanMassAreEmptyWhenWeightIsAbsent() {
    WeeklyTrackingRecord record =
        new WeeklyTrackingRecord(1, WEEK_1_DATE, null, 14.7, null, null, null, null, null);

    assertThat(record.fatMassKg()).isEmpty();
    assertThat(record.leanMassKg()).isEmpty();
  }

  @Test
  void partialRecordWithOnlyWeekAndDateIsValid() {
    // SEGUIMIENTO starts empty; a week with only some fields filled is a normal state
    // (spec FOR-155 Edge Cases: "A week with only some fields filled ... is valid").
    WeeklyTrackingRecord record =
        new WeeklyTrackingRecord(
            2, LocalDate.parse("2026-07-13"), null, null, null, null, null, null, null);

    assertThat(record.weightKg()).isNull();
    assertThat(record.runningKm()).isNull();
    assertThat(record.pace4kmMinPerKm()).isNull();
    assertThat(record.recommendedKcal()).isNull();
  }

  @Test
  void rejectsNonPositiveWeek() {
    assertThatThrownBy(
            () ->
                new WeeklyTrackingRecord(0, WEEK_1_DATE, null, null, null, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("week");
  }

  @Test
  void rejectsNullDate() {
    assertThatThrownBy(
            () -> new WeeklyTrackingRecord(1, null, null, null, null, null, null, null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("date");
  }

  @Test
  void rejectsNonPositiveWeight() {
    assertThatThrownBy(
            () -> new WeeklyTrackingRecord(1, WEEK_1_DATE, 0.0, null, null, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("weightKg");
  }

  @Test
  void rejectsBodyFatPercentageOutOfRange() {
    assertThatThrownBy(
            () ->
                new WeeklyTrackingRecord(1, WEEK_1_DATE, 73.6, 150.0, null, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("bodyFatPercentage");
  }

  @Test
  void rejectsNonPositiveBmi() {
    assertThatThrownBy(
            () -> new WeeklyTrackingRecord(1, WEEK_1_DATE, null, null, 0.0, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("bmi");
  }

  @Test
  void rejectsNegativeRunningKm() {
    assertThatThrownBy(
            () ->
                new WeeklyTrackingRecord(1, WEEK_1_DATE, null, null, null, -1.0, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("runningKm");
  }

  @Test
  void rejectsNegativeRecommendedKcal() {
    assertThatThrownBy(
            () ->
                new WeeklyTrackingRecord(
                    1, WEEK_1_DATE, null, null, null, null, null, -100.0, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("recommendedKcal");
  }

  @Test
  void rejectsMalformedPace() {
    assertThatThrownBy(
            () ->
                new WeeklyTrackingRecord(
                    1, WEEK_1_DATE, null, null, null, null, "not-a-pace", null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("pace4kmMinPerKm");
  }

  @Test
  void acceptsValidPaceFormats() {
    // mm:ss, minutes 1-2 digits, seconds 00-59.
    assertThat(
            new WeeklyTrackingRecord(1, WEEK_1_DATE, null, null, null, null, "5:45", null, null)
                .pace4kmMinPerKm())
        .isEqualTo("5:45");
    assertThat(
            new WeeklyTrackingRecord(1, WEEK_1_DATE, null, null, null, null, "12:00", null, null)
                .pace4kmMinPerKm())
        .isEqualTo("12:00");
  }
}
