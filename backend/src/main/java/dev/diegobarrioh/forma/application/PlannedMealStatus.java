package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.MealType;

/**
 * Whether a planned meal has been eaten (V55).
 *
 * <p>Derived on every read from the entries pointing at it, never stored. The source document
 * (section 10) asks for a {@code status} column holding PENDING / COMPLETED / SKIPPED; each of
 * those is a question the rows already answer, and a stored one would go stale on its own as the
 * day goes on — nobody logs anything at midnight to turn today's pending meals into skipped ones.
 *
 * @param plannedMealId the planned meal this is about
 * @param name what the plan calls it
 * @param mealType which moment of the day it is
 * @param optional whether the plan says it can be skipped
 * @param state what has become of it
 */
public record PlannedMealStatus(
    String plannedMealId, String name, MealType mealType, boolean optional, State state) {

  /** What has become of a planned meal. */
  public enum State {
    /** Something was logged against it. */
    EATEN,
    /** Nothing yet, and the day is not over. */
    PENDING,
    /** Nothing, and the day has passed. */
    SKIPPED
  }
}
