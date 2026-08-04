package dev.diegobarrioh.forma.domain;

/**
 * Type of a meal within a nutrition day (FOR-31), per docs/domain-model.md.
 *
 * <p>Closed classification. New types can be added later without breaking the contract, as with
 * {@link MealType} siblings across the domain.
 */
public enum MealType {
  BREAKFAST,
  MID_MORNING,
  LUNCH,
  /**
   * The merienda. Added by V53: the source plan document lists it and this enum did not have it, so
   * an afternoon meal had no honest type — {@link #MID_MORNING} is the wrong half of the day and
   * {@link #PRE_WORKOUT} says something about training that a merienda does not.
   */
  SNACK,
  PRE_WORKOUT,
  POST_WORKOUT,
  DINNER
}
