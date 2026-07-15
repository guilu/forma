package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.ShoppingCategory;
import dev.diegobarrioh.forma.domain.ShoppingListItem;
import dev.diegobarrioh.forma.domain.ShoppingListStatus;
import dev.diegobarrioh.forma.domain.ShoppingProduct;
import dev.diegobarrioh.forma.domain.ShoppingUnit;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ShoppingListService} (FOR-39, FOR-108): resolves product names + budget,
 * threads {@code unit}/{@code servings}/{@code generatedAt} (FOR-108), and toggles checked state
 * (no Spring context — ADR-007).
 */
class ShoppingListServiceTest {

  private static final Instant GENERATED_AT = Instant.parse("2026-07-06T08:00:00Z");

  private final FakeProductRepository products = new FakeProductRepository();
  private final FakeListRepository lists = new FakeListRepository();
  private final ShoppingListService service =
      new ShoppingListService(lists, products, new ShoppingBudgetService(products));

  @Test
  void resolvesProductNamesAndComputesBudget() {
    ShoppingListView view = service.currentView();

    assertThat(view.status()).isEqualTo(ShoppingListStatus.ACTIVE);
    assertThat(view.generatedAt()).isEqualTo(GENERATED_AT);
    assertThat(view.items())
        .singleElement()
        .satisfies(
            entry -> {
              assertThat(entry.id()).isEqualTo("i1");
              assertThat(entry.productId()).isEqualTo("p1");
              assertThat(entry.productName()).isEqualTo("Avena");
              assertThat(entry.category()).isEqualTo(ShoppingCategory.CEREALES_Y_LEGUMBRES);
              assertThat(entry.quantity()).isEqualTo(2);
              assertThat(entry.unit()).isEqualTo(ShoppingUnit.KG);
              // p1 ("Avena") is not linked to a food item in this fixture -> no servings.
              assertThat(entry.servings()).isNull();
            });
    // 1.95 * 2 = 3.90.
    assertThat(view.budget().weeklyEur()).isEqualByComparingTo("3.90");
  }

  @Test
  void resolvesServingsWhenProductIsLinkedToAFood() {
    FakeProductRepository linkedProducts = new FakeProductRepository("oats");
    FakeListRepository listsWithServings = new FakeListRepository(4);
    ShoppingListService serviceWithLinkedProduct =
        new ShoppingListService(
            listsWithServings, linkedProducts, new ShoppingBudgetService(linkedProducts));

    ShoppingListView view = serviceWithLinkedProduct.currentView();

    assertThat(view.items())
        .singleElement()
        .satisfies(entry -> assertThat(entry.servings()).isEqualTo(4));
  }

  @Test
  void nonFoodLinkedItemHasNullServingsEvenIfStored() {
    // The product is not linked to a food (fixture default), so even a raw stored servings value
    // must not surface — never fabricate/leak servings for non-food items (spec edge case).
    FakeListRepository listsWithRawServings = new FakeListRepository(4);
    ShoppingListService serviceWithUnlinkedProduct =
        new ShoppingListService(
            listsWithRawServings, products, new ShoppingBudgetService(products));

    ShoppingListView view = serviceWithUnlinkedProduct.currentView();

    assertThat(view.items())
        .singleElement()
        .satisfies(entry -> assertThat(entry.servings()).isNull());
  }

  @Test
  void unresolvedProductIdFallsBackToIdAsNameAndOtrosCategory() {
    ShoppingListService serviceWithNoProducts =
        new ShoppingListService(
            lists,
            new ShoppingProductRepository() {
              @Override
              public List<StoredShoppingProduct> findAll() {
                return List.of();
              }

              @Override
              public StoredShoppingProduct create(ShoppingProduct product) {
                throw new UnsupportedOperationException();
              }

              @Override
              public Optional<StoredShoppingProduct> update(String id, ShoppingProduct product) {
                throw new UnsupportedOperationException();
              }
            },
            new ShoppingBudgetService(products));

    ShoppingListView view = serviceWithNoProducts.currentView();

    assertThat(view.items())
        .singleElement()
        .satisfies(
            entry -> {
              assertThat(entry.productId()).isEqualTo("p1");
              assertThat(entry.productName()).isEqualTo("p1");
              assertThat(entry.category()).isEqualTo(ShoppingCategory.OTROS);
              assertThat(entry.servings()).isNull();
            });
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
    private final Integer storedServings;

    FakeListRepository() {
      this(null);
    }

    FakeListRepository(Integer storedServings) {
      this.storedServings = storedServings;
    }

    @Override
    public Optional<ActiveShoppingList> findActive() {
      StoredShoppingListItem item =
          new StoredShoppingListItem(
              "i1",
              new ShoppingListItem(
                  "p1", 2, new BigDecimal("3.90"), false, ShoppingUnit.KG, storedServings));
      return Optional.of(
          new ActiveShoppingList(
              "list1",
              LocalDate.of(2026, 7, 6),
              ShoppingListStatus.ACTIVE,
              null,
              List.of(item),
              GENERATED_AT));
    }

    @Override
    public Optional<StoredShoppingListItem> setChecked(String itemId, boolean checked) {
      if (!itemId.equals("i1")) {
        return Optional.empty();
      }
      return Optional.of(
          new StoredShoppingListItem(
              itemId,
              new ShoppingListItem(
                  "p1", 2, new BigDecimal("3.90"), checked, ShoppingUnit.KG, storedServings)));
    }
  }

  private static final class FakeProductRepository implements ShoppingProductRepository {
    private final String linkedFoodItemId;

    FakeProductRepository() {
      this(null);
    }

    FakeProductRepository(String linkedFoodItemId) {
      this.linkedFoodItemId = linkedFoodItemId;
    }

    @Override
    public List<StoredShoppingProduct> findAll() {
      return List.of(
          new StoredShoppingProduct(
              "p1",
              new ShoppingProduct(
                  "Avena",
                  null,
                  null,
                  new BigDecimal("1.95"),
                  null,
                  linkedFoodItemId,
                  null,
                  null,
                  ShoppingCategory.CEREALES_Y_LEGUMBRES)));
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
