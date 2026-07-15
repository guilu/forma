package dev.diegobarrioh.forma.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * A personal goal with milestones (FOR-125): first implementable slice of FOR-104's progress/ goals
 * domain. Unblocks the Objetivos screen (FOR-122).
 *
 * <p>Framework-free (ADR-001) — no Spring, JDBC or HTTP types. Carries no identity, following the
 * {@code ShoppingProduct} precedent (FOR-35): persistence generates and owns the id (FOR-125 {@code
 * GoalRepository}). Owner-scoping (ADR-002) also lives outside this type, at the
 * application/persistence boundary, exactly like {@code UserProfile}'s {@code ownerId} — a Goal
 * itself is not "who owns it", the repository call is.
 *
 * <p>Progress toward {@code target} is deliberately not a field here: it is derived on demand (see
 * {@link GoalProgress}) from real source data, never stored, so it can never drift out of sync with
 * the underlying measurements (spec FOR-125 Data Model Notes).
 *
 * @param title short label (e.g. "Bajar a 12% grasa"); required, non-blank
 * @param metric the dimension this goal tracks; required
 * @param target the target value in the metric's unit; required, finite
 * @param dueDate optional target date
 * @param status lifecycle status; defaults to {@link GoalStatus#ACTIVE} when {@code null}
 * @param milestones ordered checkpoints toward {@code target}; required, may be empty
 */
public record Goal(
    String title,
    GoalMetric metric,
    double target,
    LocalDate dueDate,
    GoalStatus status,
    List<Milestone> milestones) {

  public Goal {
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("title must not be blank");
    }
    if (metric == null) {
      throw new IllegalArgumentException("metric must not be null");
    }
    if (Double.isNaN(target) || Double.isInfinite(target)) {
      throw new IllegalArgumentException("target must be a finite number, was: " + target);
    }
    if (milestones == null) {
      throw new IllegalArgumentException("milestones must not be null");
    }
    milestones = List.copyOf(milestones);
    if (status == null) {
      status = GoalStatus.ACTIVE;
    }
  }
}
