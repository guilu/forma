package dev.diegobarrioh.forma.domain;

/**
 * Self-reported general activity level on the user's profile (FOR-107, spec FOR-58's Ajustes
 * mockup). A standard five-band scale, ordered from least to most active; future stories (nutrition
 * calorie targets) may use it as an input without changing this contract.
 */
public enum ActivityLevel {
  SEDENTARY,
  LIGHT,
  MODERATE,
  ACTIVE,
  VERY_ACTIVE
}
