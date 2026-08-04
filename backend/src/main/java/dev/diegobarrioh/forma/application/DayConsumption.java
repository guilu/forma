package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.KeyNutrientTotals;
import dev.diegobarrioh.forma.domain.MacroTargets;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import dev.diegobarrioh.forma.domain.TargetComparison;
import java.time.LocalDate;
import java.util.List;

/**
 * Day consumption read model (FOR-127, target/comparison wired by FOR-128, key nutrients by
 * FOR-134): consumed macros for {@code date} vs the plan target, with the logged entries. Mirrors
 * the FOR-125 {@code GoalView} pattern (an application-level view distinct from both the domain
 * aggregate and the delivery DTO, ADR-005).
 *
 * <p>{@code dayType} is always resolved from {@code date} (FOR-128 {@code
 * NutritionDayTypeResolver}, reusing the shared {@code WeeklyTrainingDayPolicy} — no duplicated
 * policy). {@code target}/{@code comparison} come from the user's ACTIVE PLAN (V53/V54) since that
 * plan stopped being three constants in Java: the day of that kind in the plan, with its targets
 * resolved down the day → plan → profile chain. Both are {@code null} when the user has no active
 * plan, or has one whose targets nobody has completed — which is a real state now rather than the
 * fail-safe it used to be, and the documented answer to it stays the same (spec FOR-127/FOR-128
 * edge case: "Day with no plan target → return consumed totals with null/omitted comparison, not an
 * error").
 *
 * @param date the day this read model covers
 * @param dayType the date's resolved {@link NutritionDayType} (FOR-128)
 * @param consumed the day's consumed macro totals, derived fresh from {@code entries}
 * @param keyNutrients the day's consumed key-nutrient totals (FOR-134), derived fresh from {@code
 *     entries} via {@code MealLog#consumedKeyNutrients} — zeroed (never null) for an empty day, and
 *     otherwise null per-nutrient if any contributing entry lacks that nutrient (documented rule)
 * @param target the effective target for that kind of day, or {@code null} when the user has no
 *     active plan or nobody has set all four macros anywhere down the chain
 * @param comparison consumed-vs-target comparison, or {@code null} when {@code target} is {@code
 *     null}
 * @param entries the day's logged entries, in the order they were logged
 */
public record DayConsumption(
    LocalDate date,
    NutritionDayType dayType,
    NutritionTotals consumed,
    KeyNutrientTotals keyNutrients,
    MacroTargets target,
    TargetComparison comparison,
    List<StoredMealLogEntry> entries) {}
