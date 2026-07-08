package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.ShoppingList;
import dev.diegobarrioh.forma.domain.ShoppingListStatus;
import java.time.LocalDate;
import java.util.List;

/**
 * A persisted weekly shopping list with its stored (id-bearing) items (FOR-39).
 *
 * @param id the stored list's id
 * @param weekStartDate the list's week start
 * @param status the list status
 * @param notes optional note
 * @param items the list's items, each with its id
 */
public record ActiveShoppingList(
    String id,
    LocalDate weekStartDate,
    ShoppingListStatus status,
    String notes,
    List<StoredShoppingListItem> items) {

  /** The domain {@link ShoppingList} (drops item ids), for budgeting and rules. */
  public ShoppingList toDomain() {
    return new ShoppingList(
        weekStartDate, status, items.stream().map(StoredShoppingListItem::item).toList(), notes);
  }
}
