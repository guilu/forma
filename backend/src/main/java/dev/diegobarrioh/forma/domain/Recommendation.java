package dev.diegobarrioh.forma.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * An explainable suggestion produced by the Insights engine (FOR-41): a {@link #category} and
 * {@link #severity}, a short {@link #message}, and a {@link #reason} that references the observed
 * data behind it. Framework-free domain value (ADR-001); the shared output of the FOR-42/43/44
 * rules and the FOR-45 API.
 *
 * <p>Explainability is core: every recommendation carries <em>both</em> a message and a reason, so
 * a suggestion is never shown without the observation that justifies it. Copy is neutral and
 * non-alarming, with no fake precision and no medical claims (docs/ui-guidelines.md).
 *
 * <p>The {@code relatedMetric} is an optional light reference — a short label such as {@code
 * "weeklyWeightChangeKg"} — not a full domain object; it is {@code null} when none, and a blank
 * value is normalized to {@code null} (spec FOR-41 Open Questions). Computed on demand by the
 * rules; no persistence (FOR-45 exposes them).
 *
 * @param createdAt when the recommendation was produced; required
 * @param category the area it applies to; required (see {@link RecommendationCategory})
 * @param severity how strongly it is expressed; required (see {@link RecommendationSeverity})
 * @param message short, clear, non-alarming suggestion; required, non-blank
 * @param reason the observed data justifying it; required, non-blank
 * @param relatedMetric optional light metric reference, or {@code null} when none
 */
public record Recommendation(
    Instant createdAt,
    RecommendationCategory category,
    RecommendationSeverity severity,
    String message,
    String reason,
    String relatedMetric) {

  public Recommendation {
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    Objects.requireNonNull(category, "category must not be null");
    Objects.requireNonNull(severity, "severity must not be null");
    if (message == null || message.isBlank()) {
      throw new IllegalArgumentException("message must not be blank");
    }
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("reason must not be blank");
    }
    relatedMetric = (relatedMetric == null || relatedMetric.isBlank()) ? null : relatedMetric;
  }
}
