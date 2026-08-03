package dev.diegobarrioh.forma.application;

/**
 * One aisle of one shop, as a row (V46).
 *
 * <p>Read model for a {@code store_category} row. Not to be confused with {@link
 * dev.diegobarrioh.forma.domain.ShoppingCategory}: that is one of OUR six aisles, chosen by an
 * admin and the same words whichever shop a product came from. This is what the SHOP calls the
 * shelf, copied verbatim and never mapped onto ours automatically.
 *
 * @param id ours, built from {@code storeId} and {@code externalId} so a second crawl writes the
 *     same row instead of a second copy of the tree
 * @param storeId which shop's tree this belongs to
 * @param parentId the aisle above, or {@code null} at the top
 * @param externalId the shop's own id — the identity, since names repeat across a tree
 * @param name what the shop calls it
 * @param slug the name, lowercased and stripped, for reading and routing. Not an identity: two
 *     shelves under different parents may well slug alike
 * @param level how deep it sits, 0 at the top. Derived from the parent chain and stored anyway, so
 *     a screen can indent without walking it
 * @param sortOrder its place among its siblings, in the order the shop listed them
 * @param enabled whether the aisle is still offered
 */
public record StoreCategory(
    String id,
    String storeId,
    String parentId,
    String externalId,
    String name,
    String slug,
    int level,
    int sortOrder,
    boolean enabled) {}
