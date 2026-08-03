package dev.diegobarrioh.forma.application;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Turns a shop's nested aisles into the rows {@code store_category} holds (V46).
 *
 * <p>A shop publishes a tree; the table stores a flat list with a parent pointer. This is the only
 * place that conversion happens, which matters because {@code level} is derived data: no constraint
 * can check it against the parent chain (a CHECK cannot read another row), so it is right here or
 * it is wrong everywhere.
 */
public final class StoreCategoryTree {

  private StoreCategoryTree() {}

  /**
   * The shop's aisles as rows, depth first, parents before their children.
   *
   * <p>Order matters to the caller: writing a child before its parent would trip the foreign key,
   * so the list is safe to insert in sequence.
   *
   * @throws IllegalArgumentException when the shop uses one aisle id twice. That is the shop
   *     telling us two things about one row, and taking the last would silently drop a whole branch
   */
  public static List<StoreCategory> flatten(String storeId, List<StoreCategoryNode> roots) {
    List<StoreCategory> rows = new ArrayList<>();
    collect(storeId, roots, null, 0, rows, new HashSet<>());
    return List.copyOf(rows);
  }

  /** Our id for a shop's aisle: deterministic, so crawling twice writes the same row. */
  public static String idFor(String storeId, String externalId) {
    return storeId + ":" + externalId;
  }

  private static void collect(
      String storeId,
      List<StoreCategoryNode> siblings,
      String parentId,
      int level,
      List<StoreCategory> into,
      Set<String> seen) {
    int position = 0;
    for (StoreCategoryNode node : siblings) {
      String id = idFor(storeId, node.externalId());
      if (!seen.add(id)) {
        throw new IllegalArgumentException(
            "the store lists the same category id twice: " + node.externalId());
      }
      into.add(
          new StoreCategory(
              id,
              storeId,
              parentId,
              node.externalId(),
              node.name(),
              slug(node.name()),
              level,
              // The shop's own order, not ours. Alphabetising a shop's aisles would be replacing
              // how it chose to lay out its shelves with an opinion of our own.
              position++,
              true));
      collect(storeId, node.children(), id, level + 1, into, seen);
    }
  }

  /**
   * The name as a slug: lowercased, unaccented, punctuation collapsed to single hyphens.
   *
   * <p>Spanish aisle names are full of accents and commas ("Leche, café e infusiones"), and a slug
   * carrying either is no slug. Decomposing first and dropping the combining marks handles the
   * accents without a character-by-character table.
   */
  private static String slug(String name) {
    String unaccented = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    return unaccented
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("^-+|-+$", "");
  }
}
