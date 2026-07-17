package dev.diegobarrioh.forma.application;

import java.time.Instant;
import java.util.List;

/**
 * Port for persisting and reading earned achievements (FOR-135). Owned by the application/domain
 * side; adapters implement it (ADR-001). Every method is owner-scoped (ADR-002) — the caller always
 * supplies the owner id, the adapter never returns another owner's rows.
 *
 * <p>Only <em>earned</em> achievements are ever persisted here (spec FOR-135 Data Model Notes) —
 * the catalog itself is in-code ({@code dev.diegobarrioh.forma.domain.AchievementCatalog}), never a
 * table. There is deliberately no update/delete method: an earned achievement is never revoked
 * (spec FOR-135 Edge Cases) and its {@code earnedAt} is never rewritten once set.
 */
public interface AchievementRepository {

  /** All achievements {@code ownerId} has earned, in any order. Empty when none are earned yet. */
  List<EarnedAchievement> findAllByOwner(String ownerId);

  /**
   * Persists {@code achievementId} as earned by {@code ownerId} at {@code earnedAt}, unless it is
   * already earned. Idempotent: the {@code (owner_id, achievement_id)} primary key (migration V18)
   * guarantees a re-evaluated already-earned achievement is a no-op — never duplicated, and its
   * original {@code earnedAt} is never overwritten, including under concurrent evaluation (spec
   * FOR-135 Edge Cases: "Concurrent evaluation → PK prevents duplicates").
   *
   * @return {@code true} if this call newly persisted the award, {@code false} if it was already
   *     earned (no-op)
   */
  boolean awardIfNotEarned(String ownerId, String achievementId, Instant earnedAt);
}
