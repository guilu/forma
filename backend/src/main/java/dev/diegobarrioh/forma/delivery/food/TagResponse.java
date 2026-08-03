package dev.diegobarrioh.forma.delivery.food;

import dev.diegobarrioh.forma.application.Tag;

/**
 * Response body for one label (V50).
 *
 * <p>Delivery read model, distinct from the application type (ADR-005). Carries no hint of what
 * kind of label it is — ingredient, state, occasion — because the catalog does not record one and
 * inventing a grouping here would be the screen deciding something the data never said.
 */
public record TagResponse(String id, String name, int sortOrder) {

  public static TagResponse from(Tag tag) {
    return new TagResponse(tag.id(), tag.name(), tag.sortOrder());
  }
}
