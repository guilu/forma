package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.ShoppingProductRepository;
import dev.diegobarrioh.forma.application.StoredShoppingProduct;
import dev.diegobarrioh.forma.domain.ShoppingCategory;
import dev.diegobarrioh.forma.domain.ShoppingProduct;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link JdbcShoppingProductRepository} (FOR-36) against the in-memory
 * PostgreSQL-mode H2 with Flyway applied (ADR-007), like the FOR-16 test.
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcShoppingProductRepositoryTest {

  @Autowired private ShoppingProductRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM shopping_products");
  }

  private static ShoppingProduct product(String name, String price) {
    return product(name, price, ShoppingCategory.CEREALES_Y_LEGUMBRES);
  }

  private static ShoppingProduct product(String name, String price, ShoppingCategory category) {
    return new ShoppingProduct(
        name,
        "https://tienda.example/x",
        "1 kg",
        new BigDecimal(price),
        new BigDecimal("1.95"),
        "oats",
        Instant.parse("2026-07-08T10:00:00Z"),
        "nota",
        category);
  }

  @Test
  void createsThenListsWithGeneratedId() {
    StoredShoppingProduct created = repository.create(product("Avena", "1.95"));

    assertThat(created.id()).isNotBlank();
    assertThat(repository.findAll())
        .singleElement()
        .satisfies(
            stored -> {
              assertThat(stored.id()).isEqualTo(created.id());
              assertThat(stored.product().name()).isEqualTo("Avena");
              assertThat(stored.product().estimatedPriceEur()).isEqualByComparingTo("1.95");
              assertThat(stored.product().linkedFoodItemId()).isEqualTo("oats");
              assertThat(stored.product().lastCheckedAt())
                  .isEqualTo(Instant.parse("2026-07-08T10:00:00Z"));
              assertThat(stored.product().category())
                  .isEqualTo(ShoppingCategory.CEREALES_Y_LEGUMBRES);
            });
  }

  @Test
  void updatesAnExistingProduct() {
    StoredShoppingProduct created = repository.create(product("Avena", "1.95"));

    Optional<StoredShoppingProduct> updated =
        repository.update(created.id(), product("Avena integral", "2.30"));

    assertThat(updated).isPresent();
    assertThat(repository.findAll())
        .singleElement()
        .satisfies(
            stored -> {
              assertThat(stored.product().name()).isEqualTo("Avena integral");
              assertThat(stored.product().estimatedPriceEur()).isEqualByComparingTo("2.30");
            });
  }

  @Test
  void updateOfUnknownIdReturnsEmpty() {
    assertThat(repository.update("00000000-0000-0000-0000-000000000000", product("X", "1.00")))
        .isEmpty();
  }

  @Test
  void roundTripsCategory() {
    StoredShoppingProduct created =
        repository.create(product("Platano", "1.80", ShoppingCategory.FRUTAS_Y_VERDURAS));

    assertThat(repository.findAll())
        .singleElement()
        .satisfies(
            stored ->
                assertThat(stored.product().category())
                    .isEqualTo(ShoppingCategory.FRUTAS_Y_VERDURAS));

    Optional<StoredShoppingProduct> updated =
        repository.update(
            created.id(), product("Platano", "1.80", ShoppingCategory.LACTEOS_Y_HUEVOS));

    assertThat(updated).isPresent();
    assertThat(repository.findAll())
        .singleElement()
        .satisfies(
            stored ->
                assertThat(stored.product().category())
                    .isEqualTo(ShoppingCategory.LACTEOS_Y_HUEVOS));
  }

  @Test
  void productWithNoCategoryDefaultsToOtros() {
    repository.create(product("Sin categoria", "1.00", null));

    assertThat(repository.findAll())
        .singleElement()
        .satisfies(
            stored -> assertThat(stored.product().category()).isEqualTo(ShoppingCategory.OTROS));
  }
}
