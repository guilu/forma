package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link MealLogEntry} (FOR-127): building a logged entry from a FOR-30
 * catalog food + portions (reusing {@link NutritionCalculator}, no duplicated macro math) or from
 * free/ad-hoc macros, and construction validation. Plain JUnit 5 + AssertJ (ADR-007).
 */
class MealLogEntryTest {

  private static final LocalDate DAY = LocalDate.of(2026, 7, 15);

  @Test
  void fromCatalogComputesTotalsFromFoodItemAndPortionsViaNutritionCalculator() {
    // whey-protein: 390 kcal/78P/8C/6F per 100g, defaultServingG=30 -> 1 portion = 30g.
    FoodItem wheyProtein = FoodCatalog.findById("whey-protein").orElseThrow();

    MealLogEntry entry = MealLogEntry.fromCatalog(DAY, MealType.BREAKFAST, wheyProtein, 1.0);

    assertThat(entry.date()).isEqualTo(DAY);
    assertThat(entry.mealType()).isEqualTo(MealType.BREAKFAST);
    assertThat(entry.foodItemId()).isEqualTo("whey-protein");
    assertThat(entry.name()).isEqualTo("Whey proteína");
    assertThat(entry.totals().calories()).isEqualTo(117); // 390 * 0.3
    assertThat(entry.totals().proteinG()).isEqualTo(23.4); // 78 * 0.3
    assertThat(entry.totals().carbsG()).isEqualTo(2.4); // 8 * 0.3
    assertThat(entry.totals().fatG()).isEqualTo(1.8); // 6 * 0.3
  }

  @Test
  void fromCatalogScalesQuantityByPortionsTimesDefaultServing() {
    // oats: defaultServingG=60, 1.5 portions -> 90g. kcal 370/100g -> 333.
    FoodItem oats = FoodCatalog.findById("oats").orElseThrow();

    MealLogEntry entry = MealLogEntry.fromCatalog(DAY, MealType.LUNCH, oats, 1.5);

    assertThat(entry.totals().calories()).isEqualTo(333);
  }

  @Test
  void freeEntryStoresProvidedMacrosAsIsWithNoCatalogReference() {
    NutritionTotals macros = new NutritionTotals(90, 5.0, 8.0, 3.0);

    MealLogEntry entry =
        MealLogEntry.freeEntry(DAY, MealType.MID_MORNING, "Café con leche", macros);

    assertThat(entry.foodItemId()).isNull();
    assertThat(entry.name()).isEqualTo("Café con leche");
    assertThat(entry.totals()).isEqualTo(macros);
  }

  // --- FOR-134: key nutrients travel alongside macros, computed once at logging time ---

  @Test
  void fromCatalogComputesKeyNutrientsViaNutritionCalculatorReusingTheSameScaling() {
    FoodItem oats = FoodCatalog.findById("oats").orElseThrow(); // fiber 10.6/100g

    MealLogEntry entry = MealLogEntry.fromCatalog(DAY, MealType.LUNCH, oats, 1.0); // 60g -> x0.6

    assertThat(entry.keyNutrients().fiberG()).isCloseTo(6.4, within(0.05));
  }

  @Test
  void fromCatalogPropagatesNullKeyNutrientsForAFoodWithNoData() {
    FoodItem vegetables = FoodCatalog.findById("vegetables").orElseThrow();

    MealLogEntry entry = MealLogEntry.fromCatalog(DAY, MealType.LUNCH, vegetables, 1.0);

    assertThat(entry.keyNutrients()).isEqualTo(KeyNutrientTotals.empty());
  }

  @Test
  void theShortFreeEntryFactoryDefaultsKeyNutrientsToUnknown() {
    NutritionTotals macros = new NutritionTotals(90, 5.0, 8.0, 3.0);

    MealLogEntry entry = MealLogEntry.freeEntry(DAY, MealType.MID_MORNING, "Café", macros);

    assertThat(entry.keyNutrients()).isEqualTo(KeyNutrientTotals.empty());
  }

  @Test
  void freeEntryAcceptsOptionalKeyNutrientsProvidedByTheCaller() {
    NutritionTotals macros = new NutritionTotals(180, 6.0, 24.0, 7.0);
    KeyNutrientTotals keyNutrients = new KeyNutrientTotals(3.0, 12.0, 90, 2.0);

    MealLogEntry entry =
        MealLogEntry.freeEntry(DAY, MealType.MID_MORNING, "Barrita", macros, keyNutrients);

    assertThat(entry.keyNutrients()).isEqualTo(keyNutrients);
  }

  @Test
  void rejectsANullDate() {
    NutritionTotals macros = new NutritionTotals(90, 5.0, 8.0, 3.0);
    assertThatThrownBy(() -> MealLogEntry.freeEntry(null, MealType.LUNCH, "X", macros))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsABlankName() {
    NutritionTotals macros = new NutritionTotals(90, 5.0, 8.0, 3.0);
    assertThatThrownBy(() -> MealLogEntry.freeEntry(DAY, MealType.LUNCH, " ", macros))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
