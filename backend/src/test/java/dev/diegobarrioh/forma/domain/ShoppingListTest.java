package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link ShoppingList} and {@link ShoppingListItem} (FOR-37): creation,
 * check/uncheck, the constrained status, empty-list validity, and item validation.
 */
class ShoppingListTest {

  private static ShoppingListItem item(String productId, boolean checked) {
    return new ShoppingListItem(productId, 2, new BigDecimal("3.90"), checked);
  }

  @Test
  void createsAListWithItemsReferencingProducts() {
    ShoppingList list =
        new ShoppingList(
            LocalDate.of(2026, 7, 6),
            ShoppingListStatus.ACTIVE,
            List.of(item("p1", false), item("p2", true)),
            "Semana");

    assertThat(list.status()).isEqualTo(ShoppingListStatus.ACTIVE);
    assertThat(list.items()).hasSize(2);
    assertThat(list.items().get(0).productId()).isEqualTo("p1");
  }

  @Test
  void anEmptyListIsValid() {
    ShoppingList empty =
        new ShoppingList(LocalDate.of(2026, 7, 6), ShoppingListStatus.DRAFT, List.of(), null);

    assertThat(empty.items()).isEmpty();
  }

  @Test
  void statusIsConstrained() {
    assertThat(ShoppingListStatus.values())
        .containsExactlyInAnyOrder(
            ShoppingListStatus.DRAFT, ShoppingListStatus.ACTIVE, ShoppingListStatus.DONE);
  }

  @Test
  void itemCanBeCheckedAndUnchecked() {
    ShoppingListItem unchecked = item("p1", false);

    assertThat(unchecked.withChecked(true).checked()).isTrue();
    assertThat(unchecked.withChecked(true).withChecked(false).checked()).isFalse();
    // The original is unchanged (immutable record).
    assertThat(unchecked.checked()).isFalse();
  }

  @Nested
  class ItemValidation {

    @Test
    void rejectsBlankProductId() {
      assertThatThrownBy(() -> new ShoppingListItem(" ", 1, BigDecimal.ONE, false))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("productId");
    }

    @Test
    void rejectsNonPositiveQuantity() {
      assertThatThrownBy(() -> new ShoppingListItem("p1", 0, BigDecimal.ONE, false))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("quantity");
    }

    @Test
    void rejectsNegativeCost() {
      assertThatThrownBy(() -> new ShoppingListItem("p1", 1, new BigDecimal("-1.00"), false))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("estimatedCostEur");
    }
  }
}
