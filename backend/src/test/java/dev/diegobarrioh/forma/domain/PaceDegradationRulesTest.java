package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link PaceDegradationRules} evaluator (FOR-150 rule 4).
 *
 * <p>Reframed per the resolved design decision: the original Excel rule ("mismo ritmo, más ppm")
 * needed a heart-rate signal the app deliberately does not capture (see {@link
 * WeeklyTrackingRecord} javadoc, "Heart-rate field"). This rule instead uses the pace itself:
 * running the same 4 km distance slower, week over week, is read as a fatigue/recovery signal.
 */
class PaceDegradationRulesTest {

  private static final Instant NOW = Instant.parse("2026-07-09T08:00:00Z");

  private static WeeklyTrackingRecord record(int week, String pace) {
    return new WeeklyTrackingRecord(
        week, LocalDate.of(2026, 1, 1).plusWeeks(week), null, null, null, null, pace, null, null);
  }

  @Test
  void slowerPaceWeekOverWeekFires() {
    List<Recommendation> recs =
        PaceDegradationRules.evaluate(List.of(record(2, "5:45"), record(1, "5:30")), NOW);

    assertThat(recs).hasSize(1);
    Recommendation rec = recs.get(0);
    assertThat(rec.category()).isEqualTo(RecommendationCategory.RECOVERY);
    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.WARNING);
    assertThat(rec.message()).isNotBlank();
    assertThat(rec.reason()).contains("5:30").contains("5:45");
    assertThat(rec.relatedMetric()).isEqualTo("pace4kmMinPerKm");
    assertThat(rec.createdAt()).isEqualTo(NOW);
  }

  @Test
  void fasterPaceDoesNotFire() {
    List<Recommendation> recs =
        PaceDegradationRules.evaluate(List.of(record(2, "5:30"), record(1, "5:45")), NOW);

    assertThat(recs).isEmpty();
  }

  @Test
  void samePaceDoesNotFire() {
    List<Recommendation> recs =
        PaceDegradationRules.evaluate(List.of(record(2, "5:30"), record(1, "5:30")), NOW);

    assertThat(recs).isEmpty();
  }

  @Test
  void fewerThanTwoPaceBearingRecordsDoesNotFire() {
    List<Recommendation> recs =
        PaceDegradationRules.evaluate(List.of(record(2, null), record(1, "5:30")), NOW);

    assertThat(recs).isEmpty();
  }

  @Test
  void emptyListDoesNotFire() {
    assertThat(PaceDegradationRules.evaluate(List.of(), NOW)).isEmpty();
  }

  @Test
  void nullListDoesNotFire() {
    assertThat(PaceDegradationRules.evaluate(null, NOW)).isEmpty();
  }

  @Test
  void usesTheTwoMostRecentPaceBearingRecordsEvenAcrossAGap() {
    // Weeks 1 and 5 (gap in between, e.g. week 3 has no pace recorded) -> still compared,
    // since rule 4 (unlike rule 2) does not require calendar-adjacent weeks.
    List<Recommendation> recs =
        PaceDegradationRules.evaluate(
            List.of(record(5, "6:00"), record(3, null), record(1, "5:30")), NOW);

    assertThat(recs).hasSize(1);
  }
}
