package dev.diegobarrioh.forma.domain;

/**
 * The area a {@link Recommendation} applies to (FOR-41).
 *
 * <p>Follows the Jira FOR-41 set, which differs from docs/domain-model.md's {@code RUNNING}/{@code
 * STRENGTH}: here {@link #TRAINING} covers both running and strength, and {@link #BODY} covers body
 * composition. Mapping: {@code BODY} ↔ body metrics, {@code TRAINING} ↔ running + strength.
 */
public enum RecommendationCategory {
  BODY,
  TRAINING,
  NUTRITION,
  RECOVERY,
  SHOPPING
}
