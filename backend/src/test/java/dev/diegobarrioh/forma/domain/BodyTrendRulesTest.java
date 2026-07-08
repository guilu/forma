package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link BodyTrendRules} evaluator (FOR-42). */
class BodyTrendRulesTest {

  private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");

  private static WeeklyBodySummary summary(
      Double latestWeightKg,
      Double weeklyWeightChangeKg,
      Double weeklyBodyFatChange,
      Integer comparisonDays) {
    return new WeeklyBodySummary(
        latestWeightKg, 18.0, 55.0, weeklyWeightChangeKg, weeklyBodyFatChange, comparisonDays, "x");
  }

  private static Recommendation only(List<Recommendation> recs) {
    assertThat(recs).hasSize(1);
    Recommendation rec = recs.get(0);
    assertThat(rec.category()).isEqualTo(RecommendationCategory.BODY);
    assertThat(rec.message()).isNotBlank();
    assertThat(rec.reason()).isNotBlank();
    assertThat(rec.createdAt()).isEqualTo(NOW);
    return rec;
  }

  @Test
  void insufficientDataWhenFewerThanTwoMeasurements() {
    Recommendation rec = only(BodyTrendRules.evaluate(summary(70.0, null, null, null), NOW));

    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.INFO);
    assertThat(rec.reason()).contains("dos mediciones");
  }

  @Test
  void insufficientDataWhenSummaryIsNull() {
    Recommendation rec = only(BodyTrendRules.evaluate(null, NOW));

    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.INFO);
  }

  @Test
  void excessiveDropWhenLosingMoreThanOnePercentPerWeek() {
    // -1.5 kg over 7 days on 70 kg ≈ -2.1%/week → too fast.
    Recommendation rec = only(BodyTrendRules.evaluate(summary(70.0, -1.5, null, 7), NOW));

    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.ACTION);
    assertThat(rec.relatedMetric()).isEqualTo("weeklyWeightChangeKg");
  }

  @Test
  void worseningWhenBodyFatIncreases() {
    // Weight roughly stable, body fat up 0.5%.
    Recommendation rec = only(BodyTrendRules.evaluate(summary(70.0, -0.1, 0.5, 7), NOW));

    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.ACTION);
    assertThat(rec.relatedMetric()).isEqualTo("weeklyBodyFatChange");
    assertThat(rec.reason()).contains("grasa corporal");
  }

  @Test
  void positiveWhenBodyFatDownAndWeightStable() {
    Recommendation rec = only(BodyTrendRules.evaluate(summary(70.0, -0.2, -0.3, 7), NOW));

    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.INFO);
    assertThat(rec.message()).contains("mantén el plan");
  }

  @Test
  void neutralWhenNoDirectionalSignal() {
    // Weight stable, no body-fat delta available.
    Recommendation rec = only(BodyTrendRules.evaluate(summary(70.0, -0.1, null, 7), NOW));

    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.INFO);
  }

  @Test
  void thresholdIsExclusiveExactlyOnePercentIsNotExcessive() {
    // -0.7 kg over 7 days on 70 kg = exactly -1.0%/week → not flagged as excessive.
    Recommendation rec = only(BodyTrendRules.evaluate(summary(70.0, -0.7, null, 7), NOW));

    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.INFO);
  }

  @Test
  void excessiveDropTakesPrecedenceOverWorseningBodyFat() {
    // Losing too fast AND body fat rising → safety (excessive drop) wins.
    Recommendation rec = only(BodyTrendRules.evaluate(summary(70.0, -1.5, 0.4, 7), NOW));

    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.ACTION);
    assertThat(rec.relatedMetric()).isEqualTo("weeklyWeightChangeKg");
  }
}
