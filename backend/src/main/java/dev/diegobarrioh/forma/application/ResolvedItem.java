package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.NutritionTotals;

/**
 * One line of a planned meal, worked out against today's catalog (V53/V54).
 *
 * <p>{@link #grams} and {@link #totals} are computed on every read, never stored. That is the whole
 * point of the plan holding an {@code amount} and a portion rather than a frozen weight: a portion
 * corrected from 120 g to 125 g moves every plan that says "one medium banana", and a food whose
 * macros are corrected moves every plan that uses it.
 *
 * @param label what to call it — the food's name, or the dish's
 * @param grams how much it works out to
 * @param totals what it works out to, per today's catalog
 * @param optional whether this line can be skipped
 * @param notes free text from the plan ("a la plancha")
 * @param unresolved the id that could not be found, or null when everything resolved. A line whose
 *     food has gone is carried rather than thrown: the foreign key should make it impossible, and
 *     if it ever happens a day with one bad line should still render the rest
 */
public record ResolvedItem(
    String label,
    double grams,
    NutritionTotals totals,
    boolean optional,
    String notes,
    String unresolved) {}
