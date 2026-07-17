package dev.diegobarrioh.forma.domain;

/**
 * One entry in the closed {@link AchievementCatalog} (FOR-135): a stable id, display copy, and the
 * deterministic {@link AchievementRule} that decides when it is earned. Framework-free (ADR-001).
 *
 * <p>Carries no {@code earnedAt} or owner — that is the persisted, per-owner fact ({@code
 * EarnedAchievement}, application layer), not catalog metadata. Mirrors {@code FoodItem}'s
 * in-code-catalog-entry shape (FOR-30): a stable id, never renumbered, referenced by {@code
 * earned_achievement.achievement_id} once persisted.
 *
 * @param id stable, unique catalog id (e.g. {@code "FIRST_MEASUREMENT"}); never renumbered once
 *     shipped, since it is the persisted foreign key in {@code earned_achievement}
 * @param title short display title (Spanish, matching the mockups' "logros" copy)
 * @param description short display description
 * @param rule the deterministic condition that decides whether this achievement is met
 */
public record Achievement(String id, String title, String description, AchievementRule rule) {

  public Achievement {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("id must not be blank");
    }
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("title must not be blank");
    }
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("description must not be blank");
    }
    if (rule == null) {
      throw new IllegalArgumentException("rule must not be null");
    }
  }
}
