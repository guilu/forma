package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.domain.ShoppingList;
import dev.diegobarrioh.forma.domain.ShoppingListItem;
import dev.diegobarrioh.forma.domain.ShoppingListStatus;
import dev.diegobarrioh.forma.domain.ShoppingProduct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link ShoppingBudgetService} (FOR-38): resolves product prices via the repository
 * and computes the budget (no Spring context — ADR-007).
 */
class ShoppingBudgetServiceTest {

  @Test
  void computesBudgetFromCurrentProductPrices() {
    FakeProductRepository repository = new FakeProductRepository();
    ShoppingBudgetService service = new ShoppingBudgetService(repository);

    ShoppingList list =
        new ShoppingList(
            LocalDate.of(2026, 7, 6),
            ShoppingListStatus.ACTIVE,
            List.of(
                new ShoppingListItem("p1", 2, new BigDecimal("0.00"), false),
                new ShoppingListItem("p2", 1, new BigDecimal("0.00"), false)),
            null);

    // p1 1.95 * 2 + p2 3.90 * 1 = 7.80.
    assertThat(service.budgetFor(list).weeklyEur()).isEqualByComparingTo("7.80");
  }

  /** In-memory repository returning two priced products. */
  private static final class FakeProductRepository implements ShoppingProductRepository {
    @Override
    public List<StoredShoppingProduct> findAll() {
      return List.of(
          new StoredShoppingProduct("p1", product(new BigDecimal("1.95"))),
          new StoredShoppingProduct("p2", product(new BigDecimal("3.90"))));
    }

    @Override
    public StoredShoppingProduct create(ShoppingProduct product) {
      throw new UnsupportedOperationException("not used");
    }

    @Override
    public Optional<StoredShoppingProduct> update(String id, ShoppingProduct product) {
      throw new UnsupportedOperationException("not used");
    }

    private static ShoppingProduct product(BigDecimal price) {
      return new ShoppingProduct("x", null, null, price, null, null, null, null);
    }
  }
}
