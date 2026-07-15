package dev.diegobarrioh.forma.domain;

/**
 * Unit of measure for a {@link ShoppingListItem}'s {@code quantity} (FOR-108).
 *
 * <p>Closed set, mirroring the {@link ShoppingCategory} pattern from FOR-106 so the UI can render a
 * consistent item line (e.g. "14 ud"). {@link #UD} is the backward-compatible default for items
 * with no unit recorded (old rows, or an item created before this field existed).
 */
public enum ShoppingUnit {
  UD,
  G,
  KG,
  L,
  PAQUETE
}
