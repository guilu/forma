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

  // --- FOR-134: key nutrients populated with known values, null where genuinely unknown ---

  @Test
  void oatsHasAllKeyNutrientsKnown() {
    // Sourced from the same USDA raw-oats reference the existing macros already match exactly.
    FoodItem oats = FoodCatalog.findById("oats").orElseThrow();

    assertThat(oats.fiberPer100g()).isNotNull();
    assertThat(oats.sugarsPer100g()).isNotNull();
    assertThat(oats.sodiumMgPer100g()).isNotNull();
    assertThat(oats.saturatedFatPer100g()).isNotNull();
  }

  @Test
  void bananaHasAllKeyNutrientsKnown() {
    FoodItem banana = FoodCatalog.findById("banana").orElseThrow();

    assertThat(banana.fiberPer100g()).isNotNull();
    assertThat(banana.sugarsPer100g()).isNotNull();
    assertThat(banana.sodiumMgPer100g()).isNotNull();
    assertThat(banana.saturatedFatPer100g()).isNotNull();
  }

  @Test
  void freshCheeseHasNoKeyNutrientDataKnown() {
    // "Queso fresco" varies too much by brand/region to responsibly assert a precise value ->
    // never fabricated, left entirely null (spec FOR-134: "never fabricate").
    FoodItem freshCheese = FoodCatalog.findById("fresh-cheese").orElseThrow();

    assertThat(freshCheese.fiberPer100g()).isNull();
    assertThat(freshCheese.sugarsPer100g()).isNull();
    assertThat(freshCheese.sodiumMgPer100g()).isNull();
    assertThat(freshCheese.saturatedFatPer100g()).isNull();
  }

  @Test
  void mixedVegetablesHasNoKeyNutrientDataKnown() {
    // A generic "mixed vegetables" entry has no single accurate reference composition -> null.
    FoodItem vegetables = FoodCatalog.findById("vegetables").orElseThrow();

    assertThat(vegetables.fiberPer100g()).isNull();
    assertThat(vegetables.sugarsPer100g()).isNull();
    assertThat(vegetables.sodiumMgPer100g()).isNull();
    assertThat(vegetables.saturatedFatPer100g()).isNull();
  }

  @Test
  void chickenHasAPartialKeyNutrientProfile() {
    // Meat has no carbs -> fiber/sugars are confidently zero; sodium is too prep-dependent to
    // assert -> left null (partial, not fabricated).
    FoodItem chicken = FoodCatalog.findById("chicken").orElseThrow();

    assertThat(chicken.fiberPer100g()).isEqualTo(0.0);
    assertThat(chicken.sugarsPer100g()).isEqualTo(0.0);
    assertThat(chicken.sodiumMgPer100g()).isNull();
    assertThat(chicken.saturatedFatPer100g()).isNotNull();
  }

  @Test
  void everyKnownKeyNutrientIsNonNegative() {
    assertThat(catalog)
        .allSatisfy(
            food -> {
              if (food.fiberPer100g() != null) {
                assertThat(food.fiberPer100g()).isGreaterThanOrEqualTo(0);
              }
              if (food.sugarsPer100g() != null) {
                assertThat(food.sugarsPer100g()).isGreaterThanOrEqualTo(0);
              }
              if (food.sodiumMgPer100g() != null) {
                assertThat(food.sodiumMgPer100g()).isGreaterThanOrEqualTo(0);
              }
              if (food.saturatedFatPer100g() != null) {
                assertThat(food.saturatedFatPer100g()).isGreaterThanOrEqualTo(0);
              }
            });
  }
}
