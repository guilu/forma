package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link NutritionCalculator} (FOR-32): meal and day totals from catalog
 * foods × grams, the empty case, and unknown-food rejection. Plain JUnit 5 + AssertJ (ADR-007).
 *
 * <p>Foods are supplied through an explicit {@link FoodLookup} fixture rather than read from a
 * static catalog: the calculator no longer knows where foods come from, so the test owns them. The
 * fixture's values mirror the seeded {@code food_catalog} rows the assertions were originally
 * written against, so the expected numbers are unchanged.
 */
class NutritionCalculatorTest {

  private static final FoodLookup FOODS =
      lookup(
          new FoodItem("oats", "Copos de avena", 370, 13.0, 60.0, 7.0, 60, 10.6, 0.0, 2.0, 1.2),
          new FoodItem("banana", "Plátano", 89, 1.1, 23.0, 0.3, 120, 2.6, 12.2, 1.0, 0.1),
          new FoodItem("chicken", "Pechuga pollo", 110, 23.0, 0.0, 2.0, 200, 0.0, 0.0, null, null),
          new FoodItem("vegetables", "Verdura variada", 35, 2.0, 6.0, 0.3, 300));

  private static FoodLookup lookup(FoodItem... foods) {
    Map<String, FoodItem> byId =
        java.util.Arrays.stream(foods)
            .collect(java.util.stream.Collectors.toMap(FoodItem::id, food -> food));
    return id -> Optional.ofNullable(byId.get(id));
  }

  @Test
  void scalesAFoodByItsGrams() {
    // oats 60 g (0.6x), values from the FOR-152 reseeded catalog.
    NutritionTotals totals = NutritionCalculator.itemTotals(new MealItem("oats", 60), FOODS);

    assertThat(totals.calories()).isEqualTo(222); // 370 * 0.6
    assertThat(totals.proteinG()).isCloseTo(7.8, within(1e-9));
    assertThat(totals.carbsG()).isCloseTo(36.0, within(1e-9));
    assertThat(totals.fatG()).isCloseTo(4.2, within(1e-9));
  }

  /**
   * Callers sum these themselves, and the rounding is why it is worth saying so: each item rounds
   * to a tenth, so a caller adding twenty of them accumulates at most a tenth of a gram of drift.
   * The meal- and day-level helpers that used to round once at the end went with the {@code
   * MealTemplate} the in-code day catalog was built from (V54).
   */
  @Test
  void roundsEachItemToATenth() {
    NutritionTotals banana = NutritionCalculator.itemTotals(new MealItem("banana", 120), FOODS);

    assertThat(banana.proteinG()).isCloseTo(1.3, within(1e-9)); // 1.1 * 1.2 = 1.32 -> 1.3
    assertThat(banana.calories()).isEqualTo(107); // 89 * 1.2 = 106.8 -> 107
  }

  @Test
  void rejectsAnUnknownFoodId() {
    assertThatThrownBy(() -> NutritionCalculator.itemTotals(new MealItem("ghost-food", 100), FOODS))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ghost-food");
  }

  @Test
  void rejectsAMissingLookup() {
    // A null lookup is a programming error, not "no foods": failing loudly here stops it being
    // mistaken for an unknown-food rejection further down.
    assertThatThrownBy(() -> NutritionCalculator.itemTotals(new MealItem("oats", 60), null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("foods");
  }

  // --- FOR-134: itemKeyNutrients reuses the same per-100g x factor scaling as itemTotals ---

  @Test
  void itemKeyNutrientsScalesAKnownFoodsNutrientsByGrams() {
    // oats: fiber 10.6/sugars 0/sodium 2mg/satFat 1.2 per 100g (unchanged by FOR-152 — kept the
    // same rolled-oats reference); 60g -> x0.6 factor (same as macros).
    NutritionTotals macros = NutritionCalculator.itemTotals(new MealItem("oats", 60), FOODS);
    KeyNutrientTotals keyNutrients =
        NutritionCalculator.itemKeyNutrients(new MealItem("oats", 60), FOODS);

    assertThat(macros.calories()).isEqualTo(222); // sanity: same factor as macros, 370*0.6
    assertThat(keyNutrients.fiberG()).isCloseTo(6.4, within(0.05)); // 10.6 * 0.6 = 6.36 -> 6.4
    assertThat(keyNutrients.sugarsG()).isEqualTo(0.0);
    assertThat(keyNutrients.sodiumMg()).isEqualTo(1); // 2 * 0.6 = 1.2 -> round to 1
    assertThat(keyNutrients.saturatedFatG())
        .isCloseTo(0.7, within(0.05)); // 1.2 * 0.6 = 0.72 -> 0.7
  }

  @Test
  void itemKeyNutrientsPropagatesNullForAFoodWithNoKeyNutrientData() {
    // "vegetables" catalog entry has no known key nutrients -> every field stays null, never
    // fabricated, even though the food and grams are perfectly valid.
    KeyNutrientTotals keyNutrients =
        NutritionCalculator.itemKeyNutrients(new MealItem("vegetables", 200), FOODS);

    assertThat(keyNutrients.fiberG()).isNull();
    assertThat(keyNutrients.sugarsG()).isNull();
    assertThat(keyNutrients.sodiumMg()).isNull();
    assertThat(keyNutrients.saturatedFatG()).isNull();
  }

  @Test
  void itemKeyNutrientsPropagatesNullPerNutrientIndependentlyForAPartialFood() {
    // "chicken" (pollo, 0 g carbs/100g) has fiber/sugars known (0) but sodium and sat-fat unknown
    // (null, not given by the Macros sheet) -> each nutrient is independent, not all-or-nothing.
    KeyNutrientTotals keyNutrients =
        NutritionCalculator.itemKeyNutrients(new MealItem("chicken", 150), FOODS);

    assertThat(keyNutrients.fiberG()).isEqualTo(0.0);
    assertThat(keyNutrients.sugarsG()).isEqualTo(0.0);
    assertThat(keyNutrients.sodiumMg()).isNull();
    assertThat(keyNutrients.saturatedFatG()).isNull();
  }

  @Test
  void itemKeyNutrientsRejectsAnUnknownFoodId() {
    assertThatThrownBy(
            () -> NutritionCalculator.itemKeyNutrients(new MealItem("ghost-food", 100), FOODS))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ghost-food");
  }
}
