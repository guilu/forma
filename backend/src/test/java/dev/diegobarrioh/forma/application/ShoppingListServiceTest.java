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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ShoppingListService} (FOR-39, FOR-108, FOR-109): resolves product names +
 * budget, threads {@code unit}/{@code servings}/{@code generatedAt} (FOR-108) and {@code
 * productUrl} (FOR-109), toggles checked state, regenerates the list and edits an item's quantity
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
              assertThat(entry.productUrl()).isEqualTo("https://tienda.mercadona.es/p1");
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
              // Unresolved product -> no URL to link out to either, not a broken link.
              assertThat(entry.productUrl()).isNull();
            });
  }

  @Test
  void productWithNoUrlSurfacesNullProductUrl() {
    FakeProductRepository productsWithoutUrl = new FakeProductRepository(null, false);
    ShoppingListService serviceWithoutUrl =
        new ShoppingListService(
            lists, productsWithoutUrl, new ShoppingBudgetService(productsWithoutUrl));

    ShoppingListView view = serviceWithoutUrl.currentView();

    assertThat(view.items())
        .singleElement()
        .satisfies(entry -> assertThat(entry.productUrl()).isNull());
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

  @Test
  void regenerateRebuildsListFromProductCatalogAndResetsChecked() {
    ShoppingListView view = service.regenerate();

    assertThat(view.items())
        .singleElement()
        .satisfies(
            entry -> {
              assertThat(entry.productId()).isEqualTo("p1");
              assertThat(entry.quantity()).isEqualTo(1);
              // 1.95 * 1 = 1.95.
              assertThat(entry.estimatedCostEur()).isEqualByComparingTo("1.95");
              assertThat(entry.checked()).isFalse();
            });
    assertThat(lists.lastRegeneratedItems()).hasSize(1);
    assertThat(lists.lastRegeneratedAt()).isNotNull();
  }

  @Test
  void regenerateOnEmptyProductCatalogProducesValidEmptyList() {
    ShoppingProductRepository noProducts =
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
        };
    ShoppingListService serviceWithNoProducts =
        new ShoppingListService(lists, noProducts, new ShoppingBudgetService(noProducts));

    ShoppingListView view = serviceWithNoProducts.regenerate();

    assertThat(view.items()).isEmpty();
  }

  @Test
  void regenerateWithNoActiveListThrowsNotFound() {
    FakeListRepository listsWithNoActive = new FakeListRepository();
    listsWithNoActive.hasActiveList = false;
    ShoppingListService serviceWithNoActiveList =
        new ShoppingListService(listsWithNoActive, products, new ShoppingBudgetService(products));

    assertThatThrownBy(serviceWithNoActiveList::regenerate).isInstanceOf(NotFoundException.class);
  }

  @Test
  void quantityEditRecalculatesCostFromProductPrice() {
    StoredShoppingListItem updated = service.updateQuantity("i1", 5);

    // 1.95 * 5 = 9.75.
    assertThat(updated.item().quantity()).isEqualTo(5);
    assertThat(updated.item().estimatedCostEur()).isEqualByComparingTo("9.75");
  }

  @Test
  void quantityEditToSameValueIsIdempotent() {
    StoredShoppingListItem updated = service.updateQuantity("i1", 2);

    assertThat(updated.item().quantity()).isEqualTo(2);
    assertThat(updated.item().estimatedCostEur()).isEqualByComparingTo("3.90");
  }

  @Test
  void quantityEditOnUnknownItemThrowsNotFound() {
    assertThatThrownBy(() -> service.updateQuantity("nope", 3))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("nope");
  }

  @Test
  void quantityEditOnUnresolvableProductThrowsNotFoundInsteadOfFabricatingCost() {
    FakeListRepository listsWithOrphanItem = new FakeListRepository();
    listsWithOrphanItem.orphanProductId = true;
    ShoppingListService serviceWithOrphanItem =
        new ShoppingListService(listsWithOrphanItem, products, new ShoppingBudgetService(products));

    assertThatThrownBy(() -> serviceWithOrphanItem.updateQuantity("i1", 3))
        .isInstanceOf(NotFoundException.class);
  }

  private static final class FakeListRepository implements ShoppingListRepository {
    private boolean hasActiveList = true;
    private boolean orphanProductId = false;
    private List<ShoppingListItem> lastRegeneratedItems;
    private Instant lastRegeneratedAt;
    private Instant generatedAt = GENERATED_AT;

    // Mutable "persisted" items, keyed by id, so regenerate/updateQuantity/setChecked/findItem all
    // behave like a real store (subsequent findActive()/findItem() reflect prior writes), mirroring
    // JdbcShoppingListRepository's real persistence semantics.
    private final java.util.LinkedHashMap<String, ShoppingListItem> itemsById =
        new java.util.LinkedHashMap<>();

    FakeListRepository() {
      this(null);
    }

    FakeListRepository(Integer storedServings) {
      itemsById.put(
          "i1",
          new ShoppingListItem(
              "p1", 2, new BigDecimal("3.90"), false, ShoppingUnit.KG, storedServings));
    }

    List<ShoppingListItem> lastRegeneratedItems() {
      return lastRegeneratedItems;
    }

    Instant lastRegeneratedAt() {
      return lastRegeneratedAt;
    }

    @Override
    public Optional<ActiveShoppingList> findActive() {
      if (!hasActiveList) {
        return Optional.empty();
      }
      List<StoredShoppingListItem> items =
          itemsById.entrySet().stream()
              .map(e -> new StoredShoppingListItem(e.getKey(), e.getValue()))
              .toList();
      return Optional.of(
          new ActiveShoppingList(
              "list1",
              LocalDate.of(2026, 7, 6),
              ShoppingListStatus.ACTIVE,
              null,
              items,
              generatedAt));
    }

    @Override
    public Optional<StoredShoppingListItem> setChecked(String itemId, boolean checked) {
      ShoppingListItem item = itemsById.get(itemId);
      if (item == null) {
        return Optional.empty();
      }
      ShoppingListItem updated =
          new ShoppingListItem(
              item.productId(),
              item.quantity(),
              item.estimatedCostEur(),
              checked,
              item.unit(),
              item.servings());
      itemsById.put(itemId, updated);
      return Optional.of(new StoredShoppingListItem(itemId, updated));
    }

    @Override
    public Optional<ActiveShoppingList> regenerate(
        List<ShoppingListItem> items, Instant newGeneratedAt) {
      if (!hasActiveList) {
        return Optional.empty();
      }
      this.lastRegeneratedItems = new ArrayList<>(items);
      this.lastRegeneratedAt = newGeneratedAt;
      itemsById.clear();
      for (int i = 0; i < items.size(); i++) {
        itemsById.put("regen-" + i, items.get(i));
      }
      this.generatedAt = newGeneratedAt;
      return findActive();
    }

    @Override
    public Optional<StoredShoppingListItem> updateQuantity(
        String itemId, int quantity, BigDecimal estimatedCostEur) {
      ShoppingListItem item = itemsById.get(itemId);
      if (item == null) {
        return Optional.empty();
      }
      ShoppingListItem updated =
          new ShoppingListItem(
              item.productId(),
              quantity,
              estimatedCostEur,
              item.checked(),
              item.unit(),
              item.servings());
      itemsById.put(itemId, updated);
      return Optional.of(new StoredShoppingListItem(itemId, updated));
    }

    @Override
    public Optional<StoredShoppingListItem> findItem(String itemId) {
      ShoppingListItem item = itemsById.get(itemId);
      if (item == null) {
        return Optional.empty();
      }
      if (orphanProductId) {
        item =
            new ShoppingListItem(
                "unknown-product",
                item.quantity(),
                item.estimatedCostEur(),
                item.checked(),
                item.unit(),
                item.servings());
      }
      return Optional.of(new StoredShoppingListItem(itemId, item));
    }
  }

  private static final class FakeProductRepository implements ShoppingProductRepository {
    private final String linkedFoodItemId;
    private final boolean withUrl;

    FakeProductRepository() {
      this(null, true);
    }

    FakeProductRepository(String linkedFoodItemId) {
      this(linkedFoodItemId, true);
    }

    FakeProductRepository(String linkedFoodItemId, boolean withUrl) {
      this.linkedFoodItemId = linkedFoodItemId;
      this.withUrl = withUrl;
    }

    @Override
    public List<StoredShoppingProduct> findAll() {
      return List.of(
          new StoredShoppingProduct(
              "p1",
              new ShoppingProduct(
                  "Avena",
                  withUrl ? "https://tienda.mercadona.es/p1" : null,
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
