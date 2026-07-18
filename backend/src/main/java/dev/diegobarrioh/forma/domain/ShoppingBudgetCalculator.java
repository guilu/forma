package dev.diegobarrioh.forma.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Computes the weekly and monthly shopping budget for a {@link ShoppingList} (FOR-38).
 *
 * <p>Pure, deterministic domain calculation (mirroring the FOR-21/FOR-28/FOR-32 precedents). The
 * weekly total is <em>derived</em> from the current product prices (unit price × item quantity),
 * summed — so the budget updates when a product price or an item quantity changes (spec FOR-38).
 * The monthly estimate is weekly × {@value #MONTHLY_FACTOR_TEXT} (weeks per month). Amounts sum raw
 * and are rounded once to two decimals.
 *
 * <p>An item whose product has no known price contributes zero (documented rule), so a missing
 * price never inflates the budget.
 *
 * <p><b>Cost threshold (FOR-152).</b> Every computed budget also carries the plan's single &lt;120
 * €/week cost threshold (epic FOR-148 slice 4, {@code docs/fitness_os.xlsm} sheet Dashboard: "Coste
 * compra semanal ... objetivo &lt;120 €/sem") and an {@code overThreshold} signal — {@code true}
 * only when the weekly total is <em>strictly greater than</em> the threshold, per the Excel's own
 * "&gt;120 €" rule (exactly 120.00 stays OK, matching specs/FOR-150's documented boundary). This
 * keeps the cost-vs-threshold comparison in the domain (ADR-001), not the UI or FOR-150's insight
 * rules, which only read the resulting flag.
 */
public final class ShoppingBudgetCalculator {

  static final String MONTHLY_FACTOR_TEXT = "4.33";
  private static final BigDecimal MONTHLY_FACTOR = new BigDecimal(MONTHLY_FACTOR_TEXT);

  /** Text form of {@link #WEEKLY_COST_THRESHOLD_EUR}, for javadoc {@code @value} substitution. */
  static final String WEEKLY_COST_THRESHOLD_EUR_TEXT = "120.00";

  /** The plan's single weekly cost threshold (FOR-152, Dashboard sheet target: "&lt;120 €/sem"). */
  public static final BigDecimal WEEKLY_COST_THRESHOLD_EUR =
      new BigDecimal(WEEKLY_COST_THRESHOLD_EUR_TEXT);

  private ShoppingBudgetCalculator() {}

  /**
   * Budget for a list given the current unit price per product id (e.g. product {@code
   * estimatedPriceEur}). Missing prices count as zero.
   */
  public static ShoppingBudget budget(ShoppingList list, Map<String, BigDecimal> unitPriceById) {
    BigDecimal weekly =
        list.items().stream()
            .map(
                item ->
                    unitPriceById
                        .getOrDefault(item.productId(), BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(item.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal weeklyRounded = weekly.setScale(2, RoundingMode.HALF_UP);
    BigDecimal monthly = weekly.multiply(MONTHLY_FACTOR).setScale(2, RoundingMode.HALF_UP);
    boolean overThreshold = weeklyRounded.compareTo(WEEKLY_COST_THRESHOLD_EUR) > 0;
    return new ShoppingBudget(weeklyRounded, monthly, WEEKLY_COST_THRESHOLD_EUR, overThreshold);
  }
}
