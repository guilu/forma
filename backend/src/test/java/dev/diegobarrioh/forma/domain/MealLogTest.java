package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link MealLog} (FOR-127): the per-day meal-log aggregate that accumulates
 * {@link MealLogEntry} totals. Derived-on-read (spec FOR-127 Open Questions: consumed totals are
 * never stored separately, so they can never drift from the entries) — {@link #consumedTotals()} is
 * always a fresh sum of the current entries. Plain JUnit 5 + AssertJ (ADR-007).
 */
class MealLogTest {

  private static final LocalDate DAY = LocalDate.of(2026, 7, 15);

  @Test
  void emptyLogHasZeroedConsumedTotals() {
    MealLog log = MealLog.empty(DAY);

    NutritionTotals totals = log.consumedTotals();

    assertThat(totals.calories()).isZero();
    assertThat(totals.proteinG()).isZero();
    assertThat(totals.carbsG()).isZero();
    assertThat(totals.fatG()).isZero();
  }

  @Test
  void addingCatalogAndFreeEntriesAccumulatesConsumedMacros() {
    FoodItem oats = FoodCatalog.findById("oats").orElseThrow(); // 389/16.9/66.3/6.9 per 100g
    MealLogEntry catalogEntry =
        MealLogEntry.fromCatalog(DAY, MealType.BREAKFAST, oats, 1.0); // 60g -> 233/10.1/39.8/4.1
    MealLogEntry freeEntry =
        MealLogEntry.freeEntry(
            DAY, MealType.MID_MORNING, "Café con leche", new NutritionTotals(90, 5.0, 8.0, 3.0));

    MealLog log = MealLog.empty(DAY).withEntry(catalogEntry).withEntry(freeEntry);

    assertThat(log.entries()).hasSize(2);
    NutritionTotals totals = log.consumedTotals();
    assertThat(totals.calories()).isEqualTo(catalogEntry.totals().calories() + 90);
    assertThat(totals.proteinG()).isEqualTo(catalogEntry.totals().proteinG() + 5.0);
    assertThat(totals.carbsG()).isEqualTo(catalogEntry.totals().carbsG() + 8.0);
    assertThat(totals.fatG()).isEqualTo(catalogEntry.totals().fatG() + 3.0);
  }

  @Test
  void multipleEntriesForTheSameMealAreAllCountedNeverOverwritten() {
    NutritionTotals macros = new NutritionTotals(100, 10.0, 10.0, 10.0);
    MealLogEntry first = MealLogEntry.freeEntry(DAY, MealType.LUNCH, "A", macros);
    MealLogEntry second = MealLogEntry.freeEntry(DAY, MealType.LUNCH, "B", macros);

    MealLog log = MealLog.empty(DAY).withEntry(first).withEntry(second);

    assertThat(log.entries()).hasSize(2);
    assertThat(log.consumedTotals().calories()).isEqualTo(200);
  }
}
