package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link ShoppingProduct} (FOR-35): creation, the optional food link, and
 * construction validation. Plain JUnit 5 + AssertJ (ADR-007).
 */
class ShoppingProductTest {

  private static ShoppingProduct product(String linkedFoodItemId, BigDecimal price) {
    return new ShoppingProduct(
        "Avena 1 kg",
        "https://tienda.example/avena",
        "1 kg",
        price,
        new BigDecimal("1.95"),
        linkedFoodItemId,
        null,
        "Marca blanca",
        ShoppingCategory.CEREALES_Y_LEGUMBRES);
  }

  @Test
  void createsAProductLinkedToAFood() {
    ShoppingProduct linked = product("oats", new BigDecimal("1.95"));

    assertThat(linked.name()).isEqualTo("Avena 1 kg");
    assertThat(linked.linkedFoodItemId()).isEqualTo("oats");
    assertThat(linked.estimatedPriceEur()).isEqualByComparingTo("1.95");
  }

  @Test
  void createsAnUnlinkedProduct() {
    ShoppingProduct unlinked =
        new ShoppingProduct(
            "Bolsas", null, null, new BigDecimal("0.90"), null, null, null, null, null);

    assertThat(unlinked.linkedFoodItemId()).isNull();
    assertThat(unlinked.pricePerUnitEur()).isNull();
  }

  @Test
  void rejectsBlankName() {
    assertThatThrownBy(
            () ->
                new ShoppingProduct(
                    " ", null, null, new BigDecimal("1.00"), null, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("name");
  }

  @Test
  void rejectsMissingEstimatedPrice() {
    assertThatThrownBy(
            () -> new ShoppingProduct("Avena", null, null, null, null, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("estimatedPriceEur");
  }

  @Test
  void rejectsNonPositivePrice() {
    assertThatThrownBy(() -> product("oats", BigDecimal.ZERO))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("estimatedPriceEur");
    assertThatThrownBy(() -> product("oats", new BigDecimal("-1.00")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("estimatedPriceEur");
  }

  @Test
  void acceptsAnExplicitCategory() {
    ShoppingProduct linked = product("oats", new BigDecimal("1.95"));

    assertThat(linked.category()).isEqualTo(ShoppingCategory.CEREALES_Y_LEGUMBRES);
  }

  @Test
  void defaultsCategoryToOtrosWhenAbsent() {
    ShoppingProduct noCategory =
        new ShoppingProduct(
            "Bolsas", null, null, new BigDecimal("0.90"), null, null, null, null, null);

    assertThat(noCategory.category()).isEqualTo(ShoppingCategory.OTROS);
  }
}
