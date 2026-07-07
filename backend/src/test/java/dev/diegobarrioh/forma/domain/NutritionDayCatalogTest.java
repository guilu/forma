package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for the seeded {@link NutritionDayCatalog} (FOR-33): the three day templates
 * exist with meals, food references resolve, protein lands in range, running carbs exceed rest
 * carbs, and each day's targets match its computed macros.
 */
class NutritionDayCatalogTest {

  private NutritionDay day(NutritionDayType type) {
    return NutritionDayCatalog.findByType(type).orElseThrow();
  }

  @Test
  void hasRunningStrengthAndRestDays() {
    Set<NutritionDayType> types =
        NutritionDayCatalog.days().stream()
            .map(nutritionDay -> nutritionDay.template().type())
            .collect(Collectors.toSet());

    assertThat(types)
        .containsExactlyInAnyOrder(
            NutritionDayType.RUNNING, NutritionDayType.STRENGTH, NutritionDayType.REST);
  }

  @Test
  void everyDayHasMealsAndEveryMealItemResolvesToACatalogFood() {
    assertThat(NutritionDayCatalog.days())
        .allSatisfy(
            nutritionDay -> {
              assertThat(nutritionDay.meals()).isNotEmpty();
              assertThat(nutritionDay.meals())
                  .allSatisfy(
                      meal ->
                          meal.items()
                              .forEach(
                                  item ->
                                      assertThat(FoodCatalog.findById(item.foodItemId()))
                                          .isPresent()));
            });
  }

  @Test
  void dailyProteinLandsAroundOneHundredFiftyToOneHundredSeventy() {
    for (NutritionDay nutritionDay : NutritionDayCatalog.days()) {
      double protein = NutritionCalculator.dayTotals(nutritionDay.meals()).proteinG();
      assertThat(protein).isBetween(150.0, 170.0);
    }
  }

  @Test
  void runningDayHasMoreCarbsThanRestDay() {
    double runningCarbs =
        NutritionCalculator.dayTotals(day(NutritionDayType.RUNNING).meals()).carbsG();
    double restCarbs = NutritionCalculator.dayTotals(day(NutritionDayType.REST).meals()).carbsG();

    assertThat(runningCarbs).isGreaterThan(restCarbs);
  }

  @Test
  void eachDayTargetsMatchItsComputedMacros() {
    for (NutritionDay nutritionDay : NutritionDayCatalog.days()) {
      NutritionTotals totals = NutritionCalculator.dayTotals(nutritionDay.meals());
      NutritionDayTemplate template = nutritionDay.template();

      assertThat(template.targetCalories()).isEqualTo(totals.calories());
      assertThat(template.targetProteinG()).isEqualTo((int) Math.round(totals.proteinG()));
      assertThat(template.targetCarbsG()).isEqualTo((int) Math.round(totals.carbsG()));
    }
  }
}
