package dev.diegobarrioh.forma.application;

import java.util.List;

/**
 * One aisle as a shop describes it, with whatever hangs off it (V46).
 *
 * <p>The shape a {@link StoreCatalogSource} answers in: nested, because that is how a shop
 * publishes its shelves, and free of anything we decide — no id of ours, no level, no slug. {@link
 * StoreCategoryTree} turns this into rows.
 *
 * @param externalId the shop's own id for the aisle; required, and the identity we key on
 * @param name what the shop calls it, verbatim
 * @param children the aisles below, in the order the shop listed them; empty at the bottom
 */
public record StoreCategoryNode(String externalId, String name, List<StoreCategoryNode> children) {

  public StoreCategoryNode {
    if (externalId == null || externalId.isBlank()) {
      throw new IllegalArgumentException("externalId must not be blank");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    children = List.copyOf(children);
  }
}
