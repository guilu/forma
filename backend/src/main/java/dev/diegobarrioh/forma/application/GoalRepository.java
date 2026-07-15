package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.Goal;
import java.util.List;
import java.util.Optional;

/**
 * Port for persisting and reading {@link Goal}s (FOR-125). Owned by the application/domain side;
 * adapters implement it (ADR-001). Every method is owner-scoped (ADR-002) — the caller always
 * supplies the owner id, the adapter never returns another owner's rows.
 */
public interface GoalRepository {

  /** All goals belonging to {@code ownerId}, in a stable order. Empty when none exist yet. */
  List<StoredGoal> findAllByOwner(String ownerId);

  /**
   * Persists a new goal (with its milestones) for {@code ownerId}, generating and returning ids.
   */
  StoredGoal create(String ownerId, Goal goal);

  /**
   * Finds one of {@code ownerId}'s goals by id; empty if it doesn't exist or belongs to another
   * owner.
   */
  Optional<StoredGoal> findById(String ownerId, String goalId);

  /**
   * Replaces the stored goal's fields and updates its existing milestones' completion state from
   * {@code goal.milestones()} (matched by milestone id; this slice's PATCH never adds or removes
   * milestones, see {@code GoalService}). Empty if no goal with {@code goalId} exists for {@code
   * ownerId}.
   */
  Optional<StoredGoal> update(String ownerId, String goalId, Goal goal);
}
