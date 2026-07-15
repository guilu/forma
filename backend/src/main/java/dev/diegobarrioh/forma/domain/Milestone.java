package dev.diegobarrioh.forma.domain;

/**
 * A checkpoint on the way to a {@link Goal}'s target (FOR-125).
 *
 * <p>Framework-free (ADR-001). Unlike {@code ShoppingListItem}/{@code ShoppingProduct} (FOR-35/
 * FOR-37), which carry no identity because they have their own independent CRUD endpoints, a
 * milestone is only ever read or written together with its parent {@link Goal} — {@code PATCH
 * /api/v1/goals/{id}} addresses a specific milestone by id within the same request (spec FOR-125
 * api.md). So {@code id} lives directly on this value object rather than behind a separate {@code
 * StoredMilestone} wrapper: {@code null} before persistence assigns one, set once the milestone has
 * been saved.
 *
 * <p>Completion is always user-set (spec FOR-125 Open Questions: "default to user-set unless the
 * derivation is trivial" — no trivial derivation exists here, so this slice never auto-completes a
 * milestone from progress).
 *
 * @param id the persisted milestone's id, or {@code null} before it has been saved
 * @param title short label (e.g. "15%"); required, non-blank
 * @param target the milestone's target value in the goal's metric unit; required, finite
 * @param completed whether the user has marked this milestone complete
 */
public record Milestone(String id, String title, double target, boolean completed) {

  public Milestone {
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("title must not be blank");
    }
    if (Double.isNaN(target) || Double.isInfinite(target)) {
      throw new IllegalArgumentException("target must be a finite number, was: " + target);
    }
  }

  /** Returns a copy of this milestone with the given completion state. */
  public Milestone withCompleted(boolean value) {
    return new Milestone(id, title, target, value);
  }
}
