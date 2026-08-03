package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.domain.FoodItem;
import dev.diegobarrioh.forma.domain.MealItem;
import dev.diegobarrioh.forma.domain.NutritionCalculator;
import dev.diegobarrioh.forma.domain.SeededFoods;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link FoodCatalogService} (FOR-30): serves the persisted catalog as nutrition
 * values and resolves by id (no Spring context — ADR-007).
 */
class FoodCatalogServiceTest {

  private final FoodCatalogService service = SeededFoodCatalog.service();

  @Test
  void exposesTheCatalog() {
    assertThat(service.allFoods()).containsExactlyElementsOf(SeededFoods.all());
  }

  @Test
  void resolvesFoodById() {
    assertThat(service.findById("oats")).contains(SeededFoods.byId("oats"));
    assertThat(service.findById("nope")).isEmpty();
  }

  /**
   * The service is the seam the domain calculations resolve foods through, so it has to satisfy
   * that contract rather than merely happen to have a method of the same name.
   */
  @Test
  void isTheLookupTheDomainCalculationsTake() {
    int calories = NutritionCalculator.itemTotals(new MealItem("oats", 60), service).calories();

    assertThat(calories).isEqualTo(222); // 370 kcal/100g x 0.6
  }

  /** A serving is a suggested portion: stored to a tenth of a gram, consumed as whole grams. */
  @Test
  void roundsTheStoredServingToWholeGrams() {
    FoodCatalogService rounding = new FoodCatalogService(singleFood(withServing("60.5")));

    assertThat(rounding.findById("x")).get().extracting(FoodItem::defaultServingG).isEqualTo(61);
  }

  /** A row with no serving stays without one — the food is still perfectly readable. */
  @Test
  void keepsAMissingServingNull() {
    FoodCatalogService noServing = new FoodCatalogService(singleFood(withServing(null)));

    assertThat(noServing.findById("x")).get().extracting(FoodItem::defaultServingG).isNull();
  }

  private static CatalogFood withServing(String servingSizeG) {
    return new CatalogFood(
        "x",
        "X",
        servingSizeG == null ? null : new BigDecimal(servingSizeG),
        100,
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.ONE,
        null,
        null,
        null,
        null,
        null);
  }

  private static FoodCatalogRepository singleFood(CatalogFood food) {
    return new FoodCatalogRepository() {
      @Override
      public List<CatalogFood> findAll() {
        return List.of(food);
      }

      @Override
      public Optional<CatalogFood> findById(String id) {
        return food.id().equals(id) ? Optional.of(food) : Optional.empty();
      }

      @Override
      public void insert(CatalogFood value) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void update(CatalogFood value) {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean delete(String id) {
        throw new UnsupportedOperationException();
      }
    };
  }
}
