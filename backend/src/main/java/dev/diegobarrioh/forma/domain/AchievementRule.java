package dev.diegobarrioh.forma.domain;

/**
 * A deterministic condition over existing data that decides whether an {@link Achievement} is met
 * (FOR-135). Framework-free (ADR-001), pure — the same {@link AchievementData} always yields the
 * same result, which is what makes an award explainable/auditable from source data (spec FOR-135
 * NFR "Explainable/auditable").
 */
@FunctionalInterface
public interface AchievementRule {

  /** Whether this rule's condition is currently met by {@code data}. */
  boolean isMet(AchievementData data);
}
