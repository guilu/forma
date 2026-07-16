package dev.diegobarrioh.forma.application;

import java.time.LocalDate;
import java.util.List;

/**
 * Hydration progress read model (FOR-130): the day's total logged volume vs the resolved daily
 * goal, with progress (`total/goal`). Mirrors the FOR-127 {@code DayConsumption} pattern (an
 * application-level view distinct from both the domain aggregate and the delivery DTO, ADR-005).
 *
 * @param date the day this read model covers
 * @param totalMl the day's total logged volume, derived fresh from {@code entries}
 * @param goalMl the resolved daily goal (profile {@code DefaultObjectives.dailyWaterMl}, or the
 *     documented fallback default when unset); {@code null} only if the goal genuinely cannot be
 *     determined (spec FOR-130: "progress is null (not fabricated)") — not reachable today since
 *     {@link HydrationService} always applies a fallback, but kept nullable to document the
 *     contract honestly rather than fabricate a non-nullable guarantee
 * @param progress {@code totalMl / goalMl}, or {@code null} when {@code goalMl} is {@code null}
 * @param entries the day's logged entries, in the order they were logged
 */
public record HydrationProgress(
    LocalDate date,
    double totalMl,
    Double goalMl,
    Double progress,
    List<StoredWaterIntakeEntry> entries) {}
