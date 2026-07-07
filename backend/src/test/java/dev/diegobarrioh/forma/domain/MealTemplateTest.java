package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link MealTemplate} and {@link MealItem} (FOR-31): construction
 * validation, multi-item meals, the constrained meal type, and the day-template association. Plain
 * JUnit 5 + AssertJ (ADR-007).
 */
class MealTemplateTest {

  private static MealTemplate breakfast(List<MealItem> items) {
    return new MealTemplate(
        NutritionDayType.RUNNING, MealType.BREAKFAST, "Desayuno", LocalTime.of(8, 0), items, null);
  }

  @Test
  void createsAMealWithMultipleItemsBelongingToADay() {
    MealTemplate meal = breakfast(List.of(new MealItem("oats", 60), new MealItem("banana", 120)));

    assertThat(meal.dayType()).isEqualTo(NutritionDayType.RUNNING);
    assertThat(meal.mealType()).isEqualTo(MealType.BREAKFAST);
    assertThat(meal.items()).hasSize(2);
    assertThat(meal.preferredTime()).isEqualTo(LocalTime.of(8, 0));
  }

  @Test
  void mealTypeIsConstrained() {
    assertThat(MealType.values())
        .containsExactlyInAnyOrder(
            MealType.BREAKFAST,
            MealType.MID_MORNING,
            MealType.LUNCH,
            MealType.PRE_WORKOUT,
            MealType.POST_WORKOUT,
            MealType.DINNER);
  }

  @Nested
  class ItemValidation {

    @Test
    void rejectsBlankFoodItemId() {
      assertThatThrownBy(() -> new MealItem(" ", 60))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("foodItemId");
    }

    @Test
    void rejectsNonPositiveQuantity() {
      assertThatThrownBy(() -> new MealItem("oats", 0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("quantityG");
    }
  }

  @Nested
  class TemplateValidation {

    @Test
    void rejectsEmptyMeal() {
      assertThatThrownBy(() -> breakfast(List.of()))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("at least one item");
    }

    @Test
    void requiresDayTypeMealTypeAndTime() {
      List<MealItem> items = List.of(new MealItem("oats", 60));
      assertThatThrownBy(
              () ->
                  new MealTemplate(
                      null, MealType.BREAKFAST, "Desayuno", LocalTime.of(8, 0), items, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("dayType");
      assertThatThrownBy(
              () ->
                  new MealTemplate(
                      NutritionDayType.RUNNING, MealType.BREAKFAST, "Desayuno", null, items, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("preferredTime");
    }
  }
}
