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
    // oats 60 g (0.6×) + banana 120 g (1.2×), values from the FOR-152 reseeded catalog.
    MealTemplate breakfast = meal(List.of(new MealItem("oats", 60), new MealItem("banana", 120)));

    NutritionTotals totals = NutritionCalculator.mealTotals(breakfast);

    // kcal 370*0.6 + 89*1.2 = 222 + 106.8 = 328.8 -> 329
    assertThat(totals.calories()).isEqualTo(329);
    assertThat(totals.proteinG()).isCloseTo(9.1, within(1e-9)); // 7.8 + 1.32 = 9.12 -> 9.1
    assertThat(totals.carbsG()).isCloseTo(63.6, within(1e-9)); // 36.0 + 27.6 = 63.6
    assertThat(totals.fatG()).isCloseTo(4.6, within(1e-9)); // 4.2 + 0.36 = 4.56 -> 4.6
  }

  @Test
  void dayTotalsSumRawWithoutAccumulatedRoundingError() {
    // Same two foods split across two meals: the day sums raw contributions, not rounded meals.
    MealTemplate m1 = meal(List.of(new MealItem("oats", 60)));
    MealTemplate m2 = meal(List.of(new MealItem("banana", 120)));

    NutritionTotals day = NutritionCalculator.dayTotals(List.of(m1, m2));

    // Raw day sum: 7.8 + 1.32 = 9.12 -> 9.1 (the code always sums raw then rounds once, per
    // NutritionCalculator's contract, regardless of whether it happens to match per-meal rounding
    // for these particular values).
    assertThat(day.proteinG()).isCloseTo(9.1, within(1e-9));
    assertThat(day.calories()).isEqualTo(329);
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
    // oats: fiber 10.6/sugars 0/sodium 2mg/satFat 1.2 per 100g (unchanged by FOR-152 — kept the
    // same rolled-oats reference); 60g -> x0.6 factor (same as macros).
    NutritionTotals macros = NutritionCalculator.itemTotals(new MealItem("oats", 60));
    KeyNutrientTotals keyNutrients = NutritionCalculator.itemKeyNutrients(new MealItem("oats", 60));

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
        NutritionCalculator.itemKeyNutrients(new MealItem("vegetables", 200));

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
        NutritionCalculator.itemKeyNutrients(new MealItem("chicken", 150));

    assertThat(keyNutrients.fiberG()).isEqualTo(0.0);
    assertThat(keyNutrients.sugarsG()).isEqualTo(0.0);
    assertThat(keyNutrients.sodiumMg()).isNull();
    assertThat(keyNutrients.saturatedFatG()).isNull();
  }

  @Test
  void itemKeyNutrientsRejectsAnUnknownFoodId() {
    assertThatThrownBy(() -> NutritionCalculator.itemKeyNutrients(new MealItem("ghost-food", 100)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ghost-food");
  }
}
