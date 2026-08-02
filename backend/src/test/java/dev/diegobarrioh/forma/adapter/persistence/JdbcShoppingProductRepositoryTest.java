package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.ShoppingProductRepository;
import dev.diegobarrioh.forma.application.StoredShoppingProduct;
import dev.diegobarrioh.forma.bootstrap.LegacyUserBootstrap;
import dev.diegobarrioh.forma.domain.ShoppingCategory;
import dev.diegobarrioh.forma.domain.ShoppingProduct;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
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

  private static final UUID OWNER = LegacyUserBootstrap.PLACEHOLDER_USER_ID;

  @Autowired private ShoppingProductRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  /**
   * The catalog is Flyway seed data shared by every test in this class, and one test here renames a
   * row to prove a rename reaches the accounts referencing it. Restoring the seeded name up front
   * keeps that test from deciding what the others see — including after it fails halfway.
   */
  @BeforeEach
  void resetFixtures() {
    jdbcTemplate.update("DELETE FROM shopping_products");
    jdbcTemplate.update(
        "UPDATE store_product SET name = ? WHERE id = ?",
        "Copos de avena Brüggen",
        "mercadona-oats");
  }

  /** One column of a stored row, as text, so a null reads as a null and not as an empty string. */
  private String storedColumn(String id, String column) {
    return jdbcTemplate.queryForObject(
        "SELECT CAST(" + column + " AS VARCHAR) FROM shopping_products WHERE id = ?",
        String.class,
        UUID.fromString(id));
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
    StoredShoppingProduct created = repository.create(OWNER, product("Avena", "1.95"));

    assertThat(created.id()).isNotBlank();
    assertThat(repository.findAllByOwner(OWNER))
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
    StoredShoppingProduct created = repository.create(OWNER, product("Avena", "1.95"));

    Optional<StoredShoppingProduct> updated =
        repository.update(OWNER, created.id(), product("Avena integral", "2.30"));

    assertThat(updated).isPresent();
    assertThat(repository.findAllByOwner(OWNER))
        .singleElement()
        .satisfies(
            stored -> {
              assertThat(stored.product().name()).isEqualTo("Avena integral");
              assertThat(stored.product().estimatedPriceEur()).isEqualByComparingTo("2.30");
            });
  }

  @Test
  void updateOfUnknownIdReturnsEmpty() {
    assertThat(
            repository.update(OWNER, "00000000-0000-0000-0000-000000000000", product("X", "1.00")))
        .isEmpty();
  }

  @Test
  void roundTripsCategory() {
    StoredShoppingProduct created =
        repository.create(OWNER, product("Platano", "1.80", ShoppingCategory.FRUTAS_Y_VERDURAS));

    assertThat(repository.findAllByOwner(OWNER))
        .singleElement()
        .satisfies(
            stored ->
                assertThat(stored.product().category())
                    .isEqualTo(ShoppingCategory.FRUTAS_Y_VERDURAS));

    Optional<StoredShoppingProduct> updated =
        repository.update(
            OWNER, created.id(), product("Platano", "1.80", ShoppingCategory.LACTEOS_Y_HUEVOS));

    assertThat(updated).isPresent();
    assertThat(repository.findAllByOwner(OWNER))
        .singleElement()
        .satisfies(
            stored ->
                assertThat(stored.product().category())
                    .isEqualTo(ShoppingCategory.LACTEOS_Y_HUEVOS));
  }

  @Test
  void productWithNoCategoryDefaultsToOtros() {
    repository.create(OWNER, product("Sin categoria", "1.00", null));

    assertThat(repository.findAllByOwner(OWNER))
        .singleElement()
        .satisfies(
            stored -> assertThat(stored.product().category()).isEqualTo(ShoppingCategory.OTROS));
  }

  /**
   * A catalog reference stores nothing of its own, so everything it shows comes from the catalog
   * row (FOR-192, V37) — including the aisle, which the NOT NULL column forces to its OTROS default
   * on insert.
   */
  @Test
  void aCatalogReferenceReadsItsValuesFromTheCatalog() {
    int created =
        repository.addMissingCatalogReferences(OWNER, java.util.List.of("mercadona-oats"));

    assertThat(created).isEqualTo(1);
    assertThat(repository.findAllByOwner(OWNER))
        .singleElement()
        .satisfies(
            stored -> {
              assertThat(stored.product().name()).isEqualTo("Copos de avena Brüggen");
              assertThat(stored.product().estimatedPriceEur()).isEqualByComparingTo("1.30");
              assertThat(stored.product().packageSize()).isEqualTo("Caja 0.8 kg");
              assertThat(stored.product().linkedFoodItemId()).isEqualTo("oats");
              assertThat(stored.product().category())
                  .isEqualTo(ShoppingCategory.CEREALES_Y_LEGUMBRES);
              assertThat(stored.product().storeProductId()).isEqualTo("mercadona-oats");
            });
  }

  /** Regenerating twice must not give an account the same product twice. */
  @Test
  void addingTheSameReferenceTwiceCreatesOneEntry() {
    repository.addMissingCatalogReferences(OWNER, java.util.List.of("mercadona-oats"));
    int created =
        repository.addMissingCatalogReferences(OWNER, java.util.List.of("mercadona-oats"));

    assertThat(created).isZero();
    assertThat(repository.findAllByOwner(OWNER)).hasSize(1);
  }

  /**
   * The account's own price wins over the catalog's and survives a regenerate — that is the whole
   * point of an override. The rest of the row keeps reading through to the catalog.
   */
  @Test
  void anOverriddenPriceWinsAndSurvivesAnotherRegenerate() {
    repository.addMissingCatalogReferences(OWNER, java.util.List.of("mercadona-oats"));
    jdbcTemplate.update(
        "UPDATE shopping_products SET estimated_price_eur = 9.99 WHERE store_product_id = ?",
        "mercadona-oats");

    repository.addMissingCatalogReferences(OWNER, java.util.List.of("mercadona-oats"));

    assertThat(repository.findAllByOwner(OWNER))
        .singleElement()
        .satisfies(
            stored -> {
              assertThat(stored.product().estimatedPriceEur()).isEqualByComparingTo("9.99");
              assertThat(stored.product().name()).isEqualTo("Copos de avena Brüggen");
            });
  }

  /** An account's own product, with no catalog behind it, keeps working exactly as before. */
  @Test
  void aStandaloneProductIsUnaffectedByTheCatalog() {
    repository.create(OWNER, product("Pan de mi panadería", "2.30"));

    assertThat(repository.findAllByOwner(OWNER))
        .singleElement()
        .satisfies(
            stored -> {
              assertThat(stored.product().name()).isEqualTo("Pan de mi panadería");
              assertThat(stored.product().estimatedPriceEur()).isEqualByComparingTo("2.30");
              assertThat(stored.product().storeProductId()).isNull();
            });
  }

  /**
   * Editing one field must override only that field (FOR-192). The request body carries the whole
   * resolved product — the client sends back the name and package it was shown — so a naive update
   * would pin every one of them and quietly cut the row off from the catalog it references.
   */
  @Test
  void editingOnlyThePriceLeavesTheOtherFieldsReadingFromTheCatalog() {
    repository.addMissingCatalogReferences(OWNER, java.util.List.of("mercadona-oats"));
    String id = repository.findAllByOwner(OWNER).get(0).id();

    repository.update(
        OWNER,
        id,
        new ShoppingProduct(
            // Everything as the catalog has it, except the price.
            "Copos de avena Brüggen",
            "https://tienda.mercadona.es/product/86341",
            "Caja 0.8 kg",
            new BigDecimal("9.99"),
            null,
            "oats",
            Instant.now(),
            "Precio no extraíble de HTML público",
            ShoppingCategory.CEREALES_Y_LEGUMBRES));

    // Read column by column: H2 hands back a map keyed by UPPERCASE column names,
    // so asserting on a map would fail for a reason that has nothing to do with
    // the rule under test.
    assertThat(storedColumn(id, "name")).isNull();
    assertThat(storedColumn(id, "package_size")).isNull();
    assertThat(storedColumn(id, "estimated_price_eur")).isEqualTo("9.99");
  }

  /**
   * The consequence of the above, and the reason it matters: the catalog keeps moving the fields
   * nobody overrode. A pinned name would freeze at whatever the shelf said the day someone edited a
   * price.
   */
  @Test
  void aCatalogRenameStillReachesARowWhoseOnlyOverrideIsThePrice() {
    repository.addMissingCatalogReferences(OWNER, java.util.List.of("mercadona-oats"));
    String id = repository.findAllByOwner(OWNER).get(0).id();
    repository.update(
        OWNER,
        id,
        new ShoppingProduct(
            "Copos de avena Brüggen",
            null,
            "Caja 0.8 kg",
            new BigDecimal("9.99"),
            null,
            "oats",
            Instant.now(),
            null,
            ShoppingCategory.CEREALES_Y_LEGUMBRES));

    jdbcTemplate.update(
        "UPDATE store_product SET name = ? WHERE id = ?", "Avena Brüggen 500 g", "mercadona-oats");

    assertThat(repository.findAllByOwner(OWNER))
        .singleElement()
        .satisfies(
            stored -> {
              assertThat(stored.product().name()).isEqualTo("Avena Brüggen 500 g");
              assertThat(stored.product().estimatedPriceEur()).isEqualByComparingTo("9.99");
            });
  }
}
