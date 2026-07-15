package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.NutritionDayTemplate;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import dev.diegobarrioh.forma.domain.TargetComparison;
import java.time.LocalDate;
import java.util.List;

/**
 * Day consumption read model (FOR-127): consumed macros for {@code date} vs the plan target, with
 * the logged entries. Mirrors the FOR-125 {@code GoalView} pattern (an application-level view
 * distinct from both the domain aggregate and the delivery DTO, ADR-005).
 *
 * <p>{@code target}/{@code comparison} are {@code null} when the day has no resolvable plan target
 * (spec FOR-127 edge case: "Day with no plan target → return consumed totals with null/omitted
 * comparison, not an error"). In this slice they are <b>always</b> {@code null} — see {@link
 * MealLogService} javadoc for why: the plan side ({@code NutritionDayTemplate}) is keyed by {@code
 * NutritionDayType} (RUNNING/STRENGTH/REST), and no date-to-day-type schedule exists anywhere in
 * the repository yet, so a calendar date cannot be resolved to a plan target. Wiring that
 * resolution is explicitly out of scope for FOR-127 (documented discrepancy, not an oversight).
 *
 * @param date the day this read model covers
 * @param consumed the day's consumed macro totals, derived fresh from {@code entries}
 * @param target the day's plan target, or {@code null} if none can be resolved
 * @param comparison consumed-vs-target comparison, or {@code null} when {@code target} is {@code
 *     null}
 * @param entries the day's logged entries, in the order they were logged
 */
public record DayConsumption(
    LocalDate date,
    NutritionTotals consumed,
    NutritionDayTemplate target,
    TargetComparison comparison,
    List<StoredMealLogEntry> entries) {}
