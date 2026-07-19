package dev.diegobarrioh.forma.domain;

import java.math.BigDecimal;

/**
 * Estimated shopping budget (FOR-38): the weekly total and the monthly estimate, in euros, plus the
 * FOR-152 cost-threshold signal (epic FOR-148 slice 4, Dashboard sheet: "Coste compra semanal ...
 * objetivo &lt;120 €/sem").
 *
 * <p>Framework-free value produced by {@link ShoppingBudgetCalculator}. Both amounts are rounded to
 * two decimals for display. {@code weeklyThresholdEur} is the single plan constant ({@value
 * ShoppingBudgetCalculator#WEEKLY_COST_THRESHOLD_EUR_TEXT} €, {@link
 * ShoppingBudgetCalculator#WEEKLY_COST_THRESHOLD_EUR}); {@code overThreshold} is {@code true} only
 * when {@code weeklyEur} is <em>strictly greater than</em> the threshold, matching the Excel's
 * "&gt;120 €" rule (exactly 120.00 stays OK) — consumed by the dashboard signal (frontend batch,
 * deferred) and by FOR-150 rule 6.
 *
 * @param weeklyEur estimated weekly cost
 * @param monthlyEur estimated monthly cost (weekly × 4.33)
 * @param weeklyThresholdEur the weekly cost threshold plan constant
 * @param overThreshold whether {@code weeklyEur} exceeds {@code weeklyThresholdEur}
 */
public record ShoppingBudget(
    BigDecimal weeklyEur,
    BigDecimal monthlyEur,
    BigDecimal weeklyThresholdEur,
    boolean overThreshold) {}
