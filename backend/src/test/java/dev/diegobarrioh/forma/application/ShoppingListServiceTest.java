package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.MacroTargets;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import dev.diegobarrioh.forma.domain.NutritionTotals;
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
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ShoppingListService} (FOR-39, FOR-108, FOR-109): resolves product names +
 * budget, threads {@code unit}/{@code servings}/{@code generatedAt} (FOR-108) and {@code
 * productUrl} (FOR-109), toggles checked state, regenerates the list and edits an item's quantity
 * (no Spring context — ADR-007).
 */
class ShoppingListServiceTest {

  private static final Instant GENERATED_AT = Instant.parse("2026-07-06T08:00:00Z");
  private static final UUID USER_ID = UUID.randomUUID();

  /**
   * The shared catalog a user's entries are created from. Two products is enough: the point is that
   * regenerate asks for every one of them, not how many there are.
   */
  private static final StoreProductRepository CATALOG =
      new StoreProductRepository() {
        @Override
        public List<CatalogStoreProduct> findAll(String store) {
          return List.of(catalogProduct("mercadona-oats"), catalogProduct("mercadona-rice"));
        }

        @Override
        public Optional<CatalogStoreProduct> findById(String id) {
          return Optional.empty();
        }

        @Override
        public void insert(CatalogStoreProduct product) {
          throw new UnsupportedOperationException();
        }

        @Override
        public void update(CatalogStoreProduct product) {
          throw new UnsupportedOperationException();
        }

        @Override
        public boolean delete(String id) {
          throw new UnsupportedOperationException();
        }
      };

  private static CatalogStoreProduct catalogProduct(String id) {
    return new CatalogStoreProduct(
        id,
        "MERCADONA",
        "Producto " + id,
        null,
        "1 kg",
        new BigDecimal("1.95"),
        null,
        ShoppingCategory.CEREALES_Y_LEGUMBRES,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        true,
        null,
        null);
  }

  private final FakeProductRepository products = new FakeProductRepository();
  private final FakeListRepository lists = new FakeListRepository();
  private final ShoppingListService service =
      new ShoppingListService(
          lists,
          products,
          new ShoppingBudgetService(products, () -> USER_ID),
          () -> USER_ID,
          CATALOG,
          userId -> List.of());

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
            listsWithServings,
            linkedProducts,
            new ShoppingBudgetService(linkedProducts, () -> USER_ID),
            () -> USER_ID,
            CATALOG,
            userId -> List.of());

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
            listsWithRawServings,
            products,
            new ShoppingBudgetService(products, () -> USER_ID),
            () -> USER_ID,
            CATALOG,
            userId -> List.of());

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
              public List<StoredShoppingProduct> findAllByOwner(UUID userId) {
                return List.of();
              }

              @Override
              public int addMissingCatalogReferences(UUID userId, List<String> storeProductIds) {
                return 0;
              }

              @Override
              public StoredShoppingProduct create(UUID userId, ShoppingProduct product) {
                throw new UnsupportedOperationException();
              }

              @Override
              public Optional<StoredShoppingProduct> update(
                  UUID userId, String id, ShoppingProduct product) {
                throw new UnsupportedOperationException();
              }
            },
            new ShoppingBudgetService(products, () -> USER_ID),
            () -> USER_ID,
            CATALOG,
            userId -> List.of());

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
              // No product to derive a live cost from -> fall back to the last-known stored
              // snapshot instead of crashing or fabricating a cost from nothing.
              assertThat(entry.estimatedCostEur()).isEqualByComparingTo("3.90");
            });
  }

  @Test
  void productWithNoUrlSurfacesNullProductUrl() {
    FakeProductRepository productsWithoutUrl = new FakeProductRepository(null, false);
    ShoppingListService serviceWithoutUrl =
        new ShoppingListService(
            lists,
            productsWithoutUrl,
            new ShoppingBudgetService(productsWithoutUrl, () -> USER_ID),
            () -> USER_ID,
            CATALOG,
            userId -> List.of());

    ShoppingListView view = serviceWithoutUrl.currentView();

    assertThat(view.items())
        .singleElement()
        .satisfies(entry -> assertThat(entry.productUrl()).isNull());
  }

  @Test
  void listLineCostReflectsCurrentProductPriceNotStaleStoredSnapshot() {
    // Stored snapshot cost (2.00) is stale — e.g. left over from before the product's price was
    // edited. The product's current price (fixture: 1.95) x quantity (2) = 3.90 must win, matching
    // what the budget total already derives (ShoppingBudgetCalculator) and what updateQuantity()
    // already recomputes. Regression test for the edit-price-stale-line bug.
    FakeListRepository listsWithStaleCost = new FakeListRepository(2, new BigDecimal("2.00"));
    ShoppingListService serviceWithStaleCost =
        new ShoppingListService(
            listsWithStaleCost,
            products,
            new ShoppingBudgetService(products, () -> USER_ID),
            () -> USER_ID,
            CATALOG,
            userId -> List.of());

    ShoppingListView view = serviceWithStaleCost.currentView();

    assertThat(view.items())
        .singleElement()
        .satisfies(entry -> assertThat(entry.estimatedCostEur()).isEqualByComparingTo("3.90"));
  }

  @Test
  void listLineCostScalesWithQuantityGreaterThanOne() {
    // Stored cost (100.00) is intentionally wrong/irrelevant here — quantity 3 x current product
    // price 1.95 = 5.85 must be the live-derived value.
    FakeListRepository listsWithQuantityThree = new FakeListRepository(3, new BigDecimal("100.00"));
    ShoppingListService serviceWithQuantityThree =
        new ShoppingListService(
            listsWithQuantityThree,
            products,
            new ShoppingBudgetService(products, () -> USER_ID),
            () -> USER_ID,
            CATALOG,
            userId -> List.of());

    ShoppingListView view = serviceWithQuantityThree.currentView();

    assertThat(view.items())
        .singleElement()
        .satisfies(entry -> assertThat(entry.estimatedCostEur()).isEqualByComparingTo("5.85"));
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

  /**
   * A user's list is built from the shared catalog (FOR-192): regenerate first gives the account an
   * entry for every catalog product it does not have yet, then rebuilds the list from its entries.
   * Without that step a new account regenerates into an empty list forever, because it owns
   * nothing.
   */
  @Test
  void regenerateGivesTheAccountAnEntryForEveryCatalogProduct() {
    service.regenerate();

    assertThat(products.addedReferences())
        .containsExactlyInAnyOrder("mercadona-oats", "mercadona-rice");
  }

  @Test
  void regenerateOnEmptyProductCatalogProducesValidEmptyList() {
    ShoppingProductRepository noProducts =
        new ShoppingProductRepository() {
          @Override
          public List<StoredShoppingProduct> findAllByOwner(UUID userId) {
            return List.of();
          }

          @Override
          public int addMissingCatalogReferences(UUID userId, List<String> storeProductIds) {
            return 0;
          }

          @Override
          public StoredShoppingProduct create(UUID userId, ShoppingProduct product) {
            throw new UnsupportedOperationException();
          }

          @Override
          public Optional<StoredShoppingProduct> update(
              UUID userId, String id, ShoppingProduct product) {
            throw new UnsupportedOperationException();
          }
        };
    ShoppingListService serviceWithNoProducts =
        new ShoppingListService(
            lists,
            noProducts,
            new ShoppingBudgetService(noProducts, () -> USER_ID),
            () -> USER_ID,
            CATALOG,
            userId -> List.of());

    ShoppingListView view = serviceWithNoProducts.regenerate();

    assertThat(view.items()).isEmpty();
  }

  @Test
  void regenerateCreatesTheFirstListWhenTheAccountHasNone() {
    FakeListRepository listsWithNoActive = new FakeListRepository();
    listsWithNoActive.hasActiveList = false;
    ShoppingListService serviceWithNoActiveList =
        new ShoppingListService(
            listsWithNoActive,
            products,
            new ShoppingBudgetService(products, () -> USER_ID),
            () -> USER_ID,
            CATALOG,
            userId -> List.of());

    assertThat(serviceWithNoActiveList.regenerate().items()).isNotEmpty();
  }

  /**
   * Una cuenta sin lista no es un fallo, y decirlo importa: el 404 hacía que la pantalla enseñara
   * «no se pudo cargar», que acusa de una avería donde solo falta pulsar un botón.
   */
  @Test
  void currentViewOfAnAccountWithNoListIsAnEmptyWeek() {
    FakeListRepository listsWithNoActive = new FakeListRepository();
    listsWithNoActive.hasActiveList = false;
    ShoppingListService serviceWithNoActiveList =
        new ShoppingListService(
            listsWithNoActive,
            products,
            new ShoppingBudgetService(products, () -> USER_ID),
            () -> USER_ID,
            CATALOG,
            userId -> List.of());

    ShoppingListView view = serviceWithNoActiveList.currentView();

    assertThat(view.items()).isEmpty();
    assertThat(view.weekStartDate().getDayOfWeek()).isEqualTo(java.time.DayOfWeek.MONDAY);
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
        new ShoppingListService(
            listsWithOrphanItem,
            products,
            new ShoppingBudgetService(products, () -> USER_ID),
            () -> USER_ID,
            CATALOG,
            userId -> List.of());

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

    // Lets tests seed a stored snapshot cost that intentionally diverges from the fixture
    // product's current price x quantity, to prove the live view no longer trusts it.
    FakeListRepository(int quantity, BigDecimal storedCost) {
      itemsById.put(
          "i1", new ShoppingListItem("p1", quantity, storedCost, false, ShoppingUnit.KG, null));
    }

    List<ShoppingListItem> lastRegeneratedItems() {
      return lastRegeneratedItems;
    }

    Instant lastRegeneratedAt() {
      return lastRegeneratedAt;
    }

    @Override
    public Optional<ActiveShoppingList> findActive(UUID userId) {
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
    public Optional<StoredShoppingListItem> setChecked(
        UUID userId, String itemId, boolean checked) {
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
        UUID userId, List<ShoppingListItem> items, Instant newGeneratedAt) {
      // Regenerar CREA la lista cuando no hay ninguna, igual que el adaptador JDBC: era la única
      // puerta a la primera lista y estaba cerrada por dentro.
      this.hasActiveList = true;
      this.lastRegeneratedItems = new ArrayList<>(items);
      this.lastRegeneratedAt = newGeneratedAt;
      itemsById.clear();
      for (int i = 0; i < items.size(); i++) {
        itemsById.put("regen-" + i, items.get(i));
      }
      this.generatedAt = newGeneratedAt;
      return findActive(userId);
    }

    @Override
    public Optional<StoredShoppingListItem> updateQuantity(
        UUID userId, String itemId, int quantity, BigDecimal estimatedCostEur) {
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
    public Optional<StoredShoppingListItem> findItem(UUID userId, String itemId) {
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
    private final List<String> addedReferences = new ArrayList<>();
    private final String linkedFoodItemId;
    private final boolean withUrl;
    private final BigDecimal price;
    private final String storeProductId;

    FakeProductRepository() {
      this(null, true, new BigDecimal("1.95"), null);
    }

    FakeProductRepository(String linkedFoodItemId) {
      this(linkedFoodItemId, true, new BigDecimal("1.95"), null);
    }

    FakeProductRepository(String linkedFoodItemId, boolean withUrl) {
      this(linkedFoodItemId, withUrl, new BigDecimal("1.95"), null);
    }

    FakeProductRepository(
        String linkedFoodItemId, boolean withUrl, BigDecimal price, String storeProductId) {
      this.linkedFoodItemId = linkedFoodItemId;
      this.withUrl = withUrl;
      this.price = price;
      this.storeProductId = storeProductId;
    }

    @Override
    public List<StoredShoppingProduct> findAllByOwner(UUID userId) {
      return List.of(
          new StoredShoppingProduct(
              "p1",
              new ShoppingProduct(
                  "Avena",
                  withUrl ? "https://tienda.mercadona.es/p1" : null,
                  null,
                  price,
                  null,
                  linkedFoodItemId,
                  null,
                  null,
                  ShoppingCategory.CEREALES_Y_LEGUMBRES,
                  storeProductId)));
    }

    @Override
    public StoredShoppingProduct create(UUID userId, ShoppingProduct product) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<StoredShoppingProduct> update(UUID userId, String id, ShoppingProduct product) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int addMissingCatalogReferences(UUID userId, List<String> storeProductIds) {
      addedReferences.addAll(storeProductIds);
      return storeProductIds.size();
    }

    List<String> addedReferences() {
      return addedReferences;
    }
  }

  private record SingleProductCatalog(CatalogStoreProduct product)
      implements StoreProductRepository {
    @Override
    public List<CatalogStoreProduct> findAll(String store) {
      return List.of(product);
    }

    @Override
    public Optional<CatalogStoreProduct> findById(String id) {
      return product.id().equals(id) ? Optional.of(product) : Optional.empty();
    }

    @Override
    public void insert(CatalogStoreProduct ignored) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void update(CatalogStoreProduct ignored) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean delete(String id) {
      throw new UnsupportedOperationException();
    }
  }

  /** Un producto del catálogo que cubre un alimento, con el envase que declara la tienda. */
  private static CatalogStoreProduct catalogProductFor(
      String id, String foodId, String packageAmount, String packageUnit, String price) {
    return new CatalogStoreProduct(
        id,
        "MERCADONA",
        "Producto " + id,
        foodId,
        packageAmount + " " + packageUnit,
        price == null ? null : new BigDecimal(price),
        null,
        ShoppingCategory.CEREALES_Y_LEGUMBRES,
        null,
        null,
        null,
        null,
        null,
        packageAmount == null ? null : new BigDecimal(packageAmount),
        packageUnit,
        true,
        null,
        null);
  }

  private static ResolvedDay dayEating(String foodId, double grams) {
    ResolvedMeal meal =
        new ResolvedMeal(
            UUID.randomUUID(),
            MealType.BREAKFAST,
            "Desayuno",
            null,
            false,
            null,
            MacroTargets.none(),
            new NutritionTotals(0, 0, 0, 0),
            List.of(
                new ResolvedItem(
                    foodId,
                    "Alimento",
                    grams,
                    new NutritionTotals(0, 0, 0, 0),
                    false,
                    null,
                    null)));
    return new ResolvedDay(
        NutritionDayType.RUNNING,
        1,
        1,
        null,
        null,
        MacroTargets.none(),
        new NutritionTotals(0, 0, 0, 0),
        null,
        List.of(meal));
  }

  private ShoppingListService serviceFollowing(
      List<ResolvedDay> planDays, List<CatalogStoreProduct> catalog) {
    StoreProductRepository catalogRepo =
        new StoreProductRepository() {
          @Override
          public List<CatalogStoreProduct> findAll(String store) {
            return catalog;
          }

          @Override
          public Optional<CatalogStoreProduct> findById(String id) {
            return Optional.empty();
          }

          @Override
          public void insert(CatalogStoreProduct product) {
            throw new UnsupportedOperationException("not used by this test");
          }

          @Override
          public void update(CatalogStoreProduct product) {
            throw new UnsupportedOperationException("not used by this test");
          }

          @Override
          public boolean delete(String id) {
            throw new UnsupportedOperationException("not used by this test");
          }
        };
    return new ShoppingListService(
        lists,
        products,
        new ShoppingBudgetService(products, () -> USER_ID),
        () -> USER_ID,
        catalogRepo,
        userId -> planDays);
  }

  /**
   * La semana se suma entera y se pide en envases enteros.
   *
   * <p>80 g de avena en siete días son 560, y en bolsas de 500 g eso son dos bolsas: quedarse en
   * una dejaría la semana corta el último día.
   */
  @Test
  void asksForThePackagesThatCoverTheWeek() {
    List<ResolvedDay> week =
        List.of(
            dayEating("oats", 80),
            dayEating("oats", 80),
            dayEating("oats", 80),
            dayEating("oats", 80),
            dayEating("oats", 80),
            dayEating("oats", 80),
            dayEating("oats", 80));
    ShoppingListService service =
        serviceFollowing(
            week, List.of(catalogProductFor("mercadona-oats", "oats", "500", "G", "1.55")));

    ShoppingListView view = service.regenerate();

    assertThat(view.items())
        .singleElement()
        .satisfies(item -> assertThat(item.quantity()).isEqualTo(2));
  }

  /**
   * Lo que el plan pide y la tienda no tiene catalogado entra igualmente, y entra SIN precio.
   *
   * <p>Cero diría que sale gratis y arrastraría el presupuesto hacia abajo; nulo dice que nadie lo
   * ha dicho, que es la verdad y además es la señal de qué falta por catalogar.
   */
  @Test
  void listsWhatIsMissingFromTheCatalogWithoutAPrice() {
    ShoppingListService service = serviceFollowing(List.of(dayEating("quinoa", 300)), List.of());

    ShoppingListView view = service.regenerate();

    assertThat(view.items())
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.productId()).isEqualTo("quinoa");
              assertThat(item.estimatedCostEur()).isNull();
            });
  }

  @Test
  void keepsACataloguedProductWithUnknownPriceReadableAndBudgetable() {
    FakeProductRepository unpricedProducts =
        new FakeProductRepository("oats", true, null, "mercadona-oats");
    ShoppingListService service =
        new ShoppingListService(
            lists,
            unpricedProducts,
            new ShoppingBudgetService(unpricedProducts, () -> USER_ID),
            () -> USER_ID,
            new SingleProductCatalog(catalogProductFor("mercadona-oats", "oats", "500", "G", null)),
            userId -> List.of(dayEating("oats", 400)));

    ShoppingListView view = service.regenerate();

    assertThat(view.items())
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.catalogued()).isTrue();
              assertThat(item.estimatedCostEur()).isNull();
            });
    assertThat(view.budget().weeklyEur()).isEqualByComparingTo("0.00");
  }

  /** Sin plan activo la lista sigue saliendo del catálogo, como antes de todo esto. */
  @Test
  void fallsBackToTheCatalogWhenNoPlanIsBeingFollowed() {
    ShoppingListService service = serviceFollowing(List.of(), List.of(catalogProduct("x")));

    assertThat(service.regenerate().items()).isNotEmpty();
  }
}
