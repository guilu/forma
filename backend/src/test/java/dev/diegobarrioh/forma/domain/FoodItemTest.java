package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link FoodItem} (FOR-30): construction validation. Plain JUnit 5 + AssertJ
 * (ADR-007).
 */
class FoodItemTest {

  @Test
  void createsValidFood() {
    FoodItem food = new FoodItem("chicken", "Pollo", 165, 31.0, 0.0, 3.6, 150);

    assertThat(food.id()).isEqualTo("chicken");
    assertThat(food.kcalPer100g()).isEqualTo(165);
    assertThat(food.carbsPer100g()).isZero();
  }

  @Test
  void rejectsBlankId() {
    assertThatThrownBy(() -> new FoodItem(" ", "Pollo", 165, 31.0, 0.0, 3.6, 150))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("id");
  }

  @Test
  void rejectsNonPositiveCalories() {
    assertThatThrownBy(() -> new FoodItem("x", "X", 0, 1.0, 1.0, 1.0, 100))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("kcalPer100g");
  }

  @Test
  void rejectsNegativeMacro() {
    assertThatThrownBy(() -> new FoodItem("x", "X", 100, -1.0, 1.0, 1.0, 100))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("proteinPer100g");
  }

  @Test
  void rejectsNonPositiveServing() {
    assertThatThrownBy(() -> new FoodItem("x", "X", 100, 1.0, 1.0, 1.0, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("defaultServingG");
  }

  // --- FOR-134: nullable key nutrients (fiber, sugars, sodium, saturated fat) ---

  @Test
  void createsAValidFoodWithNoKeyNutrients() {
    // Reference data is incomplete for most foods -> a food with all-null key nutrients is valid,
    // never fabricated (spec FOR-134).
    FoodItem food = new FoodItem("x", "X", 100, 1.0, 1.0, 1.0, 100);

    assertThat(food.fiberPer100g()).isNull();
    assertThat(food.sugarsPer100g()).isNull();
    assertThat(food.sodiumMgPer100g()).isNull();
    assertThat(food.saturatedFatPer100g()).isNull();
  }

  @Test
  void createsAValidFoodWithAllKeyNutrientsKnown() {
    FoodItem food = new FoodItem("oats", "Avena", 389, 16.9, 66.3, 6.9, 60, 10.6, 0.0, 2.0, 1.2);

    assertThat(food.fiberPer100g()).isEqualTo(10.6);
    assertThat(food.sugarsPer100g()).isEqualTo(0.0);
    assertThat(food.sodiumMgPer100g()).isEqualTo(2.0);
    assertThat(food.saturatedFatPer100g()).isEqualTo(1.2);
  }

  @Test
  void rejectsNegativeFiber() {
    assertThatThrownBy(
            () -> new FoodItem("x", "X", 100, 1.0, 1.0, 1.0, 100, -1.0, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("fiberPer100g");
  }

  @Test
  void rejectsNegativeSugars() {
    assertThatThrownBy(
            () -> new FoodItem("x", "X", 100, 1.0, 1.0, 1.0, 100, null, -1.0, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sugarsPer100g");
  }

  @Test
  void rejectsNegativeSodium() {
    assertThatThrownBy(
            () -> new FoodItem("x", "X", 100, 1.0, 1.0, 1.0, 100, null, null, -1.0, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sodiumMgPer100g");
  }

  @Test
  void rejectsNegativeSaturatedFat() {
    assertThatThrownBy(
            () -> new FoodItem("x", "X", 100, 1.0, 1.0, 1.0, 100, null, null, null, -1.0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("saturatedFatPer100g");
  }

  @Test
  void theShortConstructorDefaultsAllKeyNutrientsToNull() {
    // Backward-compatible constructor (pre-FOR-134 call sites) — key nutrients are simply unknown.
    FoodItem food = new FoodItem("chicken", "Pollo", 165, 31.0, 0.0, 3.6, 150);

    assertThat(food.fiberPer100g()).isNull();
    assertThat(food.sugarsPer100g()).isNull();
    assertThat(food.sodiumMgPer100g()).isNull();
    assertThat(food.saturatedFatPer100g()).isNull();
  }
}
