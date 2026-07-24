package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.application.CatalogFood;
import dev.diegobarrioh.forma.application.FoodCatalogRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link JdbcFoodCatalogRepository} (FOR-173) against the in-memory
 * PostgreSQL-mode H2 with Flyway applied (ADR-007), asserting the V25 seed data directly -- no
 * {@code @BeforeEach} cleanup, since the migration IS the fixture under test.
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcFoodCatalogRepositoryTest {

  @Autowired private FoodCatalogRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void findAllReturnsAllTwentyThreeSeededRows() {
    assertThat(repository.findAll()).hasSize(23);
  }

  @Test
  void findByIdReturnsFullyPopulatedFood() {
    Optional<CatalogFood> oats = repository.findById("oats");

    assertThat(oats).isPresent();
    CatalogFood food = oats.get();
    assertThat(food.name()).isEqualTo("Copos de avena");
    assertThat(food.kcal()).isEqualTo(370);
    assertThat(food.proteinG()).isEqualByComparingTo("13.0");
    assertThat(food.carbsG()).isEqualByComparingTo("60.0");
    assertThat(food.fatG()).isEqualByComparingTo("7.0");
    assertThat(food.servingSizeG()).isEqualByComparingTo("60.0");
    assertThat(food.fiberG()).isEqualByComparingTo("10.6");
    assertThat(food.sugarsG()).isEqualByComparingTo("0.0");
    assertThat(food.sodiumMg()).isEqualByComparingTo("2.0");
    assertThat(food.saturatedFatG()).isEqualByComparingTo("1.2");
  }

  @Test
  void findByIdReturnsFoodWithAllKeyNutrientsNull() {
    Optional<CatalogFood> rice = repository.findById("rice");

    assertThat(rice).isPresent();
    CatalogFood food = rice.get();
    assertThat(food.kcal()).isEqualTo(360);
    assertThat(food.proteinG()).isEqualByComparingTo("7.0");
    assertThat(food.carbsG()).isEqualByComparingTo("79.0");
    assertThat(food.fatG()).isEqualByComparingTo("1.0");
    assertThat(food.fiberG()).isNull();
    assertThat(food.sugarsG()).isNull();
    assertThat(food.sodiumMg()).isNull();
    assertThat(food.saturatedFatG()).isNull();
  }

  @Test
  void findByIdReturnsPartiallyPopulatedFood() {
    Optional<CatalogFood> eggs = repository.findById("eggs");

    assertThat(eggs).isPresent();
    CatalogFood food = eggs.get();
    assertThat(food.fiberG()).isEqualByComparingTo("0.0");
    assertThat(food.sugarsG()).isNull();
    assertThat(food.sodiumMg()).isEqualByComparingTo("124.0");
    assertThat(food.saturatedFatG()).isEqualByComparingTo("3.3");
  }

  @Test
  void findByIdOfUnknownIdReturnsEmpty() {
    assertThat(repository.findById("does-not-exist")).isEmpty();
  }

  @Test
  void insertingShoppingProductWithKnownLinkedFoodSucceeds() {
    UUID id = UUID.randomUUID();

    jdbcTemplate.update(
        "INSERT INTO shopping_products (id, name, estimated_price_eur, linked_food_item_id)"
            + " VALUES (?, ?, ?, ?)",
        id,
        "Test product",
        new BigDecimal("1.00"),
        "oats");

    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM shopping_products WHERE id = ?", Integer.class, id);
    assertThat(count).isEqualTo(1);
  }

  @Test
  void insertingShoppingProductWithBogusLinkedFoodFails() {
    UUID id = UUID.randomUUID();

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "INSERT INTO shopping_products (id, name, estimated_price_eur,"
                        + " linked_food_item_id) VALUES (?, ?, ?, ?)",
                    id,
                    "Test product",
                    new BigDecimal("1.00"),
                    "no-such-food"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void insertingShoppingProductWithNullLinkedFoodSucceeds() {
    UUID id = UUID.randomUUID();

    jdbcTemplate.update(
        "INSERT INTO shopping_products (id, name, estimated_price_eur, linked_food_item_id)"
            + " VALUES (?, ?, ?, ?)",
        id,
        "Test product",
        new BigDecimal("1.00"),
        (String) null);

    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM shopping_products WHERE id = ?", Integer.class, id);
    assertThat(count).isEqualTo(1);
  }

  @Test
  void noShoppingProductHasADanglingLinkedFoodId() {
    Integer danglingCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM shopping_products"
                + " WHERE linked_food_item_id IS NOT NULL"
                + " AND linked_food_item_id NOT IN (SELECT id FROM food_catalog)",
            Integer.class);

    assertThat(danglingCount).isZero();
  }
}
