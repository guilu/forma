package dev.diegobarrioh.forma.domain;

/**
 * Grocery aisle classification for a {@link ShoppingProduct} (FOR-106).
 *
 * <p>Closed set so the UI can reliably group/filter the shopping list by category. {@link #OTROS}
 * is the backward-compatible default for products with no category set (old rows, or a product
 * created before this field existed).
 */
public enum ShoppingCategory {
  FRUTAS_Y_VERDURAS,
  PROTEINAS,
  LACTEOS_Y_HUEVOS,
  CEREALES_Y_LEGUMBRES,
  GRASAS_Y_ACEITES,
  OTROS
}
