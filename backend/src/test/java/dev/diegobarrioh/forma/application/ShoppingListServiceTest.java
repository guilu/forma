package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.ShoppingListItem;
import dev.diegobarrioh.forma.domain.ShoppingListStatus;
import dev.diegobarrioh.forma.domain.ShoppingProduct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ShoppingListService} (FOR-39): resolves product names + budget, and toggles
 * checked state (no Spring context — ADR-007).
 */
class ShoppingListServiceTest {

  private final FakeProductRepository products = new FakeProductRepository();
  private final FakeListRepository lists = new FakeListRepository();
  private final ShoppingListService service =
      new ShoppingListService(lists, products, new ShoppingBudgetService(products));

  @Test
  void resolvesProductNamesAndComputesBudget() {
    ShoppingListView view = service.currentView();

    assertThat(view.status()).isEqualTo(ShoppingListStatus.ACTIVE);
    assertThat(view.items())
        .singleElement()
        .satisfies(
            entry -> {
              assertThat(entry.id()).isEqualTo("i1");
              assertThat(entry.productName()).isEqualTo("Avena");
              assertThat(entry.quantity()).isEqualTo(2);
            });
    // 1.95 * 2 = 3.90.
    assertThat(view.budget().weeklyEur()).isEqualByComparingTo("3.90");
  }

  @Test
  void togglesCheckedState() {
    assertThat(service.setChecked("i1", true).item().checked()).isTrue();
  }

  @Test
  void unknownItemThrowsNotFound() {
    assertThatThrownBy(() -> service.setChecked("nope", true))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("nope");
  }

  private static final class FakeListRepository implements ShoppingListRepository {
    @Override
    public Optional<ActiveShoppingList> findActive() {
      StoredShoppingListItem item =
          new StoredShoppingListItem(
              "i1", new ShoppingListItem("p1", 2, new BigDecimal("3.90"), false));
      return Optional.of(
          new ActiveShoppingList(
              "list1", LocalDate.of(2026, 7, 6), ShoppingListStatus.ACTIVE, null, List.of(item)));
    }

    @Override
    public Optional<StoredShoppingListItem> setChecked(String itemId, boolean checked) {
      if (!itemId.equals("i1")) {
        return Optional.empty();
      }
      return Optional.of(
          new StoredShoppingListItem(
              itemId, new ShoppingListItem("p1", 2, new BigDecimal("3.90"), checked)));
    }
  }

  private static final class FakeProductRepository implements ShoppingProductRepository {
    @Override
    public List<StoredShoppingProduct> findAll() {
      return List.of(
          new StoredShoppingProduct(
              "p1",
              new ShoppingProduct(
                  "Avena", null, null, new BigDecimal("1.95"), null, null, null, null)));
    }

    @Override
    public StoredShoppingProduct create(ShoppingProduct product) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<StoredShoppingProduct> update(String id, ShoppingProduct product) {
      throw new UnsupportedOperationException();
    }
  }
}
