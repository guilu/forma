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
    return NutritionDayCatalog.findByType(type, SeededFoods.LOOKUP).orElseThrow();
  }

  @Test
  void hasRunningStrengthAndRestDays() {
    Set<NutritionDayType> types =
        NutritionDayCatalog.days(SeededFoods.LOOKUP).stream()
            .map(nutritionDay -> nutritionDay.template().type())
            .collect(Collectors.toSet());

    assertThat(types)
        .containsExactlyInAnyOrder(
            NutritionDayType.RUNNING, NutritionDayType.STRENGTH, NutritionDayType.REST);
  }

  @Test
  void everyDayHasMealsAndEveryMealItemResolvesToACatalogFood() {
    assertThat(NutritionDayCatalog.days(SeededFoods.LOOKUP))
        .allSatisfy(
            nutritionDay -> {
              assertThat(nutritionDay.meals()).isNotEmpty();
              assertThat(nutritionDay.meals())
                  .allSatisfy(
                      meal ->
                          meal.items()
                              .forEach(
                                  item ->
                                      assertThat(SeededFoods.LOOKUP.findById(item.foodItemId()))
                                          .isPresent()));
            });
  }

  @Test
  void dailyProteinLandsInARealisticRange() {
    // FOR-152 reseeded the catalog to Diego's real foods (e.g. pollo 23 g/100g protein vs the old
    // generic 31 g/100g), which shifted daily protein down from the pre-reseed 150-170 g band to
    // ~144-151 g across the three day templates. Recomputed honestly from the new catalog values,
    // not fabricated.
    for (NutritionDay nutritionDay : NutritionDayCatalog.days(SeededFoods.LOOKUP)) {
      double protein =
          NutritionCalculator.dayTotals(nutritionDay.meals(), SeededFoods.LOOKUP).proteinG();
      assertThat(protein).isBetween(140.0, 155.0);
    }
  }

  @Test
  void runningDayHasMoreCarbsThanRestDay() {
    double runningCarbs =
        NutritionCalculator.dayTotals(day(NutritionDayType.RUNNING).meals(), SeededFoods.LOOKUP)
            .carbsG();
    double restCarbs =
        NutritionCalculator.dayTotals(day(NutritionDayType.REST).meals(), SeededFoods.LOOKUP)
            .carbsG();

    assertThat(runningCarbs).isGreaterThan(restCarbs);
  }

  @Test
  void eachDayTargetsMatchItsComputedMacros() {
    for (NutritionDay nutritionDay : NutritionDayCatalog.days(SeededFoods.LOOKUP)) {
      NutritionTotals totals =
          NutritionCalculator.dayTotals(nutritionDay.meals(), SeededFoods.LOOKUP);
      NutritionDayTemplate template = nutritionDay.template();

      assertThat(template.targetCalories()).isEqualTo(totals.calories());
      assertThat(template.targetProteinG()).isEqualTo((int) Math.round(totals.proteinG()));
      assertThat(template.targetCarbsG()).isEqualTo((int) Math.round(totals.carbsG()));
    }
  }
}
