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
}
