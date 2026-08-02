package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.CategoryScope;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Reading and editing how categories are written and drawn (FOR-197).
 *
 * <p>Update only. Creating a category here would put a code in the table that no row could ever be
 * filed under — the CHECK constraints on {@code food_catalog}, {@code shopping_products} and {@code
 * store_product} would refuse it — and deleting one would leave the rows that use it with nothing
 * to render. The set of categories is a schema decision; their names are not.
 */
@Service
public class CategoryDisplayService {

  private final CategoryDisplayRepository repository;

  public CategoryDisplayService(CategoryDisplayRepository repository) {
    this.repository = repository;
  }

  /** Every category, or only one vocabulary's when {@code scope} is given. */
  public List<CategoryDisplay> findAll(CategoryScope scope) {
    return repository.findAll(scope);
  }

  /**
   * Changes the label and icon of an existing category (admin only).
   *
   * @throws NotFoundException when that vocabulary has no such code — the alternative would be
   *     writing a category nothing may ever point at
   */
  public CategoryDisplay update(CategoryScope scope, String code, String label, String icon) {
    repository
        .find(scope, code)
        .orElseThrow(() -> new NotFoundException("No existe la categoría: " + code));
    CategoryDisplay updated = new CategoryDisplay(scope, code, label, icon);
    repository.update(updated);
    return updated;
  }
}
