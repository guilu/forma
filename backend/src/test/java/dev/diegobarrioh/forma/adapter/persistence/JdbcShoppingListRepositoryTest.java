package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.ActiveShoppingList;
import dev.diegobarrioh.forma.application.ShoppingListRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link JdbcShoppingListRepository} (FOR-39) against the in-memory
 * PostgreSQL-mode H2 with Flyway applied (ADR-007). Uses its own fixture (not the seed) for
 * determinism.
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcShoppingListRepositoryTest {

  private static final String LIST_ID = "cccccccc-cccc-cccc-cccc-cccccccccccc";
  private static final String ITEM_ID = "dddddddd-dddd-dddd-dddd-dddddddddddd";

  @Autowired private ShoppingListRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void seedFixture() {
    jdbcTemplate.update("DELETE FROM shopping_list_items");
    jdbcTemplate.update("DELETE FROM shopping_lists");
    jdbcTemplate.update(
        "INSERT INTO shopping_lists (id, week_start_date, status, notes) VALUES (?, ?, 'ACTIVE', NULL)",
        LIST_ID,
        LocalDate.of(2026, 7, 6));
    jdbcTemplate.update(
        "INSERT INTO shopping_list_items (id, shopping_list_id, product_id, quantity,"
            + " estimated_cost_eur, checked) VALUES (?, ?, 'p1', 2, 3.90, FALSE)",
        ITEM_ID,
        LIST_ID);
  }

  @Test
  void findsTheActiveListWithItems() {
    ActiveShoppingList active = repository.findActive().orElseThrow();

    assertThat(active.id()).isEqualTo(LIST_ID);
    assertThat(active.weekStartDate()).isEqualTo(LocalDate.of(2026, 7, 6));
    assertThat(active.items())
        .singleElement()
        .satisfies(
            stored -> {
              assertThat(stored.id()).isEqualTo(ITEM_ID);
              assertThat(stored.item().productId()).isEqualTo("p1");
              assertThat(stored.item().checked()).isFalse();
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
}
