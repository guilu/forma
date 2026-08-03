package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.CategoryScope;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Reading and editing how categories are written and drawn (FOR-197).
 *
 * <p>Two vocabularies, stored differently since V43: a food group is a row in {@code food_group},
 * where its name and glyph sit alongside its identity; a shopping aisle is still an enum constant
 * whose label lives in {@code category_display}. Every screen that shows a category wants the same
 * three things from it — a code, a label, a glyph — so this service serves both through one shape,
 * and the difference stays here rather than in the controller or the frontend.
 *
 * <p>Update only. Creating a category would put a code in the table that no row could ever be filed
 * under, and deleting one would leave the rows that use it with nothing to render — for a food
 * group the foreign key refuses it outright. Which categories exist is a decision with data behind
 * it; their names are not.
 */
@Service
public class CategoryDisplayService {

  private final CategoryDisplayRepository displays;
  private final FoodGroupRepository foodGroups;

  public CategoryDisplayService(
      CategoryDisplayRepository displays, FoodGroupRepository foodGroups) {
    this.displays = displays;
    this.foodGroups = foodGroups;
  }

  /** Every category, or only one vocabulary's when {@code scope} is given. */
  public List<CategoryDisplay> findAll(CategoryScope scope) {
    List<CategoryDisplay> found = new ArrayList<>();
    if (scope == null || scope == CategoryScope.FOOD) {
      foodGroups.findAll().forEach(group -> found.add(asDisplay(group)));
    }
    if (scope == null || scope == CategoryScope.SHOPPING) {
      found.addAll(displays.findAll(CategoryScope.SHOPPING));
    }
    return found;
  }

  /**
   * Changes the label and icon of an existing category (admin only).
   *
   * @throws NotFoundException when that vocabulary has no such code — the alternative would be
   *     writing a category nothing may ever point at. A food group code is not an aisle code even
   *     when the two read alike, so each is looked up in its own vocabulary only
   */
  public CategoryDisplay update(CategoryScope scope, String code, String label, String icon) {
    if (scope == CategoryScope.FOOD) {
      FoodGroup group =
          foodGroups
              .find(code)
              .orElseThrow(() -> new NotFoundException("No existe la categoría: " + code));
      // A rename says nothing about where the group sits in a list or whether it is still offered,
      // so those travel across untouched.
      FoodGroup updated = group.renamedTo(label, icon);
      foodGroups.update(updated);
      return asDisplay(updated);
    }
    displays
        .find(scope, code)
        .orElseThrow(() -> new NotFoundException("No existe la categoría: " + code));
    CategoryDisplay updated = new CategoryDisplay(scope, code, label, icon);
    displays.update(updated);
    return updated;
  }

  private static CategoryDisplay asDisplay(FoodGroup group) {
    return new CategoryDisplay(CategoryScope.FOOD, group.id(), group.name(), group.icon());
  }
}
