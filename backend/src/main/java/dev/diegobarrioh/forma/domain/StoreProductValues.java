package dev.diegobarrioh.forma.domain;

import java.math.BigDecimal;

/**
 * The catalog side of a {@link ShoppingProduct} reference (FOR-192): what {@code store_product}
 * says about a product, as far as a user's shopping entry cares.
 *
 * <p>A read model for {@link ShoppingProduct#resolveWith}, not the catalog's own type — the
 * Shopping context needs a name, a price, a package, a link and an aisle, and nothing else the
 * catalog holds. Keeping it here means the "override wins, else catalog" rule stays in the domain
 * rather than in whichever adapter happens to do the join.
 */
public record StoreProductValues(
    String name,
    String url,
    String packageSize,
    BigDecimal priceEur,
    String foodId,
    ShoppingCategory category,
    String notes) {}
