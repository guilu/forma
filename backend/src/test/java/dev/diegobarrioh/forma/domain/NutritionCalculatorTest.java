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
}
