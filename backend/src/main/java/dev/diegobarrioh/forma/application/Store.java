package dev.diegobarrioh.forma.application;

/**
 * A supermarket chain, as a row rather than an enum constant (V45).
 *
 * <p>Read model for a {@code store} row. It answers only "where was this bought" — which is data,
 * and is not the same question as "can we import a catalogue from it". That second one is code: one
 * {@link StoreCatalogSource} per chain, and no row will ever conjure one. The enum this replaces
 * conflated the two, which is why CARREFOUR sat in it from V36 onwards with no adapter behind it.
 *
 * @param id the stored token every product points at — never editable, since {@code
 *     store_product.store} references it
 * @param name what a person reads
 * @param logoUrl the chain's mark, or absent. Nothing renders it yet; the column exists so the
 *     choice is stored where the chain is rather than being invented per screen
 * @param website the public storefront, or absent — {@code OTRAS} has none by definition
 * @param sortOrder where the chain sits in a list. "Otras" belongs last however the names sort
 * @param enabled whether the chain is still offered. Retiring one has to be possible without
 *     deleting it, because the products bought there keep pointing at it forever
 */
public record Store(
    String id, String name, String logoUrl, String website, int sortOrder, boolean enabled) {

  public Store {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("id must not be blank");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
  }
}
