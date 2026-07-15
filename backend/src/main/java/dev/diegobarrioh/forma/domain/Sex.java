package dev.diegobarrioh.forma.domain;

/**
 * Biological sex as recorded on the user's profile (FOR-107, spec FOR-58's Ajustes mockup).
 *
 * <p>Used for body-composition context (e.g. future BMI/energy calculations), never a proxy for
 * gender identity elsewhere in the product. Closed set for the MVP; {@link #OTHER} covers callers
 * who prefer not to select {@link #MALE} or {@link #FEMALE}.
 */
public enum Sex {
  MALE,
  FEMALE,
  OTHER
}
