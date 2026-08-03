package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.PrimaryMacro;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Application use case exposing the persisted food catalog (FOR-173), read-only. Thin service over
 * {@link FoodCatalogRepository}, mirroring FOR-172's {@code CatalogExerciseService} pattern. Named
 * to coexist with the static {@link FoodCatalogService}.
 */
@Service
public class CatalogFoodService {

  private final FoodCatalogRepository repository;

  public CatalogFoodService(FoodCatalogRepository repository) {
    this.repository = repository;
  }

  /** All foods in the catalog. */
  public List<CatalogFood> listAll() {
    return repository.findAll();
  }

  /** A single food by id; throws {@link NotFoundException} when no food has that id. */
  public CatalogFood getById(String id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("No existe el alimento: " + id));
  }

  /**
   * Adds a food to the shared catalog (FOR-190, admin only).
   *
   * @throws ConflictException when the id is already taken — it is the catalog's stable handle,
   *     referenced by shopping products through a foreign key, so it can never be reassigned to a
   *     different food
   */
  public CatalogFood create(CatalogFood food) {
    if (repository.findById(food.id()).isPresent()) {
      throw new ConflictException("Ya existe un alimento con ese identificador: " + food.id());
    }
    CatalogFood stored = withPrimaryMacro(food);
    repository.insert(stored);
    return stored;
  }

  /**
   * Replaces the food stored under {@code id} (FOR-190, admin only).
   *
   * <p>The id in the path wins over any id in {@code food}: a rename would silently orphan every
   * shopping product pointing at the old one, so it is not offered at all rather than half-handled.
   *
   * @throws NotFoundException when no food has that id
   */
  public CatalogFood update(String id, CatalogFood food) {
    getById(id);
    CatalogFood stored =
        new CatalogFood(
            id,
            food.name(),
            food.servingSizeG(),
            food.kcal(),
            food.proteinG(),
            food.carbsG(),
            food.fatG(),
            food.fiberG(),
            food.sugarsG(),
            food.sodiumMg(),
            food.saturatedFatG(),
            food.foodGroupId(),
            food.primaryMacro());
    CatalogFood classified = withPrimaryMacro(stored);
    repository.update(classified);
    return classified;
  }

  /**
   * This food with its primary macro filled in from its own numbers when it carries none (V44).
   *
   * <p>A default, not a verdict: a caller that states one keeps it, however the arithmetic feels
   * about it — "yogur proteína" is sold and eaten as a protein even when its label's carbohydrates
   * edge it out. A caller that says nothing gets the macro its macros imply, so the column is never
   * left empty next to the very data that fills it.
   *
   * <p>The same rule on update, which is what re-derives it: a food whose fat was corrected upwards
   * should stop claiming to be a protein just because it was one before. The admin form posts back
   * what it was shown, so an untouched classification survives; a client that omits the field is
   * asking for it to be recomputed.
   */
  private static CatalogFood withPrimaryMacro(CatalogFood food) {
    if (food.primaryMacro() != null) {
      return food;
    }
    PrimaryMacro dominant =
        PrimaryMacro.dominantOf(
                toDouble(food.proteinG()), toDouble(food.carbsG()), toDouble(food.fatG()))
            .orElse(null);
    return new CatalogFood(
        food.id(),
        food.name(),
        food.servingSizeG(),
        food.kcal(),
        food.proteinG(),
        food.carbsG(),
        food.fatG(),
        food.fiberG(),
        food.sugarsG(),
        food.sodiumMg(),
        food.saturatedFatG(),
        food.foodGroupId(),
        dominant);
  }

  private static Double toDouble(java.math.BigDecimal value) {
    return value == null ? null : value.doubleValue();
  }

  /**
   * Removes a food from the shared catalog (FOR-190, admin only).
   *
   * @throws NotFoundException when no food has that id
   */
  public void delete(String id) {
    if (!repository.delete(id)) {
      throw new NotFoundException("No existe el alimento: " + id);
    }
  }
}
