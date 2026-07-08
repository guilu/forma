package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link ShoppingBudgetCalculator} (FOR-38): weekly sum, monthly ×4.33, empty
 * list, missing price, and reaction to price/quantity changes. Plain JUnit 5 + AssertJ (ADR-007).
 */
class ShoppingBudgetCalculatorTest {

  private static ShoppingListItem item(String productId, int quantity) {
    return new ShoppingListItem(productId, quantity, new BigDecimal("0.00"), false);
  }

  private static ShoppingList list(ShoppingListItem... items) {
    return new ShoppingList(
        LocalDate.of(2026, 7, 6), ShoppingListStatus.ACTIVE, List.of(items), null);
  }

  @Test
  void computesWeeklyAndMonthlyFromProductPrices() {
    ShoppingList list = list(item("p1", 2), item("p2", 1));
    Map<String, BigDecimal> prices =
        Map.of("p1", new BigDecimal("1.95"), "p2", new BigDecimal("3.90"));

    ShoppingBudget budget = ShoppingBudgetCalculator.budget(list, prices);

    // weekly = 1.95*2 + 3.90*1 = 7.80; monthly = 7.80 * 4.33 = 33.774 -> 33.77
    assertThat(budget.weeklyEur()).isEqualByComparingTo("7.80");
    assertThat(budget.monthlyEur()).isEqualByComparingTo("33.77");
  }

  @Test
  void emptyListIsZero() {
    ShoppingBudget budget = ShoppingBudgetCalculator.budget(list(), Map.of());

    assertThat(budget.weeklyEur()).isEqualByComparingTo("0.00");
    assertThat(budget.monthlyEur()).isEqualByComparingTo("0.00");
  }

  @Test
  void anItemWithNoKnownPriceContributesZero() {
    ShoppingList list = list(item("p1", 2), item("no-price", 5));
    Map<String, BigDecimal> prices = Map.of("p1", new BigDecimal("1.95"));

    // Only p1 counts: 1.95 * 2 = 3.90.
    assertThat(ShoppingBudgetCalculator.budget(list, prices).weeklyEur())
        .isEqualByComparingTo("3.90");
  }

  @Test
  void reflectsPriceChanges() {
    ShoppingList list = list(item("p1", 2), item("p2", 1));

    BigDecimal cheaper =
        ShoppingBudgetCalculator.budget(
                list, Map.of("p1", new BigDecimal("1.95"), "p2", new BigDecimal("3.90")))
            .weeklyEur();
    BigDecimal pricier =
        ShoppingBudgetCalculator.budget(
                list, Map.of("p1", new BigDecimal("2.50"), "p2", new BigDecimal("3.90")))
            .weeklyEur();

    assertThat(cheaper).isEqualByComparingTo("7.80");
    assertThat(pricier).isEqualByComparingTo("8.90");
  }
}
