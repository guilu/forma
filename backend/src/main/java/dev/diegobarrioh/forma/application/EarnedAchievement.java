package dev.diegobarrioh.forma.application;

import java.time.Instant;

/**
 * A persisted fact: {@code achievementId} (a {@code dev.diegobarrioh.forma.domain.Achievement}
 * catalog id) was earned by the owner at {@code earnedAt} (FOR-135). Owner-scoping (ADR-002) lives
 * outside this type, at the repository boundary, exactly like {@code Goal}/{@code StoredGoal}
 * (FOR-125) — every {@link AchievementRepository} method takes the owner id explicitly.
 *
 * <p>Achievements are never revoked (spec FOR-135 Edge Cases: "A rule whose underlying data was
 * later deleted → the earned achievement stays earned") and never re-awarded — {@code earnedAt} is
 * set exactly once, the first time the rule is met, and is immutable thereafter (enforced by the
 * {@code earned_achievement} table's {@code (owner_id, achievement_id)} primary key, V18).
 *
 * @param achievementId the catalog achievement's stable id
 * @param earnedAt when this achievement was first earned
 */
public record EarnedAchievement(String achievementId, Instant earnedAt) {

  public EarnedAchievement {
    if (achievementId == null || achievementId.isBlank()) {
      throw new IllegalArgumentException("achievementId must not be blank");
    }
    if (earnedAt == null) {
      throw new IllegalArgumentException("earnedAt must not be null");
    }
  }
}
