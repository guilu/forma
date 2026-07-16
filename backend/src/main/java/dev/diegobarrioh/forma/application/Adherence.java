package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.CategoryAdherence;
import java.time.LocalDate;
import java.util.List;

/**
 * Adherence read model (FOR-129, second implementable slice of FOR-104): planned vs completed per
 * category over a rolling window ending today. Application-level view, not persisted (spec FOR-129
 * Data Model Notes: "no domain aggregate to persist") — mirrors the {@code DayConsumption}
 * (FOR-127) / {@code GoalView} (FOR-125) pattern: computed fresh on every read by {@link
 * AdherenceService}, never stored.
 *
 * @param windowDays the requested window length in days (the {@code days} query parameter)
 * @param from the window's first (inclusive) day, {@code to.minusDays(windowDays - 1)}
 * @param to the window's last (inclusive) day — "today" as resolved by the injected {@link
 *     java.time.Clock}
 * @param categories one entry per {@link dev.diegobarrioh.forma.domain.AdherenceCategory}
 */
public record Adherence(
    int windowDays, LocalDate from, LocalDate to, List<CategoryAdherence> categories) {}
