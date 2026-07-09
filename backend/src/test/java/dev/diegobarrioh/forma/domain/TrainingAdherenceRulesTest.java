package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link TrainingAdherenceRules} evaluator (FOR-43). */
class TrainingAdherenceRulesTest {

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

  private static Recommendation only(List<Recommendation> recs) {
    assertThat(recs).hasSize(1);
    Recommendation rec = recs.get(0);
    assertThat(rec.category()).isEqualTo(RecommendationCategory.TRAINING);
    assertThat(rec.message()).isNotBlank();
    assertThat(rec.reason()).isNotBlank();
    assertThat(rec.createdAt()).isEqualTo(NOW);
    return rec;
  }

  @Test
  void highAdherenceWhenMostSessionsCompleted() {
    Recommendation rec = only(TrainingAdherenceRules.evaluate(checkIn(3, 3, 3, 3), NOW));

    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.INFO);
    assertThat(rec.message()).contains("constante");
  }

  @Test
  void lowAdherenceWhenAlmostNothingCompleted() {
    Recommendation rec = only(TrainingAdherenceRules.evaluate(checkIn(3, 0, 3, 0), NOW));

    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.ACTION);
    assertThat(rec.reason()).contains("0 de 6");
  }

  @Test
  void runningDoneStrengthMissedSuggestsStrength() {
    Recommendation rec = only(TrainingAdherenceRules.evaluate(checkIn(3, 3, 3, 0), NOW));

    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.INFO);
    assertThat(rec.message()).contains("fuerza");
  }

  @Test
  void strengthDoneRunningMissedSuggestsRunning() {
    Recommendation rec = only(TrainingAdherenceRules.evaluate(checkIn(3, 0, 3, 3), NOW));

    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.INFO);
    assertThat(rec.message()).contains("carrera");
  }

  @Test
  void noPlannedTrainingIsSafeAndNonShaming() {
    Recommendation rec = only(TrainingAdherenceRules.evaluate(checkIn(0, 0, 0, 0), NOW));

    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.INFO);
    assertThat(rec.message()).contains("No hay entrenamientos planificados");
  }

  @Test
  void nullCheckInYieldsNoPlannedTraining() {
    Recommendation rec = only(TrainingAdherenceRules.evaluate(null, NOW));

    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.INFO);
  }

  @Test
  void highThresholdIsInclusiveAtExactlyEightyPercent() {
    // 4 of 5 completed = 0.8 exactly → high adherence.
    Recommendation rec = only(TrainingAdherenceRules.evaluate(checkIn(5, 4, 0, 0), NOW));

    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.INFO);
    assertThat(rec.message()).contains("constante");
  }

  @Test
  void lowThresholdIsInclusiveAtExactlyFortyPercent() {
    // 2 of 5 completed = 0.4 exactly → low adherence.
    Recommendation rec = only(TrainingAdherenceRules.evaluate(checkIn(5, 2, 0, 0), NOW));

    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.ACTION);
  }

  @Test
  void middleAdherenceWithoutImbalanceIsSteady() {
    // 3 of 5 running completed = 0.6, no strength planned → steady note.
    Recommendation rec = only(TrainingAdherenceRules.evaluate(checkIn(5, 3, 0, 0), NOW));

    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.INFO);
    assertThat(rec.message()).contains("buen camino");
  }
}
