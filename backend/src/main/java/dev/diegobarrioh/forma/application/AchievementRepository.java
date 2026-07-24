package dev.diegobarrioh.forma.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Port for persisting and reading earned achievements (FOR-135). Owned by the application/domain
 * side; adapters implement it (ADR-001). Every method is owner-scoped (ADR-002) — the caller always
 * supplies the owner id, the adapter never returns another owner's rows.
 *
 * <p>Only <em>earned</em> achievements are ever persisted here (spec FOR-135 Data Model Notes) —
 * the catalog itself is in-code ({@code dev.diegobarrioh.forma.domain.AchievementCatalog}), never a
 * table. There is deliberately no update/delete method: an earned achievement is never revoked
 * (spec FOR-135 Edge Cases) and its {@code earnedAt} is never rewritten once set.
 *
 * <p>{@code userId} is a real account id (FOR-145b-2, migration V28) — {@code earned_achievement}'s
 * composite primary key was rebuilt from the legacy {@code owner_id VARCHAR} column to {@code
 * user_id UUID}, FK-referencing {@code users(id)}.
 */
public interface AchievementRepository {

  /** All achievements {@code userId} has earned, in any order. Empty when none are earned yet. */
  List<EarnedAchievement> findAllByOwner(UUID userId);

  /**
   * Persists {@code achievementId} as earned by {@code userId} at {@code earnedAt}, unless it is
   * already earned. Idempotent: the {@code (user_id, achievement_id)} primary key (migration V18,
   * rebuilt by V28) guarantees a re-evaluated already-earned achievement is a no-op — never
   * duplicated, and its original {@code earnedAt} is never overwritten, including under concurrent
   * evaluation (spec FOR-135 Edge Cases: "Concurrent evaluation → PK prevents duplicates").
   *
   * @return {@code true} if this call newly persisted the award, {@code false} if it was already
   *     earned (no-op)
   */
  boolean awardIfNotEarned(UUID userId, String achievementId, Instant earnedAt);
}
