package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.ShoppingBudget;
import dev.diegobarrioh.forma.domain.ShoppingListStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A fully-resolved read model of the weekly shopping list (FOR-39): items with product names, plus
 * the computed budget. Product ids are resolved to names here so the delivery layer maps 1:1.
 *
 * @param weekStartDate the list's week start
 * @param status the list status
 * @param items resolved item entries
 * @param budget the weekly + monthly budget
 */
public record ShoppingListView(
    LocalDate weekStartDate, ShoppingListStatus status, List<Entry> items, ShoppingBudget budget) {

  /**
   * One checklist entry.
   *
   * @param id the item id (used to toggle checked)
   * @param productName resolved product name
   * @param quantity units/packages
   * @param estimatedCostEur stored line cost
   * @param checked checked state
   */
  public record Entry(
      String id, String productName, int quantity, BigDecimal estimatedCostEur, boolean checked) {}
}
