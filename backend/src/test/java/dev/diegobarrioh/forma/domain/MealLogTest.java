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

  // --- FOR-134: consumedKeyNutrients reuses KeyNutrientTotals.sum (the documented null/partial
  // rule: a nutrient's day total is null if ANY logged entry that day lacks it). ---

  @Test
  void emptyLogHasZeroedKeyNutrientTotalsNeverNull() {
    MealLog log = MealLog.empty(DAY);

    KeyNutrientTotals keyNutrients = log.consumedKeyNutrients();

    assertThat(keyNutrients).isEqualTo(KeyNutrientTotals.zero());
  }

  @Test
  void keyNutrientsSumAcrossEntriesWhenAllHaveTheData() {
    MealLogEntry a =
        MealLogEntry.freeEntry(
            DAY,
            MealType.BREAKFAST,
            "A",
            new NutritionTotals(100, 1.0, 1.0, 1.0),
            new KeyNutrientTotals(2.0, 3.0, 100, 1.0));
    MealLogEntry b =
        MealLogEntry.freeEntry(
            DAY,
            MealType.LUNCH,
            "B",
            new NutritionTotals(100, 1.0, 1.0, 1.0),
            new KeyNutrientTotals(1.0, 2.0, 50, 0.5));

    MealLog log = MealLog.empty(DAY).withEntry(a).withEntry(b);

    KeyNutrientTotals total = log.consumedKeyNutrients();
    assertThat(total.fiberG()).isEqualTo(3.0);
    assertThat(total.sugarsG()).isEqualTo(5.0);
    assertThat(total.sodiumMg()).isEqualTo(150);
    assertThat(total.saturatedFatG()).isEqualTo(1.5);
  }

  @Test
  void aDayMixingAFoodWithAndWithoutFiberNullsOnlyTheFiberTotal() {
    MealLogEntry withFiber =
        MealLogEntry.freeEntry(
            DAY,
            MealType.BREAKFAST,
            "A",
            new NutritionTotals(100, 1.0, 1.0, 1.0),
            new KeyNutrientTotals(2.0, 3.0, 100, 1.0));
    MealLogEntry withoutFiber =
        MealLogEntry.freeEntry(
            DAY,
            MealType.LUNCH,
            "B",
            new NutritionTotals(100, 1.0, 1.0, 1.0),
            new KeyNutrientTotals(null, 2.0, 50, 0.5));

    MealLog log = MealLog.empty(DAY).withEntry(withFiber).withEntry(withoutFiber);

    KeyNutrientTotals total = log.consumedKeyNutrients();
    assertThat(total.fiberG()).isNull();
    assertThat(total.sugarsG()).isEqualTo(5.0);
    assertThat(total.sodiumMg()).isEqualTo(150);
    assertThat(total.saturatedFatG()).isEqualTo(1.5);
  }

  @Test
  void aCatalogEntryWithNoKeyNutrientDataNullsTheAffectedTotals() {
    FoodItem vegetables =
        FoodCatalog.findById("vegetables").orElseThrow(); // all key nutrients null
    MealLogEntry entry = MealLogEntry.fromCatalog(DAY, MealType.LUNCH, vegetables, 1.0);

    MealLog log = MealLog.empty(DAY).withEntry(entry);

    KeyNutrientTotals total = log.consumedKeyNutrients();
    assertThat(total.fiberG()).isNull();
    assertThat(total.sugarsG()).isNull();
    assertThat(total.sodiumMg()).isNull();
    assertThat(total.saturatedFatG()).isNull();
  }
}
