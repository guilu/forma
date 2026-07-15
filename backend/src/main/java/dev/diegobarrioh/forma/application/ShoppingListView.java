package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.ShoppingBudget;
import dev.diegobarrioh.forma.domain.ShoppingCategory;
import dev.diegobarrioh.forma.domain.ShoppingListStatus;
import dev.diegobarrioh.forma.domain.ShoppingUnit;
import java.math.BigDecimal;
import java.time.Instant;
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
 * @param generatedAt when this list was generated/created (FOR-108)
 */
public record ShoppingListView(
    LocalDate weekStartDate,
    ShoppingListStatus status,
    List<Entry> items,
    ShoppingBudget budget,
    Instant generatedAt) {

  /**
   * One checklist entry.
   *
   * @param id the item id (used to toggle checked)
   * @param productId the FOR-36 product id (FOR-106) — lets the UI resolve product edits by id
   * @param productName resolved product name
   * @param category resolved product category (FOR-106); {@link ShoppingCategory#OTROS} when the
   *     product id no longer resolves, mirroring the {@code productName} fallback
   * @param quantity units/packages
   * @param estimatedCostEur stored line cost
   * @param checked checked state
   * @param unit unit of measure for {@code quantity} (FOR-108)
   * @param servings number of servings this line represents (FOR-108); {@code null} when the
   *     resolved product is not linked to a nutrition food, mirroring the {@code category} fallback
   *     — never fabricated for non-food items
   */
  public record Entry(
      String id,
      String productId,
      String productName,
      ShoppingCategory category,
      int quantity,
      BigDecimal estimatedCostEur,
      boolean checked,
      ShoppingUnit unit,
      Integer servings) {}
}
