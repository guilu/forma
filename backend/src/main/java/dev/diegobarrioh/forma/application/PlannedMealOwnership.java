package dev.diegobarrioh.forma.application;

import java.util.UUID;

/**
 * Whether a planned meal is one of this user's (V55).
 *
 * <p>A second narrow contract beside {@link DayTargetSource} rather than a wider one covering both,
 * for the same reason that one exists: the meal log needs to answer two unrelated questions of the
 * plan — what a kind of day aims for, and whether a given meal is the caller's to point at — and a
 * single port named for neither would tell a reader nothing about what it is allowed to do.
 *
 * <p>Without this, logging could attach an entry to somebody else's planned meal. The foreign key
 * would accept it: the database knows the row exists, not whose it is.
 */
@FunctionalInterface
public interface PlannedMealOwnership {

  /** Whether {@code plannedMealId} belongs to a plan owned by {@code userId}. */
  boolean ownsPlannedMeal(UUID userId, UUID plannedMealId);
}
