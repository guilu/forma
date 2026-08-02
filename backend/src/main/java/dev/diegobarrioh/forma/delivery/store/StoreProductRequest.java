package dev.diegobarrioh.forma.delivery.store;

import dev.diegobarrioh.forma.application.CatalogStoreProduct;
import dev.diegobarrioh.forma.domain.ShoppingCategory;
import dev.diegobarrioh.forma.domain.Store;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Body accepted by the store catalog maintenance endpoints (FOR-191, admin only).
 *
 * <p>{@code priceEur} is the price of the package named by {@code packageSize} — the product's own
 * price, not a weekly cost. What a given person spends on it in a week depends on their plan and
 * belongs to their list, not to the shared catalog.
 *
 * <p>{@code foodId} is optional: a product nobody has matched to a food yet is still buyable, and
 * the column is a nullable foreign key precisely so that state can be stored.
 */
public record StoreProductRequest(
    /*
     * A slug, like the food catalog's: a stable human-readable handle that appears in URLs, not a
     * display name, and never renamed once something points at it.
     */
    @NotBlank @Pattern(regexp = "[a-z0-9-]{1,64}") String id,
    @NotNull Store store,
    @NotBlank @Size(max = 200) String name,
    @Size(max = 64) String foodId,
    @Size(max = 100) String packageSize,
    @PositiveOrZero BigDecimal priceEur,
    String url,
    ShoppingCategory category,
    String notes,
    /*
     * Carried by the client only when a product was picked off a shop's catalogue (FOR-195); it is
     * what makes the row refreshable later. Typing a product by hand leaves both absent, which is
     * the honest state — there is nothing to refresh it against.
     */
    @Size(max = 64) String externalId,
    String imageUrl) {

  /** Maps the request onto the application's own type. */
  public CatalogStoreProduct toCatalogStoreProduct() {
    return new CatalogStoreProduct(
        id,
        store,
        name,
        // Blank and absent mean the same thing here — no linked food — and a blank
        // string would fail the foreign key rather than reading as "none".
        foodId == null || foodId.isBlank() ? null : foodId,
        packageSize,
        priceEur,
        url,
        category,
        notes,
        externalId == null || externalId.isBlank() ? null : externalId,
        imageUrl);
  }
}
