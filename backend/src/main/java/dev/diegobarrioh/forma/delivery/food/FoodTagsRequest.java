package dev.diegobarrioh.forma.delivery.food;

import java.util.List;

/**
 * Body accepted when setting a food's labels (V50, admin only).
 *
 * <p>The complete set, not a change to it. An absent list and an empty one both mean "this food
 * carries none", which is a thing somebody can genuinely want to say — unticking the last checkbox
 * has to be possible.
 */
public record FoodTagsRequest(List<String> tagIds) {

  /** The ids, with absent read as none. */
  public List<String> tagIdsOrEmpty() {
    return tagIds == null ? List.of() : tagIds;
  }
}
