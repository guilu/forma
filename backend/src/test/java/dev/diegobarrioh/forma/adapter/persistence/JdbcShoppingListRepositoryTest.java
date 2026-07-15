package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.ActiveShoppingList;
import dev.diegobarrioh.forma.application.ShoppingListRepository;
import dev.diegobarrioh.forma.domain.ShoppingListItem;
import dev.diegobarrioh.forma.domain.ShoppingUnit;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link JdbcShoppingListRepository} (FOR-39, FOR-108, FOR-109) against the
 * in-memory PostgreSQL-mode H2 with Flyway applied (ADR-007). Uses its own fixture (not the seed)
 * for determinism.
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcShoppingListRepositoryTest {

  private static final String LIST_ID = "cccccccc-cccc-cccc-cccc-cccccccccccc";
  private static final String ITEM_ID = "dddddddd-dddd-dddd-dddd-dddddddddddd";
  private static final OffsetDateTime GENERATED_AT = OffsetDateTime.parse("2026-07-06T08:00:00Z");

  @Autowired private ShoppingListRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void seedFixture() {
    jdbcTemplate.update("DELETE FROM shopping_list_items");
    jdbcTemplate.update("DELETE FROM shopping_lists");
    jdbcTemplate.update(
        "INSERT INTO shopping_lists (id, week_start_date, status, notes, generated_at) VALUES"
            + " (?, ?, 'ACTIVE', NULL, ?)",
        LIST_ID,
        LocalDate.of(2026, 7, 6),
        GENERATED_AT);
    jdbcTemplate.update(
        "INSERT INTO shopping_list_items (id, shopping_list_id, product_id, quantity,"
            + " estimated_cost_eur, checked, unit, servings) VALUES"
            + " (?, ?, 'p1', 2, 3.90, FALSE, 'KG', 4)",
        ITEM_ID,
        LIST_ID);
  }

  @Test
  void findsTheActiveListWithItems() {
    ActiveShoppingList active = repository.findActive().orElseThrow();

    assertThat(active.id()).isEqualTo(LIST_ID);
    assertThat(active.weekStartDate()).isEqualTo(LocalDate.of(2026, 7, 6));
    assertThat(active.generatedAt()).isEqualTo(GENERATED_AT.toInstant());
    assertThat(active.items())
        .singleElement()
        .satisfies(
            stored -> {
              assertThat(stored.id()).isEqualTo(ITEM_ID);
              assertThat(stored.item().productId()).isEqualTo("p1");
              assertThat(stored.item().checked()).isFalse();
              assertThat(stored.item().unit()).isEqualTo(ShoppingUnit.KG);
              assertThat(stored.item().servings()).isEqualTo(4);
            });
  }

  @Test
  void itemWithNoUnitOrServingsRecordedDefaultsUnitAndLeavesServingsNull() {
    jdbcTemplate.update("DELETE FROM shopping_list_items");
    jdbcTemplate.update(
        "INSERT INTO shopping_list_items (id, shopping_list_id, product_id, quantity,"
            + " estimated_cost_eur, checked) VALUES (?, ?, 'p2', 1, 1.00, FALSE)",
        "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee",
        LIST_ID);

    ActiveShoppingList active = repository.findActive().orElseThrow();

    assertThat(active.items())
        .singleElement()
        .satisfies(
            stored -> {
              assertThat(stored.item().unit()).isEqualTo(ShoppingUnit.UD);
              assertThat(stored.item().servings()).isNull();
            });
  }

  @Test
  void setsItemCheckedState() {
    assertThat(repository.setChecked(ITEM_ID, true)).isPresent();

    assertThat(repository.findActive().orElseThrow().items().get(0).item().checked()).isTrue();
  }

  @Test
  void setCheckedOfUnknownItemReturnsEmpty() {
    assertThat(repository.setChecked("00000000-0000-0000-0000-000000000000", true)).isEmpty();
  }

  @Test
  void regenerateReplacesItemsAndStampsGeneratedAt() {
    Instant newGeneratedAt = Instant.parse("2026-07-13T09:00:00Z");
    List<ShoppingListItem> newItems =
        List.of(
            new ShoppingListItem("p3", 1, new BigDecimal("2.50"), false, ShoppingUnit.UD, null));

    ActiveShoppingList regenerated = repository.regenerate(newItems, newGeneratedAt).orElseThrow();

    assertThat(regenerated.generatedAt()).isEqualTo(newGeneratedAt);
    assertThat(regenerated.items())
        .singleElement()
        .satisfies(
            stored -> {
              assertThat(stored.item().productId()).isEqualTo("p3");
              assertThat(stored.item().quantity()).isEqualTo(1);
              assertThat(stored.item().estimatedCostEur()).isEqualByComparingTo("2.50");
              assertThat(stored.item().checked()).isFalse();
            });

    // Subsequent read reflects the rebuilt list (the original item is gone, not just amended).
    ActiveShoppingList reread = repository.findActive().orElseThrow();
    assertThat(reread.items())
        .singleElement()
        .satisfies(s -> assertThat(s.item().productId()).isEqualTo("p3"));
  }

  @Test
  void regenerateWithEmptyItemsClearsExistingItems() {
    ActiveShoppingList regenerated =
        repository.regenerate(List.of(), Instant.parse("2026-07-13T09:00:00Z")).orElseThrow();

    assertThat(regenerated.items()).isEmpty();
  }

  @Test
  void regenerateWithNoActiveListReturnsEmpty() {
    jdbcTemplate.update("DELETE FROM shopping_list_items");
    jdbcTemplate.update("DELETE FROM shopping_lists");

    assertThat(repository.regenerate(List.of(), Instant.now())).isEmpty();
  }

  @Test
  void updatesItemQuantityAndCost() {
    var updated = repository.updateQuantity(ITEM_ID, 5, new BigDecimal("9.75")).orElseThrow();

    assertThat(updated.item().quantity()).isEqualTo(5);
    assertThat(updated.item().estimatedCostEur()).isEqualByComparingTo("9.75");
    // Subsequent read reflects the new quantity/cost.
    ActiveShoppingList reread = repository.findActive().orElseThrow();
    assertThat(reread.items().get(0).item().quantity()).isEqualTo(5);
  }

  @Test
  void updateQuantityOfUnknownItemReturnsEmpty() {
    assertThat(
            repository.updateQuantity(
                "00000000-0000-0000-0000-000000000000", 3, new BigDecimal("1.00")))
        .isEmpty();
  }

  @Test
  void findsItemById() {
    var found = repository.findItem(ITEM_ID).orElseThrow();

    assertThat(found.id()).isEqualTo(ITEM_ID);
    assertThat(found.item().productId()).isEqualTo("p1");
  }

  @Test
  void findItemOfUnknownIdReturnsEmpty() {
    assertThat(repository.findItem("00000000-0000-0000-0000-000000000000")).isEmpty();
  }
}
