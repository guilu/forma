package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for the initial {@link FoodCatalog} (FOR-30): the plan's common foods are
 * present, ids are unique, and lookup works.
 */
class FoodCatalogTest {

  private final List<FoodItem> catalog = FoodCatalog.foods();

  @Test
  void includesThePlansCommonFoods() {
    Set<String> ids = catalog.stream().map(FoodItem::id).collect(Collectors.toSet());

    assertThat(ids)
        .contains(
            "oats",
            "eggs",
            "yogurt",
            "fresh-cheese",
            "chicken",
            "turkey",
            "fish",
            "rice",
            "potato",
            "banana",
            "vegetables",
            "whey-protein");
  }

  @Test
  void idsAreUnique() {
    Set<String> ids = catalog.stream().map(FoodItem::id).collect(Collectors.toSet());
    assertThat(ids).hasSameSizeAs(catalog);
  }

  @Test
  void everyFoodHasSaneMacros() {
    assertThat(catalog)
        .allSatisfy(
            food -> {
              assertThat(food.kcalPer100g()).isPositive();
              assertThat(food.proteinPer100g()).isGreaterThanOrEqualTo(0);
              // Protein cannot exceed 100 g per 100 g (guards against obvious seed errors).
              assertThat(food.proteinPer100g()).isLessThanOrEqualTo(100);
              assertThat(food.defaultServingG()).isPositive();
            });
  }

  @Test
  void findsFoodByIdAndReturnsEmptyForUnknown() {
    assertThat(FoodCatalog.findById("chicken")).isPresent();
    assertThat(FoodCatalog.findById("does-not-exist")).isEmpty();
  }
}
