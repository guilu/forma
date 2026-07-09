package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link RecoveryWarningRules} evaluator (FOR-44). */
class RecoveryWarningRulesTest {

  private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");
  private static final LocalDate WEEK = LocalDate.of(2026, 7, 6);

  private static WeeklyCheckIn checkIn(
      int plannedRunning, int completedRunning, int plannedStrength, int completedStrength) {
    return new WeeklyCheckIn(
        WEEK,
        70.0,
        18.0,
        55.0,
        plannedRunning,
        completedRunning,
        plannedStrength,
        completedStrength,
        null);
  }

  private static WeeklyBodySummary body(Double weeklyBodyFatChange) {
    return new WeeklyBodySummary(70.0, 18.0, 55.0, -0.1, weeklyBodyFatChange, 7, "x");
  }

  private static Recommendation onlyWarning(List<Recommendation> recs) {
    assertThat(recs).hasSize(1);
    Recommendation rec = recs.get(0);
    assertThat(rec.category()).isEqualTo(RecommendationCategory.RECOVERY);
    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.WARNING);
    assertThat(rec.message()).isNotBlank();
    assertThat(rec.reason()).isNotBlank();
    assertThat(rec.createdAt()).isEqualTo(NOW);
    return rec;
  }

  @Test
  void highLoadLowCompletionWarns() {
    Recommendation rec = onlyWarning(RecoveryWarningRules.evaluate(checkIn(3, 1, 3, 0), null, NOW));

    assertThat(rec.reason()).contains("Carga alta");
    assertThat(rec.relatedMetric()).isNull();
  }

  @Test
  void worseningBodyTrendUnderHighLoadWarns() {
    // High completion, but body fat rising while load is high.
    Recommendation rec =
        onlyWarning(RecoveryWarningRules.evaluate(checkIn(3, 3, 3, 3), body(0.5), NOW));

    assertThat(rec.reason()).contains("grasa corporal");
    assertThat(rec.relatedMetric()).isEqualTo("weeklyBodyFatChange");
  }

  @Test
  void bothSignalsProduceOneCombinedWarning() {
    Recommendation rec =
        onlyWarning(RecoveryWarningRules.evaluate(checkIn(3, 1, 3, 0), body(0.4), NOW));

    assertThat(rec.reason()).contains("Carga alta");
    assertThat(rec.reason()).contains("grasa corporal");
  }

  @Test
  void healthyWeekProducesNoWarning() {
    // High completion, body fat improving.
    List<Recommendation> recs = RecoveryWarningRules.evaluate(checkIn(3, 3, 3, 3), body(-0.2), NOW);

    assertThat(recs).isEmpty();
  }

  @Test
  void emptyWeekProducesNoWarning() {
    List<Recommendation> recs = RecoveryWarningRules.evaluate(checkIn(0, 0, 0, 0), null, NOW);

    assertThat(recs).isEmpty();
  }

  @Test
  void nullCheckInProducesNoWarning() {
    assertThat(RecoveryWarningRules.evaluate(null, body(1.0), NOW)).isEmpty();
  }

  @Test
  void loadBelowThresholdDoesNotWarnEvenWithZeroCompletion() {
    // 4 planned (< 5) → not high load → no warning, however low the completion.
    List<Recommendation> recs = RecoveryWarningRules.evaluate(checkIn(2, 0, 2, 0), null, NOW);

    assertThat(recs).isEmpty();
  }

  @Test
  void missingBodyDataNeverCausesBodyWarning() {
    // High load, high completion, no body summary → no signal.
    List<Recommendation> recs = RecoveryWarningRules.evaluate(checkIn(3, 3, 3, 3), null, NOW);

    assertThat(recs).isEmpty();
  }

  @Test
  void thresholdsAreInclusiveAtHighLoadAndLowCompletion() {
    // Exactly 5 planned, exactly 40% completed → warns.
    Recommendation rec = onlyWarning(RecoveryWarningRules.evaluate(checkIn(5, 2, 0, 0), null, NOW));

    assertThat(rec.reason()).contains("Carga alta");
  }
}
