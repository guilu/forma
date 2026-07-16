package dev.diegobarrioh.forma.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A single logged water-intake entry (FOR-130, hydration slice of FOR-102): a volume of water
 * consumed on a given day.
 *
 * <p>Framework-free (ADR-001). Mirrors {@link MealLogEntry}'s shape and validation style: a compact
 * constructor enforces invariants at construction time (positive volume) so an invalid entry can
 * never exist in memory, not just at the API boundary — the application layer ({@link
 * dev.diegobarrioh.forma.application.HydrationService}) additionally validates before construction
 * so caller input errors surface as {@code VALIDATION_ERROR} (400) rather than an unhandled {@link
 * IllegalArgumentException}.
 *
 * @param date the day the water was consumed
 * @param volumeMl the logged volume in milliliters; must be strictly positive
 */
public record WaterIntakeEntry(LocalDate date, double volumeMl) {

  public WaterIntakeEntry {
    Objects.requireNonNull(date, "date must not be null");
    if (volumeMl <= 0) {
      throw new IllegalArgumentException("volumeMl must be strictly positive, was: " + volumeMl);
    }
  }
}
