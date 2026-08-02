package dev.diegobarrioh.forma.domain;

/**
 * Which vocabulary a category belongs to (FOR-197).
 *
 * <p>Two closed sets that overlap in words and in nothing else: {@link FoodCategory} files an
 * ingredient by what it is made of, {@link ShoppingCategory} files a product by which aisle it sits
 * in. "Proteína" exists in both and means a different thing in each, so they are told apart rather
 * than merged — a single list would force one of the two screens to lie.
 */
public enum CategoryScope {
  FOOD,
  SHOPPING
}
