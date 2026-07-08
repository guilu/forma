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
 */
public final class ShoppingBudgetCalculator {

  static final String MONTHLY_FACTOR_TEXT = "4.33";
  private static final BigDecimal MONTHLY_FACTOR = new BigDecimal(MONTHLY_FACTOR_TEXT);

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
    return new ShoppingBudget(weeklyRounded, monthly);
  }
}
