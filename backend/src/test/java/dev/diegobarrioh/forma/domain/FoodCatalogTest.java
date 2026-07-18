package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for the reseeded {@link FoodCatalog} (FOR-152, epic FOR-148 slice 4): Diego's
 * 23 real foods (docs/fitness_os.xlsm sheet Macros) replace the 12 generic demo foods, ids are
 * unique, macros match the sheet exactly, and lookup works.
 */
class FoodCatalogTest {

  private final List<FoodItem> catalog = FoodCatalog.foods();

  @Test
  void containsExactlyDiegosTwentyThreeMacrosFoods() {
    Set<String> ids = catalog.stream().map(FoodItem::id).collect(Collectors.toSet());

    assertThat(ids)
        .containsExactlyInAnyOrder(
            "oats",
            "whey-protein",
            "banana",
            "eggs",
            "egg-whites",
            "fresh-cheese",
            "yogurt",
            "chicken",
            "turkey",
            "tuna",
            "fish",
            "salmon",
            "rice",
            "whole-wheat-pasta",
            "potato",
            "sweet-potato",
            "whole-wheat-bread",
            "vegetables",
            "salad",
            "olive-oil",
            "almonds-walnuts",
            "berries",
            "skim-milk");
    assertThat(catalog).hasSize(23);
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

  // --- Exact macros from the Macros sheet (kcal/prot/HC/grasa per 100 g + ración g) ---

  @Test
  void oatsMatchesCoposDeAvena() {
    FoodItem oats = FoodCatalog.findById("oats").orElseThrow();

    assertThat(oats.name()).isEqualTo("Copos de avena");
    assertThat(oats.kcalPer100g()).isEqualTo(370);
    assertThat(oats.proteinPer100g()).isEqualTo(13.0);
    assertThat(oats.carbsPer100g()).isEqualTo(60.0);
    assertThat(oats.fatPer100g()).isEqualTo(7.0);
    assertThat(oats.defaultServingG()).isEqualTo(60);
  }

  @Test
  void wheyProteinMatchesWheyProteina() {
    FoodItem whey = FoodCatalog.findById("whey-protein").orElseThrow();

    assertThat(whey.name()).isEqualTo("Whey proteína");
    assertThat(whey.kcalPer100g()).isEqualTo(390);
    assertThat(whey.proteinPer100g()).isEqualTo(78.0);
    assertThat(whey.carbsPer100g()).isEqualTo(8.0);
    assertThat(whey.fatPer100g()).isEqualTo(6.0);
    assertThat(whey.defaultServingG()).isEqualTo(30);
  }

  @Test
  void bananaMatchesPlatano() {
    FoodItem banana = FoodCatalog.findById("banana").orElseThrow();

    assertThat(banana.name()).isEqualTo("Plátano");
    assertThat(banana.kcalPer100g()).isEqualTo(89);
    assertThat(banana.proteinPer100g()).isEqualTo(1.1);
    assertThat(banana.carbsPer100g()).isEqualTo(23.0);
    assertThat(banana.fatPer100g()).isEqualTo(0.3);
    assertThat(banana.defaultServingG()).isEqualTo(120);
  }

  @Test
  void chickenMatchesPechugaPollo() {
    FoodItem chicken = FoodCatalog.findById("chicken").orElseThrow();

    assertThat(chicken.name()).isEqualTo("Pechuga pollo");
    assertThat(chicken.kcalPer100g()).isEqualTo(110);
    assertThat(chicken.proteinPer100g()).isEqualTo(23.0);
    assertThat(chicken.carbsPer100g()).isEqualTo(0.0);
    assertThat(chicken.fatPer100g()).isEqualTo(2.0);
    assertThat(chicken.defaultServingG()).isEqualTo(200);
  }

  @Test
  void salmonMatchesSalmonSheetRow() {
    FoodItem salmon = FoodCatalog.findById("salmon").orElseThrow();

    assertThat(salmon.name()).isEqualTo("Salmón");
    assertThat(salmon.kcalPer100g()).isEqualTo(208);
    assertThat(salmon.proteinPer100g()).isEqualTo(20.0);
    assertThat(salmon.carbsPer100g()).isEqualTo(0.0);
    assertThat(salmon.fatPer100g()).isEqualTo(13.0);
    assertThat(salmon.defaultServingG()).isEqualTo(180);
  }

  @Test
  void oliveOilMatchesAceiteOlivaVirgenExtra() {
    FoodItem oil = FoodCatalog.findById("olive-oil").orElseThrow();

    assertThat(oil.name()).isEqualTo("Aceite oliva virgen extra");
    assertThat(oil.kcalPer100g()).isEqualTo(900);
    assertThat(oil.proteinPer100g()).isEqualTo(0.0);
    assertThat(oil.carbsPer100g()).isEqualTo(0.0);
    assertThat(oil.fatPer100g()).isEqualTo(100.0);
    assertThat(oil.defaultServingG()).isEqualTo(10);
  }

  // --- FOR-134 key nutrients: the Macros sheet has no fibre/sugars/sodium/sat-fat columns at all,
  // so most of Diego's 23 foods stay fully null (never fabricated). A few keep a populated profile
  // only where a stable, brand-independent single-ingredient reference genuinely applies (rolled
  // oats, a raw banana matching the old catalog's banana exactly, whole eggs, pure olive oil, and
  // "carbs = 0 so fibre/sugars are confidently 0" for the zero-carb meats/fish) — mirroring the
  // original catalog's own confidence policy, never inventing a number the sheet doesn't support.
  // ---

  @Test
  void oatsHasAConfidentRolledOatsKeyNutrientProfile() {
    FoodItem oats = FoodCatalog.findById("oats").orElseThrow();

    assertThat(oats.fiberPer100g()).isEqualTo(10.6);
    assertThat(oats.sugarsPer100g()).isEqualTo(0.0);
    assertThat(oats.sodiumMgPer100g()).isEqualTo(2.0);
    assertThat(oats.saturatedFatPer100g()).isEqualTo(1.2);
  }

  @Test
  void bananaHasAllKeyNutrientsKnown() {
    FoodItem banana = FoodCatalog.findById("banana").orElseThrow();

    assertThat(banana.fiberPer100g()).isEqualTo(2.6);
    assertThat(banana.sugarsPer100g()).isEqualTo(12.2);
    assertThat(banana.sodiumMgPer100g()).isEqualTo(1.0);
    assertThat(banana.saturatedFatPer100g()).isEqualTo(0.1);
  }

  @Test
  void oliveOilHasAConfidentPureOilKeyNutrientProfile() {
    FoodItem oil = FoodCatalog.findById("olive-oil").orElseThrow();

    assertThat(oil.fiberPer100g()).isEqualTo(0.0);
    assertThat(oil.sugarsPer100g()).isEqualTo(0.0);
    assertThat(oil.sodiumMgPer100g()).isEqualTo(0.0);
    assertThat(oil.saturatedFatPer100g()).isEqualTo(14.0);
  }

  @Test
  void freshCheeseHasNoKeyNutrientDataKnown() {
    // "Queso fresco batido 0%" is a specific branded product -> never fabricated, left null.
    FoodItem freshCheese = FoodCatalog.findById("fresh-cheese").orElseThrow();

    assertThat(freshCheese.fiberPer100g()).isNull();
    assertThat(freshCheese.sugarsPer100g()).isNull();
    assertThat(freshCheese.sodiumMgPer100g()).isNull();
    assertThat(freshCheese.saturatedFatPer100g()).isNull();
  }

  @Test
  void mixedVegetablesHasNoKeyNutrientDataKnown() {
    FoodItem vegetables = FoodCatalog.findById("vegetables").orElseThrow();

    assertThat(vegetables.fiberPer100g()).isNull();
    assertThat(vegetables.sugarsPer100g()).isNull();
    assertThat(vegetables.sodiumMgPer100g()).isNull();
    assertThat(vegetables.saturatedFatPer100g()).isNull();
  }

  @Test
  void chickenHasAPartialKeyNutrientProfile() {
    // Pollo has 0 g carbs/100g -> fibre/sugars confidently 0; sodium and sat-fat are
    // prep/cut-dependent and not given by the sheet -> left null (partial, not fabricated).
    FoodItem chicken = FoodCatalog.findById("chicken").orElseThrow();

    assertThat(chicken.fiberPer100g()).isEqualTo(0.0);
    assertThat(chicken.sugarsPer100g()).isEqualTo(0.0);
    assertThat(chicken.sodiumMgPer100g()).isNull();
    assertThat(chicken.saturatedFatPer100g()).isNull();
  }

  @Test
  void turkeyHasNoKeyNutrientDataKnown() {
    // Pavo has 1 g carbs/100g (not confidently 0) -> unlike chicken, left fully null.
    FoodItem turkey = FoodCatalog.findById("turkey").orElseThrow();

    assertThat(turkey.fiberPer100g()).isNull();
    assertThat(turkey.sugarsPer100g()).isNull();
    assertThat(turkey.sodiumMgPer100g()).isNull();
    assertThat(turkey.saturatedFatPer100g()).isNull();
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
