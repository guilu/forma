package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.CategoryScope;
import java.util.List;
import java.util.Optional;

/**
 * Port over the persisted category labels and icons (FOR-197). Owned by the application side;
 * adapters implement it (ADR-001).
 *
 * <p>No insert and no delete: the set of categories is closed in the domain enums and in the
 * database's CHECK constraints. This port can only change how an existing one reads.
 */
public interface CategoryDisplayRepository {

  /** Every category, or only one vocabulary's when {@code scope} is given. */
  List<CategoryDisplay> findAll(CategoryScope scope);

  /** One category; empty when that vocabulary has no such code. */
  Optional<CategoryDisplay> find(CategoryScope scope, String code);

  /** Overwrites the label and icon of an existing category. Callers check it exists first. */
  void update(CategoryDisplay display);
}
