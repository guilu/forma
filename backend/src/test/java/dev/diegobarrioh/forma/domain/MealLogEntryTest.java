package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    // whey-protein: 400 kcal/80P/8C/6F per 100g, defaultServingG=30 -> 1 portion = 30g.
    FoodItem wheyProtein = FoodCatalog.findById("whey-protein").orElseThrow();

    MealLogEntry entry = MealLogEntry.fromCatalog(DAY, MealType.BREAKFAST, wheyProtein, 1.0);

    assertThat(entry.date()).isEqualTo(DAY);
    assertThat(entry.mealType()).isEqualTo(MealType.BREAKFAST);
    assertThat(entry.foodItemId()).isEqualTo("whey-protein");
    assertThat(entry.name()).isEqualTo("Proteína whey");
    assertThat(entry.totals().calories()).isEqualTo(120); // 400 * 0.3
    assertThat(entry.totals().proteinG()).isEqualTo(24.0); // 80 * 0.3
    assertThat(entry.totals().carbsG()).isEqualTo(2.4); // 8 * 0.3
    assertThat(entry.totals().fatG()).isEqualTo(1.8); // 6 * 0.3
  }

  @Test
  void fromCatalogScalesQuantityByPortionsTimesDefaultServing() {
    // oats: defaultServingG=60, 1.5 portions -> 90g. kcal 389/100g -> 350.1 -> 350.
    FoodItem oats = FoodCatalog.findById("oats").orElseThrow();

    MealLogEntry entry = MealLogEntry.fromCatalog(DAY, MealType.LUNCH, oats, 1.5);

    assertThat(entry.totals().calories()).isEqualTo(350);
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
