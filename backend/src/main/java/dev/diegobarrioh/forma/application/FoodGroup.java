package dev.diegobarrioh.forma.application;

/**
 * A food group: what an ingredient is made of, as a row rather than an enum constant (V43).
 *
 * <p>Read model for a {@code food_group} row. Until V43 this set lived in a compiled enum, a CHECK
 * constraint and a display table at once; a foreign key protects the references better than the
 * CHECK did, so the set became data and a group's name and glyph moved onto the group itself.
 *
 * @param id the stored token every food points at — never editable, since {@code
 *     food_catalog.food_group_id} references it
 * @param name what a person reads
 * @param icon a glyph, or absent: decoration is allowed to be missing, a name is not
 * @param color a colour for the group, or absent. Nothing renders it yet; the column exists so the
 *     choice is stored where the group is, rather than being invented per screen
 * @param sortOrder where the group sits in a list. Groups have a conventional order (carbohydrate,
 *     protein, fruit…) that is neither alphabetical nor the order they were created in
 * @param enabled whether the group is still offered. Retiring one has to be possible without
 *     deleting it, because the foods filed under it keep pointing at it forever
 */
public record FoodGroup(
    String id, String name, String icon, String color, int sortOrder, boolean enabled) {

  public FoodGroup {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("id must not be blank");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
  }

  /** This group renamed and re-drawn, keeping everything an edit is not allowed to touch. */
  public FoodGroup renamedTo(String newName, String newIcon) {
    return new FoodGroup(id, newName, newIcon, color, sortOrder, enabled);
  }
}
