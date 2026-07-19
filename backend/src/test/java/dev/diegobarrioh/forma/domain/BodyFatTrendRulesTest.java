package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link BodyFatTrendRules} evaluator (FOR-150 rule 2): sustained 2-week
 * body-fat rise, sourced from the FOR-155 {@link WeeklyTrackingRecord} history (spec FOR-150 Data
 * Model Notes: "arrives via FOR-155's weekly records").
 */
class BodyFatTrendRulesTest {

  private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");

  private static WeeklyTrackingRecord record(int week, Double bodyFatPercentage) {
    return new WeeklyTrackingRecord(
        week,
        LocalDate.of(2026, 1, 1).plusWeeks(week),
        null,
        bodyFatPercentage,
        null,
        null,
        null,
        null,
        null);
  }

  @Test
  void sustainedTwoWeekRiseFires() {
    // Weeks 1, 2, 3 (consecutive): body fat rises each week -> sustained trend.
    List<Recommendation> recs =
        BodyFatTrendRules.evaluate(List.of(record(3, 20.0), record(2, 19.0), record(1, 18.0)), NOW);

    assertThat(recs).hasSize(1);
    Recommendation rec = recs.get(0);
    assertThat(rec.category()).isEqualTo(RecommendationCategory.BODY);
    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.ACTION);
    assertThat(rec.message()).contains("100 kcal");
    assertThat(rec.reason()).isNotBlank();
    assertThat(rec.relatedMetric()).isEqualTo("bodyFatPercentage");
    assertThat(rec.createdAt()).isEqualTo(NOW);
  }

  @Test
  void singleWeekRiseDoesNotFire() {
    List<Recommendation> recs =
        BodyFatTrendRules.evaluate(List.of(record(2, 19.0), record(1, 18.0)), NOW);

    assertThat(recs).isEmpty();
  }

  @Test
  void nonConsecutiveWeeksDoesNotFire() {
    // Gap between week 5 and week 2 -> not "seguidas" (in a row).
    List<Recommendation> recs =
        BodyFatTrendRules.evaluate(List.of(record(5, 20.0), record(2, 19.0), record(1, 18.0)), NOW);

    assertThat(recs).isEmpty();
  }

  @Test
  void missingBodyFatValueBreaksTheChain() {
    List<Recommendation> recs =
        BodyFatTrendRules.evaluate(List.of(record(3, 20.0), record(2, null), record(1, 18.0)), NOW);

    assertThat(recs).isEmpty();
  }

  @Test
  void riseThenFallDoesNotFire() {
    // week1 -> week2 rises, week2 -> week3 falls: not two consecutive rises.
    List<Recommendation> recs =
        BodyFatTrendRules.evaluate(List.of(record(3, 19.0), record(2, 20.0), record(1, 18.0)), NOW);

    assertThat(recs).isEmpty();
  }

  @Test
  void fewerThanThreeRecordsDoesNotFire() {
    List<Recommendation> recs = BodyFatTrendRules.evaluate(List.of(record(1, 18.0)), NOW);

    assertThat(recs).isEmpty();
  }

  @Test
  void emptyListDoesNotFire() {
    assertThat(BodyFatTrendRules.evaluate(List.of(), NOW)).isEmpty();
  }

  @Test
  void nullListDoesNotFire() {
    assertThat(BodyFatTrendRules.evaluate(null, NOW)).isEmpty();
  }

  @Test
  void unorderedInputIsSortedByWeekBeforeEvaluating() {
    // Same data as sustainedTwoWeekRiseFires, but passed out of order.
    List<Recommendation> recs =
        BodyFatTrendRules.evaluate(List.of(record(1, 18.0), record(3, 20.0), record(2, 19.0)), NOW);

    assertThat(recs).hasSize(1);
  }
}
