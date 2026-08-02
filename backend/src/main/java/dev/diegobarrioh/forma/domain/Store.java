package dev.diegobarrioh.forma.domain;

/**
 * A supermarket chain a {@link ShoppingCategory} product can be bought from (FOR-191).
 *
 * <p>Closed set, mirrored by the {@code chk_store_product_store} check in V36. One catalog table
 * holds every chain and this column separates them: the columns of a product are the same wherever
 * it is sold, so a table per chain would duplicate the schema and every query over it. Adding a
 * chain is a constant here and an edit to that check.
 */
public enum Store {
  MERCADONA,
  CARREFOUR
}
