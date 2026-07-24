package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.Goal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for persisting and reading {@link Goal}s (FOR-125). Owned by the application/domain side;
 * adapters implement it (ADR-001). Every method is owner-scoped (ADR-002) — the caller always
 * supplies the owner id, the adapter never returns another owner's rows.
 *
 * <p>{@code userId} is a real account id (FOR-145b-1, migration V27) — {@code goal.user_id UUID},
 * FK-referencing {@code users(id)}, backfilled from the legacy {@code owner_id VARCHAR} column.
 */
public interface GoalRepository {

  /** All goals belonging to {@code userId}, in a stable order. Empty when none exist yet. */
  List<StoredGoal> findAllByOwner(UUID userId);

  /** Persists a new goal (with its milestones) for {@code userId}, generating and returning ids. */
  StoredGoal create(UUID userId, Goal goal);

  /**
   * Finds one of {@code userId}'s goals by id; empty if it doesn't exist or belongs to another
   * owner.
   */
  Optional<StoredGoal> findById(UUID userId, String goalId);

  /**
   * Replaces the stored goal's fields and updates its existing milestones' completion state from
   * {@code goal.milestones()} (matched by milestone id; this slice's PATCH never adds or removes
   * milestones, see {@code GoalService}). Empty if no goal with {@code goalId} exists for {@code
   * userId}.
   */
  Optional<StoredGoal> update(UUID userId, String goalId, Goal goal);
}
