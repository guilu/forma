package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.NutritionDayTemplate;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import dev.diegobarrioh.forma.domain.TargetComparison;
import java.time.LocalDate;
import java.util.List;

/**
 * Day consumption read model (FOR-127, target/comparison wired by FOR-128): consumed macros for
 * {@code date} vs the plan target, with the logged entries. Mirrors the FOR-125 {@code GoalView}
 * pattern (an application-level view distinct from both the domain aggregate and the delivery DTO,
 * ADR-005).
 *
 * <p>{@code dayType} is always resolved from {@code date} (FOR-128 {@code
 * NutritionDayTypeResolver}, reusing the shared {@code WeeklyTrainingDayPolicy} — no duplicated
 * policy). {@code target}/{@code comparison} are derived from that day type's {@code
 * NutritionDayTemplate} and are {@code null} only if the (closed, always-seeded) catalog has no
 * template for the resolved type — a fail-safe that should not happen in practice, not a crash
 * (spec FOR-127/FOR-128 edge case: "Day with no plan target → return consumed totals with
 * null/omitted comparison, not an error").
 *
 * @param date the day this read model covers
 * @param dayType the date's resolved {@link NutritionDayType} (FOR-128)
 * @param consumed the day's consumed macro totals, derived fresh from {@code entries}
 * @param target the day type's plan target, or {@code null} if none can be resolved
 * @param comparison consumed-vs-target comparison, or {@code null} when {@code target} is {@code
 *     null}
 * @param entries the day's logged entries, in the order they were logged
 */
public record DayConsumption(
    LocalDate date,
    NutritionDayType dayType,
    NutritionTotals consumed,
    NutritionDayTemplate target,
    TargetComparison comparison,
    List<StoredMealLogEntry> entries) {}
