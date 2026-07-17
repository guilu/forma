package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link NutritionCalculator} (FOR-32): meal and day totals from catalog
 * foods × grams, the empty case, and unknown-food rejection. Plain JUnit 5 + AssertJ (ADR-007).
 */
class NutritionCalculatorTest {

  private static MealTemplate meal(List<MealItem> items) {
    return new MealTemplate(
        NutritionDayType.RUNNING, MealType.BREAKFAST, "Desayuno", LocalTime.of(8, 0), items, null);
  }

  @Test
  void computesMealTotalsFromFoodsAndGrams() {
    // oats 60 g (0.6×) + banana 120 g (1.2×), values from the FOR-30 catalog.
    MealTemplate breakfast = meal(List.of(new MealItem("oats", 60), new MealItem("banana", 120)));

    NutritionTotals totals = NutritionCalculator.mealTotals(breakfast);

    // kcal 389*0.6 + 89*1.2 = 233.4 + 106.8 = 340.2 -> 340
    assertThat(totals.calories()).isEqualTo(340);
    assertThat(totals.proteinG()).isCloseTo(11.5, within(1e-9)); // 10.14 + 1.32 = 11.46 -> 11.5
    assertThat(totals.carbsG()).isCloseTo(67.1, within(1e-9)); // 39.78 + 27.36 = 67.14 -> 67.1
    assertThat(totals.fatG()).isCloseTo(4.5, within(1e-9)); // 4.14 + 0.36 = 4.5
  }

  @Test
  void dayTotalsSumRawWithoutAccumulatedRoundingError() {
    // Same two foods split across two meals: the day sums raw contributions, not rounded meals.
    MealTemplate m1 = meal(List.of(new MealItem("oats", 60)));
    MealTemplate m2 = meal(List.of(new MealItem("banana", 120)));

    NutritionTotals day = NutritionCalculator.dayTotals(List.of(m1, m2));

    // Rounded per-meal protein would be 10.1 + 1.3 = 11.4; the raw day sum is 11.46 -> 11.5.
    assertThat(day.proteinG()).isCloseTo(11.5, within(1e-9));
    assertThat(day.calories()).isEqualTo(340);
  }

  @Test
  void emptyDayIsAllZero() {
    NutritionTotals day = NutritionCalculator.dayTotals(List.of());

    assertThat(day.calories()).isZero();
    assertThat(day.proteinG()).isZero();
    assertThat(day.carbsG()).isZero();
    assertThat(day.fatG()).isZero();
  }

  @Test
  void rejectsAnUnknownFoodId() {
    MealTemplate bad = meal(List.of(new MealItem("ghost-food", 100)));

    assertThatThrownBy(() -> NutritionCalculator.mealTotals(bad))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ghost-food");
  }

  // --- FOR-134: itemKeyNutrients reuses the same per-100g x factor scaling as itemTotals ---

  @Test
  void itemKeyNutrientsScalesAKnownFoodsNutrientsByGrams() {
    // oats: fiber 10.6/sugars 0/sodium 2mg/satFat 1.2 per 100g; 60g -> x0.6 factor (same as
    // macros).
    NutritionTotals macros = NutritionCalculator.itemTotals(new MealItem("oats", 60));
    KeyNutrientTotals keyNutrients = NutritionCalculator.itemKeyNutrients(new MealItem("oats", 60));

    assertThat(macros.calories()).isEqualTo(233); // sanity: same factor as macros
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
        NutritionCalculator.itemKeyNutrients(new MealItem("vegetables", 200));

    assertThat(keyNutrients.fiberG()).isNull();
    assertThat(keyNutrients.sugarsG()).isNull();
    assertThat(keyNutrients.sodiumMg()).isNull();
    assertThat(keyNutrients.saturatedFatG()).isNull();
  }

  @Test
  void itemKeyNutrientsPropagatesNullPerNutrientIndependentlyForAPartialFood() {
    // "chicken" has fiber/sugars known (0) but sodium unknown (null) -> each nutrient is
    // independent, not all-or-nothing.
    KeyNutrientTotals keyNutrients =
        NutritionCalculator.itemKeyNutrients(new MealItem("chicken", 150));

    assertThat(keyNutrients.fiberG()).isEqualTo(0.0);
    assertThat(keyNutrients.sugarsG()).isEqualTo(0.0);
    assertThat(keyNutrients.sodiumMg()).isNull();
    assertThat(keyNutrients.saturatedFatG()).isNotNull();
  }

  @Test
  void itemKeyNutrientsRejectsAnUnknownFoodId() {
    assertThatThrownBy(() -> NutritionCalculator.itemKeyNutrients(new MealItem("ghost-food", 100)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ghost-food");
  }
}
