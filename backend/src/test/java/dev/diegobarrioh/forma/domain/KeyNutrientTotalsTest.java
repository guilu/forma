package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link KeyNutrientTotals} (FOR-134): construction validation and the
 * documented null/partial-total aggregation rule ({@link #sum}) — a nutrient's total is {@code
 * null} if ANY summed part lacks that nutrient (honest: never present a partial sum as complete).
 * Plain JUnit 5 + AssertJ (ADR-007).
 */
class KeyNutrientTotalsTest {

  @Test
  void allowsAllNullValues() {
    KeyNutrientTotals totals = new KeyNutrientTotals(null, null, null, null);

    assertThat(totals.fiberG()).isNull();
    assertThat(totals.sugarsG()).isNull();
    assertThat(totals.sodiumMg()).isNull();
    assertThat(totals.saturatedFatG()).isNull();
  }

  @Test
  void rejectsNegativeFiber() {
    assertThatThrownBy(() -> new KeyNutrientTotals(-1.0, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("fiberG");
  }

  @Test
  void rejectsNegativeSugars() {
    assertThatThrownBy(() -> new KeyNutrientTotals(null, -1.0, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sugarsG");
  }

  @Test
  void rejectsNegativeSodium() {
    assertThatThrownBy(() -> new KeyNutrientTotals(null, null, -1, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sodiumMg");
  }

  @Test
  void rejectsNegativeSaturatedFat() {
    assertThatThrownBy(() -> new KeyNutrientTotals(null, null, null, -1.0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("saturatedFatG");
  }

  @Test
  void emptyIsAllNull() {
    assertThat(KeyNutrientTotals.empty()).isEqualTo(new KeyNutrientTotals(null, null, null, null));
  }

  @Test
  void zeroIsAllZero() {
    assertThat(KeyNutrientTotals.zero()).isEqualTo(new KeyNutrientTotals(0.0, 0.0, 0, 0.0));
  }

  @Test
  void sumOfNoPartsIsZeroNeverNull() {
    assertThat(KeyNutrientTotals.sum(List.of())).isEqualTo(KeyNutrientTotals.zero());
  }

  @Test
  void sumsEachNutrientIndependentlyWhenAllPartsHaveValues() {
    KeyNutrientTotals a = new KeyNutrientTotals(2.0, 3.0, 100, 1.0);
    KeyNutrientTotals b = new KeyNutrientTotals(1.5, 2.0, 50, 0.5);

    KeyNutrientTotals total = KeyNutrientTotals.sum(List.of(a, b));

    assertThat(total.fiberG()).isEqualTo(3.5);
    assertThat(total.sugarsG()).isEqualTo(5.0);
    assertThat(total.sodiumMg()).isEqualTo(150);
    assertThat(total.saturatedFatG()).isEqualTo(1.5);
  }

  @Test
  void nutrientTotalIsNullWhenAnyContributingPartLacksThatNutrient() {
    // Documented FOR-134 rule: a day mixing a food WITH fiber data and one WITHOUT it -> null,
    // never a silently-partial number that looks complete.
    KeyNutrientTotals withFiber = new KeyNutrientTotals(2.0, 3.0, 100, 1.0);
    KeyNutrientTotals withoutFiber = new KeyNutrientTotals(null, 2.0, 50, 0.5);

    KeyNutrientTotals total = KeyNutrientTotals.sum(List.of(withFiber, withoutFiber));

    assertThat(total.fiberG()).isNull();
    // Other nutrients present on both parts still sum normally.
    assertThat(total.sugarsG()).isEqualTo(5.0);
    assertThat(total.sodiumMg()).isEqualTo(150);
    assertThat(total.saturatedFatG()).isEqualTo(1.5);
  }

  @Test
  void aSingleUnknownPartNullsEveryNutrient() {
    KeyNutrientTotals known = new KeyNutrientTotals(2.0, 3.0, 100, 1.0);
    KeyNutrientTotals unknown = KeyNutrientTotals.empty();

    KeyNutrientTotals total = KeyNutrientTotals.sum(List.of(known, unknown));

    assertThat(total.fiberG()).isNull();
    assertThat(total.sugarsG()).isNull();
    assertThat(total.sodiumMg()).isNull();
    assertThat(total.saturatedFatG()).isNull();
  }
}
