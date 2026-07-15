package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link ShoppingList} and {@link ShoppingListItem} (FOR-37, FOR-108):
 * creation, check/uncheck, the constrained status, empty-list validity, item validation, and the
 * FOR-108 {@code unit}/{@code servings}/{@code generatedAt} fields.
 */
class ShoppingListTest {

  private static final Instant GENERATED_AT = Instant.parse("2026-07-06T08:00:00Z");

  private static ShoppingListItem item(String productId, boolean checked) {
    return new ShoppingListItem(
        productId, 2, new BigDecimal("3.90"), checked, ShoppingUnit.UD, null);
  }

  @Test
  void createsAListWithItemsReferencingProducts() {
    ShoppingList list =
        new ShoppingList(
            LocalDate.of(2026, 7, 6),
            ShoppingListStatus.ACTIVE,
            List.of(item("p1", false), item("p2", true)),
            "Semana",
            GENERATED_AT);

    assertThat(list.status()).isEqualTo(ShoppingListStatus.ACTIVE);
    assertThat(list.items()).hasSize(2);
    assertThat(list.items().get(0).productId()).isEqualTo("p1");
    assertThat(list.generatedAt()).isEqualTo(GENERATED_AT);
  }

  @Test
  void anEmptyListIsValid() {
    ShoppingList empty =
        new ShoppingList(
            LocalDate.of(2026, 7, 6), ShoppingListStatus.DRAFT, List.of(), null, GENERATED_AT);

    assertThat(empty.items()).isEmpty();
  }

  @Test
  void rejectsMissingGeneratedAt() {
    assertThatThrownBy(
            () ->
                new ShoppingList(
                    LocalDate.of(2026, 7, 6), ShoppingListStatus.DRAFT, List.of(), null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("generatedAt");
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

  @Test
  void withCheckedPreservesUnitAndServings() {
    ShoppingListItem withServings =
        new ShoppingListItem("p1", 2, new BigDecimal("3.90"), false, ShoppingUnit.PAQUETE, 4);

    ShoppingListItem toggled = withServings.withChecked(true);

    assertThat(toggled.unit()).isEqualTo(ShoppingUnit.PAQUETE);
    assertThat(toggled.servings()).isEqualTo(4);
    assertThat(toggled.checked()).isTrue();
  }

  @Test
  void defaultsUnitToUdWhenAbsent() {
    ShoppingListItem noUnit =
        new ShoppingListItem("p1", 2, new BigDecimal("3.90"), false, null, null);

    assertThat(noUnit.unit()).isEqualTo(ShoppingUnit.UD);
  }

  @Test
  void servingsIsNullWhenNotApplicable() {
    ShoppingListItem nonFood =
        new ShoppingListItem("p1", 1, BigDecimal.ONE, false, ShoppingUnit.UD, null);

    assertThat(nonFood.servings()).isNull();
  }

  @Nested
  class ItemValidation {

    @Test
    void rejectsBlankProductId() {
      assertThatThrownBy(
              () -> new ShoppingListItem(" ", 1, BigDecimal.ONE, false, ShoppingUnit.UD, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("productId");
    }

    @Test
    void rejectsNonPositiveQuantity() {
      assertThatThrownBy(
              () -> new ShoppingListItem("p1", 0, BigDecimal.ONE, false, ShoppingUnit.UD, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("quantity");
    }

    @Test
    void rejectsNegativeCost() {
      assertThatThrownBy(
              () ->
                  new ShoppingListItem(
                      "p1", 1, new BigDecimal("-1.00"), false, ShoppingUnit.UD, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("estimatedCostEur");
    }

    @Test
    void rejectsNonPositiveServingsWhenPresent() {
      assertThatThrownBy(
              () -> new ShoppingListItem("p1", 1, BigDecimal.ONE, false, ShoppingUnit.UD, 0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("servings");
    }
  }
}
