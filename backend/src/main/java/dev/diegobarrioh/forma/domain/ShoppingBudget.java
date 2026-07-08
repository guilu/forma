package dev.diegobarrioh.forma.domain;

import java.math.BigDecimal;

/**
 * Estimated shopping budget (FOR-38): the weekly total and the monthly estimate, in euros.
 *
 * <p>Framework-free value produced by {@link ShoppingBudgetCalculator}. Both amounts are rounded to
 * two decimals for display.
 *
 * @param weeklyEur estimated weekly cost
 * @param monthlyEur estimated monthly cost (weekly × 4.33)
 */
public record ShoppingBudget(BigDecimal weeklyEur, BigDecimal monthlyEur) {}
