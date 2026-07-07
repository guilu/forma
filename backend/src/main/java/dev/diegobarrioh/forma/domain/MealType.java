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
  PRE_WORKOUT,
  POST_WORKOUT,
  DINNER
}
