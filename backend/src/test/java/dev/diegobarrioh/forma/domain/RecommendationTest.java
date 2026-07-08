package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link Recommendation} domain value (FOR-41). */
class RecommendationTest {

  private static final Instant NOW = Instant.parse("2026-07-08T10:00:00Z");

  @Test
  void createsValidRecommendationWithMessageAndReason() {
    Recommendation rec =
        new Recommendation(
            NOW,
            RecommendationCategory.BODY,
            RecommendationSeverity.INFO,
            "Peso estable esta semana.",
            "Sin cambio significativo vs. la medición anterior.",
            "weeklyWeightChangeKg");

    assertThat(rec.createdAt()).isEqualTo(NOW);
    assertThat(rec.category()).isEqualTo(RecommendationCategory.BODY);
    assertThat(rec.severity()).isEqualTo(RecommendationSeverity.INFO);
    assertThat(rec.message()).isEqualTo("Peso estable esta semana.");
    assertThat(rec.reason()).isEqualTo("Sin cambio significativo vs. la medición anterior.");
    assertThat(rec.relatedMetric()).isEqualTo("weeklyWeightChangeKg");
  }

  @Test
  void relatedMetricIsAbsentWhenNotProvided() {
    Recommendation rec =
        new Recommendation(
            NOW,
            RecommendationCategory.RECOVERY,
            RecommendationSeverity.WARNING,
            "Considera una semana de recuperación.",
            "El ritmo empeoró y la frecuencia cardíaca subió.",
            null);

    assertThat(rec.relatedMetric()).isNull();
  }

  @Test
  void blankRelatedMetricIsNormalizedToNull() {
    Recommendation rec =
        new Recommendation(
            NOW,
            RecommendationCategory.RECOVERY,
            RecommendationSeverity.INFO,
            "message",
            "reason",
            "   ");

    assertThat(rec.relatedMetric()).isNull();
  }

  @Test
  void rejectsBlankMessage() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new Recommendation(
                    NOW,
                    RecommendationCategory.BODY,
                    RecommendationSeverity.INFO,
                    "  ",
                    "reason",
                    null))
        .withMessageContaining("message");
  }

  @Test
  void rejectsBlankReason() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new Recommendation(
                    NOW,
                    RecommendationCategory.BODY,
                    RecommendationSeverity.INFO,
                    "message",
                    "",
                    null))
        .withMessageContaining("reason");
  }

  @Test
  void rejectsNullRequiredFields() {
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new Recommendation(
                    null,
                    RecommendationCategory.BODY,
                    RecommendationSeverity.INFO,
                    "message",
                    "reason",
                    null));
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new Recommendation(
                    NOW, null, RecommendationSeverity.INFO, "message", "reason", null));
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new Recommendation(
                    NOW, RecommendationCategory.BODY, null, "message", "reason", null));
  }

  @Test
  void categoryEnumHasExactlyTheJiraSet() {
    assertThat(RecommendationCategory.values())
        .containsExactly(
            RecommendationCategory.BODY,
            RecommendationCategory.TRAINING,
            RecommendationCategory.NUTRITION,
            RecommendationCategory.RECOVERY,
            RecommendationCategory.SHOPPING);
  }

  @Test
  void severityEnumHasExactlyTheExpectedSet() {
    assertThat(RecommendationSeverity.values())
        .containsExactly(
            RecommendationSeverity.INFO,
            RecommendationSeverity.WARNING,
            RecommendationSeverity.ACTION);
  }
}
