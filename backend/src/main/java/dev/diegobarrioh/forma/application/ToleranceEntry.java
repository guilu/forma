package dev.diegobarrioh.forma.application;

/**
 * One comparison {@link PlanToleranceAuditor} made: one metric, at one scope, claimed against
 * calculated (ADR-015 slice 4, section 11).
 *
 * <p>Deliberately flat and made of primitives/enums/boxed numbers only — no nested plan types, no
 * behaviour beyond the record's own invariants — so it serialises with Jackson's defaults and needs
 * no bespoke (de)serialiser when slice 6 writes a {@link ToleranceAuditReport} into {@code
 * plan_request.validation_report} (TEXT, V63).
 *
 * @param scope {@link ToleranceScope#DAY} or {@link ToleranceScope#PLAN}
 * @param dayNumber which day (1-7) when {@link #scope} is {@link ToleranceScope#DAY}; {@code null}
 *     for a {@link ToleranceScope#PLAN} entry
 * @param metric which figure this compares
 * @param claimed what the plan states for this metric at this scope; {@code null} when nobody
 *     claimed one, in which case {@link #calculated}, {@link #difference} and {@link #toleranceAbs}
 *     describe nothing and {@link #verdict} is {@link ToleranceVerdict#NOT_CLAIMED}
 * @param calculated what {@link PlanToleranceAuditor} recomputed from the plan's items and the food
 *     catalog, regardless of whether anything was claimed
 * @param difference {@code calculated - claimed}, signed so a caller can tell overshoot from
 *     undershoot (V56 undershoots on every day: negative); {@code null} when {@link #claimed} is
 * @param toleranceAbs the absolute tolerance this comparison was judged against — {@code claimed *
 *     kcalPercentage} for {@link ToleranceMetric#KCAL}, a flat gram figure otherwise; {@code null}
 *     when {@link #claimed} is
 * @param verdict what this comparison decided
 */
public record ToleranceEntry(
    ToleranceScope scope,
    Integer dayNumber,
    ToleranceMetric metric,
    Double claimed,
    double calculated,
    Double difference,
    Double toleranceAbs,
    ToleranceVerdict verdict) {}
