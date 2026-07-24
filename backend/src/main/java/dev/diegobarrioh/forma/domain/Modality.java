package dev.diegobarrioh.forma.domain;

/**
 * Training modality of a persisted catalog exercise (FOR-172, ADR-011 Decision 1).
 *
 * <p>Closed classification, mirrors {@link MovementPattern}/{@link SessionType}. Maps 1:1 to the
 * {@code exercise_catalog.modality} column. Adding a future modality (e.g. CYCLING) requires only a
 * new enum constant + seed data — no schema change (ADR-011 "Modality extensibility proof").
 */
public enum Modality {
  STRENGTH,
  RUNNING
}
