package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.FoodItem;
import dev.diegobarrioh.forma.domain.FoodLookup;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Application use case exposing the food catalog (FOR-30) as nutrition values.
 *
 * <p>Reads {@code food_catalog} through {@link FoodCatalogRepository}. It used to serve a static
 * in-code list of 23 foods that duplicated, row for row, what V25 had already seeded into that same
 * table — two sources of truth for the same numbers, of which only one could be edited. The admin
 * catalog (FOR-190) writes the table, so the table wins and the constant is gone.
 *
 * <p>It implements {@link FoodLookup}, which is how the domain calculations get their foods: {@link
 * dev.diegobarrioh.forma.domain.NutritionCalculator} takes a lookup rather than reaching for a
 * catalog, so this service is the seam where "where foods live" is finally decided.
 *
 * <p>Distinct from {@link CatalogFoodService}, which serves the same table as an editable admin
 * read model ({@link CatalogFood}, faithful to the row including its nulls). This one serves it as
 * the nutrition domain's {@link FoodItem}.
 */
@Service
public class FoodCatalogService implements FoodLookup {

  private final FoodCatalogRepository repository;

  public FoodCatalogService(FoodCatalogRepository repository) {
    this.repository = repository;
  }

  /** All catalog foods. */
  public List<FoodItem> allFoods() {
    return repository.findAll().stream().map(FoodCatalogService::toFoodItem).toList();
  }

  /** Resolves a catalog food by its stable id. */
  @Override
  public Optional<FoodItem> findById(String id) {
    return repository.findById(id).map(FoodCatalogService::toFoodItem);
  }

  /**
   * What state a food's numbers describe (V51), or {@code null} while nobody has said.
   *
   * <p>Not on {@link FoodItem}: that type is what the nutrition calculations consume, and they
   * compute with macros and have no business knowing about kitchens. Only the equivalences care,
   * and they ask here.
   */
  public dev.diegobarrioh.forma.domain.Preparation preparationOf(String foodId) {
    return repository.findById(foodId).map(CatalogFood::preparation).orElse(null);
  }

  /**
   * The persisted row as nutrition values.
   *
   * <p>{@link CatalogFood} carries the row as stored, nulls and all; {@link FoodItem} is what the
   * calculations consume. Macros are NOT NULL in the table, so they map straight across; the key
   * nutrients and the serving are nullable on both sides and stay null rather than being defaulted
   * into a number nobody measured (FOR-134).
   */
  private static FoodItem toFoodItem(CatalogFood food) {
    return new FoodItem(
        food.id(),
        food.name(),
        food.kcal(),
        food.proteinG().doubleValue(),
        food.carbsG().doubleValue(),
        food.fatG().doubleValue(),
        toServingGrams(food.servingSizeG()),
        toDouble(food.fiberG()),
        toDouble(food.sugarsG()),
        toDouble(food.sodiumMg()),
        toDouble(food.saturatedFatG()));
  }

  /**
   * The serving in whole grams, or {@code null} when the row has none.
   *
   * <p>Stored as {@code NUMERIC(6,1)} but consumed as grams: a serving is a suggested portion, and
   * a tenth of a gram of it changes nothing. Rounded rather than truncated so a 60.5 g serving does
   * not quietly become 60.
   */
  private static Integer toServingGrams(BigDecimal servingSizeG) {
    return servingSizeG == null
        ? null
        : servingSizeG.setScale(0, java.math.RoundingMode.HALF_UP).intValue();
  }

  private static Double toDouble(BigDecimal value) {
    return value == null ? null : value.doubleValue();
  }
}
