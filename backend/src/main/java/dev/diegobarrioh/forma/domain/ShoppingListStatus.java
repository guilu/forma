package dev.diegobarrioh.forma.domain;

/**
 * Status of a weekly {@link ShoppingList} (FOR-37).
 *
 * <p>Closed classification. New values can be added later without breaking the contract.
 *
 * <ul>
 *   <li>{@link #DRAFT} — being prepared.
 *   <li>{@link #ACTIVE} — the current week's list.
 *   <li>{@link #DONE} — completed / archived.
 * </ul>
 */
public enum ShoppingListStatus {
  DRAFT,
  ACTIVE,
  DONE
}
