package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Domain tests for {@link GoalProgress} derivation (FOR-125): reuses {@link WeeklyBodySummary}
 * (itself derived from {@code BodyMeasurement} history, FOR-21) rather than recomputing body
 * metrics, and never fabricates a value when the summary has none.
 */
class GoalProgressTest {

  @Test
  void derivesCurrentAndRatioFromALinkedMetricWithData() {
    WeeklyBodySummary summary =
        new WeeklyBodySummary(80.0, 16.4, 67.0, null, null, null, "message");

    GoalProgress progress = GoalProgress.derive(GoalMetric.BODY_FAT_PCT, 12.0, summary);

    assertThat(progress.current()).isEqualTo(16.4);
    assertThat(progress.target()).isEqualTo(12.0);
    assertThat(progress.ratio()).isEqualTo(16.4 / 12.0);
    assertThat(progress.source()).isEqualTo(ProgressSource.BODY_MEASUREMENT);
  }

  @Test
  void currentAndRatioAreNullWhenTheSummaryHasNoDataForTheMetric() {
    WeeklyBodySummary empty = WeeklyBodySummary.from(java.util.List.of());

    GoalProgress progress = GoalProgress.derive(GoalMetric.BODY_FAT_PCT, 12.0, empty);

    assertThat(progress.current()).isNull();
    assertThat(progress.ratio()).isNull();
    // the metric IS mapped to a source even though there is no data yet (never fabricated, but
    // the intended source stays visible so callers can explain the null).
    assertThat(progress.source()).isEqualTo(ProgressSource.BODY_MEASUREMENT);
  }

  @Test
  void ratioIsNullWhenTargetIsZero() {
    WeeklyBodySummary summary = new WeeklyBodySummary(80.0, 16.4, 67.0, null, null, null, "m");

    GoalProgress progress = GoalProgress.derive(GoalMetric.BODY_FAT_PCT, 0.0, summary);

    assertThat(progress.current()).isEqualTo(16.4);
    assertThat(progress.ratio()).isNull();
  }

  @Test
  void weightKgMetricReadsLatestWeightFromTheSummary() {
    WeeklyBodySummary summary = new WeeklyBodySummary(80.0, 16.4, 67.0, null, null, null, "m");

    GoalProgress progress = GoalProgress.derive(GoalMetric.WEIGHT_KG, 75.0, summary);

    assertThat(progress.current()).isEqualTo(80.0);
  }

  @Test
  void leanMassKgMetricReadsLatestLeanMassFromTheSummary() {
    WeeklyBodySummary summary = new WeeklyBodySummary(80.0, 16.4, 67.0, null, null, null, "m");

    GoalProgress progress = GoalProgress.derive(GoalMetric.LEAN_MASS_KG, 70.0, summary);

    assertThat(progress.current()).isEqualTo(67.0);
  }

  @Test
  void doesNotThrowWhenDataIsMissing() {
    WeeklyBodySummary empty = WeeklyBodySummary.from(java.util.List.of());

    assertThat(GoalProgress.derive(GoalMetric.WEIGHT_KG, 70.0, empty)).isNotNull();
    assertThat(GoalProgress.derive(GoalMetric.LEAN_MASS_KG, 70.0, empty)).isNotNull();
  }
}
