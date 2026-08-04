package dev.diegobarrioh.forma.domain;

/**
 * Where a nutrition plan sits in its life (V53).
 *
 * <p>Only one of these is constrained by the database: at most one {@link #ACTIVE} plan per user,
 * enforced by the {@code active_marker} sentinel V53 introduced (ADR-011). The rest are ordinary
 * states a plan passes through, and nothing stops a user from keeping any number of them.
 */
public enum PlanStatus {
  /** Being written. Not what anybody is following. */
  DRAFT,
  /** What this user is following now. At most one per user. */
  ACTIVE,
  /** Ran its course. Kept because what somebody followed for eight weeks is worth reading back. */
  COMPLETED,
  /** Put away without having been finished. */
  ARCHIVED;

  /**
   * The {@code active_marker} value this status implies: {@code '1'} for {@link #ACTIVE}, else
   * null.
   */
  public String marker() {
    return this == ACTIVE ? "1" : null;
  }
}
