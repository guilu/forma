package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.ShoppingCategory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Store catalog maintenance use cases (FOR-191), the writes behind the admin screen's shopping tab.
 * Hand-rolled in-memory repository, no Spring (ADR-007).
 */
class StoreProductServiceTest {

  private final InMemoryRepository repository = new InMemoryRepository();
  private final FakeSource mercadona = new FakeSource();
  private final InMemoryStores stores = new InMemoryStores();
  private final InMemoryStoreCategories aisles = new InMemoryStoreCategories();
  private final StoreProductService service =
      new StoreProductService(repository, java.util.List.of(mercadona), stores, aisles);

  private static CatalogStoreProduct product(String id, String store, String name) {
    return new CatalogStoreProduct(
        id,
        store,
        name,
        "oats",
        "500 g",
        new BigDecimal("1.55"),
        "https://example.test/producto",
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

  @Test
  void createsAProductAndReadsItBack() {
    CatalogStoreProduct created =
        service.create(product("mercadona-oats", "MERCADONA", "Copos de avena"));

    assertThat(created.id()).isEqualTo("mercadona-oats");
    assertThat(service.getById("mercadona-oats").name()).isEqualTo("Copos de avena");
  }

  @Test
  void refusesToCreateAProductWhoseIdIsTaken() {
    service.create(product("mercadona-oats", "MERCADONA", "Copos de avena"));

    assertThatThrownBy(() -> service.create(product("mercadona-oats", "MERCADONA", "Otra avena")))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void updatesAnExistingProduct() {
    service.create(product("mercadona-oats", "MERCADONA", "Copos de avena"));

    CatalogStoreProduct updated =
        service.update(
            "mercadona-oats", product("mercadona-oats", "MERCADONA", "Copos de avena Brüggen"));

    assertThat(updated.name()).isEqualTo("Copos de avena Brüggen");
    assertThat(service.getById("mercadona-oats").name()).isEqualTo("Copos de avena Brüggen");
  }

  /** The path id wins over the body id, so an edit can never rename a row out from under a link. */
  @Test
  void ignoresTheIdInTheBodyWhenUpdating() {
    service.create(product("mercadona-oats", "MERCADONA", "Copos de avena"));

    service.update("mercadona-oats", product("otro-id", "MERCADONA", "Copos de avena"));

    assertThat(service.findAll(null))
        .extracting(CatalogStoreProduct::id)
        .containsOnly("mercadona-oats");
  }

  /**
   * An edit changes what the admin typed, never where the row came from. {@code externalId} is
   * provenance rather than a field on the form: it is what the refresh action is offered on, so
   * losing it on a save would quietly turn an imported product into a hand-typed one.
   */
  @Test
  void keepsTheShopReferenceWhenUpdating() {
    service.create(
        new CatalogStoreProduct(
            "mercadona-oats",
            "MERCADONA",
            "Copos de avena",
            "oats",
            "Caja 500 g",
            new BigDecimal("1.55"),
            "https://tienda.mercadona.es/product/4241",
            ShoppingCategory.CEREALES_Y_LEGUMBRES,
            null,
            "4241",
            "https://cdn/foto.jpg",
            null,
            null,
            null,
            null,
            true,
            null,
            null));

    service.update(
        "mercadona-oats", product("mercadona-oats", "MERCADONA", "Copos de avena Brüggen"));

    CatalogStoreProduct stored = service.getById("mercadona-oats");
    assertThat(stored.name()).isEqualTo("Copos de avena Brüggen");
    assertThat(stored.externalId()).isEqualTo("4241");
  }

  /**
   * The image is on the form, so an edit that sets one keeps it — and one that clears it clears.
   */
  @Test
  void storesTheImageAnUpdateCarries() {
    service.create(product("propio", "OTRAS", "Whey proteína"));

    service.update(
        "propio",
        new CatalogStoreProduct(
            "propio",
            "OTRAS",
            "Whey proteína",
            "oats",
            "1 kg",
            new BigDecimal("22.00"),
            "https://www.amazon.es/gp/aw/d/B07Q31N9D4",
            ShoppingCategory.OTROS,
            null,
            null,
            "https://m.media-amazon.com/images/I/31kt192oAzL.jpg",
            null,
            null,
            null,
            null,
            true,
            null,
            null));

    assertThat(service.getById("propio").imageUrl())
        .isEqualTo("https://m.media-amazon.com/images/I/31kt192oAzL.jpg");
  }

  @Test
  void refusesToUpdateAProductThatDoesNotExist() {
    assertThatThrownBy(() -> service.update("ghost", product("ghost", "MERCADONA", "Fantasma")))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void deletesAProduct() {
    service.create(product("mercadona-oats", "MERCADONA", "Copos de avena"));

    service.delete("mercadona-oats");

    assertThat(service.findAll(null)).isEmpty();
  }

  @Test
  void refusesToDeleteAProductThatDoesNotExist() {
    assertThatThrownBy(() -> service.delete("ghost")).isInstanceOf(NotFoundException.class);
  }

  /**
   * One catalog, many chains (V36): the screen asks for a single store at a time, so the filter
   * belongs to the query rather than to a table per chain.
   */
  @Test
  void listsOnlyTheProductsOfTheRequestedStore() {
    service.create(product("mercadona-oats", "MERCADONA", "Copos de avena"));
    service.create(product("carrefour-oats", "CARREFOUR", "Avena"));

    assertThat(service.findAll("MERCADONA"))
        .extracting(CatalogStoreProduct::id)
        .containsExactly("mercadona-oats");
    assertThat(service.findAll(null)).hasSize(2);
  }

  private static final class InMemoryRepository implements StoreProductRepository {
    private final Map<String, CatalogStoreProduct> rows = new LinkedHashMap<>();

    @Override
    public List<CatalogStoreProduct> findAll(String store) {
      List<CatalogStoreProduct> found = new ArrayList<>();
      for (CatalogStoreProduct row : rows.values()) {
        if (store == null || row.store() == store) {
          found.add(row);
        }
      }
      return found;
    }

    @Override
    public Optional<CatalogStoreProduct> findById(String id) {
      return Optional.ofNullable(rows.get(id));
    }

    @Override
    public void insert(CatalogStoreProduct product) {
      rows.put(product.id(), product);
    }

    @Override
    public void update(CatalogStoreProduct product) {
      rows.put(product.id(), product);
    }

    @Override
    public boolean delete(String id) {
      return rows.remove(id) != null;
    }
  }

  /**
   * A refresh takes what the shop owns and leaves what we curate (FOR-195). The price and the shelf
   * name move; the food link, the aisle and our notes do not — a refresh that overwrote those would
   * undo an admin's work every time a price changed.
   */
  @Test
  void refreshTakesTheShopsFieldsAndKeepsOurs() {
    CatalogStoreProduct stored =
        new CatalogStoreProduct(
            "mercadona-4241",
            "MERCADONA",
            "Copos de avena Brüggen",
            "oats",
            "Caja 500 g",
            new BigDecimal("1.55"),
            "https://tienda.mercadona.es/product/4241",
            ShoppingCategory.CEREALES_Y_LEGUMBRES,
            "Comprar dos si hay oferta",
            "4241",
            "https://cdn/old.jpg",
            null,
            null,
            null,
            null,
            true,
            null,
            null);
    service.create(stored);
    mercadona.serve(
        new ImportableProduct(
            "4241",
            "Copos de avena Brüggen 500 g",
            "Caja 500 g",
            new BigDecimal("1.79"),
            "https://tienda.mercadona.es/product/4241",
            "8480000123456",
            "Cereales",
            null,
            null,
            null,
            true,
            "https://cdn/new.jpg"));

    CatalogStoreProduct refreshed = service.refresh("mercadona-4241");

    assertThat(refreshed.name()).isEqualTo("Copos de avena Brüggen 500 g");
    assertThat(refreshed.priceEur()).isEqualByComparingTo("1.79");
    assertThat(refreshed.imageUrl()).isEqualTo("https://cdn/new.jpg");
    // Ours, untouched.
    assertThat(refreshed.foodId()).isEqualTo("oats");
    assertThat(refreshed.category()).isEqualTo(ShoppingCategory.CEREALES_Y_LEGUMBRES);
    assertThat(refreshed.notes()).isEqualTo("Comprar dos si hay oferta");
    assertThat(service.getById("mercadona-4241").priceEur()).isEqualByComparingTo("1.79");
  }

  /** A product typed by hand has no shop behind it, so there is nothing to refresh it against. */
  @Test
  void refuseToRefreshAProductThatWasNeverImported() {
    service.create(product("propio", "MERCADONA", "Pan de mi panadería"));

    assertThatThrownBy(() -> service.refresh("propio")).isInstanceOf(ConflictException.class);
  }

  /** The shop dropping a product is an answer, not a failure — and it must not blank our row. */
  @Test
  void reportsAProductTheShopNoLongerLists() {
    service.create(
        new CatalogStoreProduct(
            "mercadona-9",
            "MERCADONA",
            "Descatalogado",
            null,
            null,
            new BigDecimal("1.00"),
            null,
            ShoppingCategory.OTROS,
            null,
            "9",
            null,
            null,
            null,
            null,
            null,
            true,
            null,
            null));
    mercadona.serveNothing();

    assertThatThrownBy(() -> service.refresh("mercadona-9")).isInstanceOf(NotFoundException.class);
    assertThat(service.getById("mercadona-9").name()).isEqualTo("Descatalogado");
  }

  private static final class FakeSource implements StoreCatalogSource {
    private ImportableProduct fresh;

    void serve(ImportableProduct product) {
      fresh = product;
    }

    void serveNothing() {
      fresh = null;
    }

    @Override
    public String store() {
      return "MERCADONA";
    }

    @Override
    public java.util.List<ImportableProduct> products() {
      return fresh == null ? java.util.List.of() : java.util.List.of(fresh);
    }

    @Override
    public Optional<ImportableProduct> findByExternalId(String externalId) {
      return Optional.ofNullable(fresh);
    }
  }

  /**
   * The chains are rows since V45, so an id that names none has to be refused here. As a filter it
   * would otherwise answer "this chain sells nothing", and as a write it would reach the foreign
   * key and surface as a server error — both blame the wrong thing.
   */
  @Test
  void refusesAChainThatIsNotOneOfOurs() {
    assertThatThrownBy(() -> service.findAll("LIDL"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("LIDL");
    assertThatThrownBy(() -> service.create(product("x", "LIDL", "Avena Lidl")))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("LIDL");
  }

  /** No filter at all is not an unknown chain: it means every chain. */
  @Test
  void listsEveryChainWhenNoneIsAskedFor() {
    repository.insert(product("a", "MERCADONA", "Avena"));
    repository.insert(product("b", "CARREFOUR", "Arroz"));

    assertThat(service.findAll(null)).hasSize(2);
  }

  private static final class InMemoryStores implements StoreRepository {
    private final java.util.Map<String, Store> rows =
        new java.util.LinkedHashMap<>(
            java.util.Map.of(
                "MERCADONA", new Store("MERCADONA", "Mercadona", null, null, 1, true),
                "CARREFOUR", new Store("CARREFOUR", "Carrefour", null, null, 2, true),
                "OTRAS", new Store("OTRAS", "Otras", null, null, 99, true)));

    @Override
    public java.util.List<Store> findAll() {
      return java.util.List.copyOf(rows.values());
    }

    @Override
    public java.util.Optional<Store> find(String id) {
      return java.util.Optional.ofNullable(rows.get(id));
    }
  }

  // --- V46: which shelf of its own shop a product came off ---

  /** The shop's id for the aisle becomes one of our rows, when we have that row. */
  @Test
  void filesAnImportedProductOnTheShelfTheShopSaysItCameFrom() {
    aisles.seed("MERCADONA:112", "MERCADONA");

    CatalogStoreProduct created = service.create(product("p", "MERCADONA", "Aceite"), "112");

    assertThat(created.storeCategoryId()).isEqualTo("MERCADONA:112");
  }

  /**
   * Importing from a shop does not require having synced its aisles first, so an id that names no
   * stored aisle is dropped rather than refused. The alternative is a foreign key violation on a
   * request that is otherwise perfectly good.
   */
  @Test
  void leavesTheShelfUnsetWhenThatAisleHasNotBeenSynced() {
    CatalogStoreProduct created = service.create(product("p", "MERCADONA", "Aceite"), "999");

    assertThat(created.storeCategoryId()).isNull();
  }

  /**
   * An aisle belonging to another shop is not this product's shelf, however well the id matches.
   */
  @Test
  void refusesToBorrowAnotherShopsAisle() {
    aisles.seed("CARREFOUR:112", "CARREFOUR");

    CatalogStoreProduct created = service.create(product("p", "MERCADONA", "Aceite"), "112");

    assertThat(created.storeCategoryId()).isNull();
  }

  /** A product typed by hand never came off a shelf. */
  @Test
  void leavesTheShelfUnsetForAProductNobodyImported() {
    assertThat(service.create(product("p", "MERCADONA", "Aceite"), null).storeCategoryId())
        .isNull();
  }

  /**
   * The shelf is the shop's to say, like the name and the price, so an edit leaves it where it was
   * — the admin form has no field for it and never did.
   */
  @Test
  void anEditDoesNotMoveAProductToADifferentShelf() {
    aisles.seed("MERCADONA:112", "MERCADONA");
    service.create(product("p", "MERCADONA", "Aceite"), "112");

    CatalogStoreProduct edited = service.update("p", product("p", "MERCADONA", "Aceite de oliva"));

    assertThat(edited.storeCategoryId()).isEqualTo("MERCADONA:112");
  }

  private static final class InMemoryStoreCategories implements StoreCategoryRepository {
    private final Map<String, StoreCategory> rows = new LinkedHashMap<>();

    void seed(String id, String storeId) {
      rows.put(id, new StoreCategory(id, storeId, null, id, "X", "x", 0, 0, true));
    }

    @Override
    public List<StoreCategory> findByStore(String storeId, boolean includeRetired) {
      return rows.values().stream().filter(row -> row.storeId().equals(storeId)).toList();
    }

    @Override
    public Optional<StoreCategory> find(String id) {
      return Optional.ofNullable(rows.get(id));
    }

    @Override
    public void save(StoreCategory category) {
      rows.put(category.id(), category);
    }

    @Override
    public void retire(String id) {
      throw new UnsupportedOperationException();
    }
  }

  // --- V48: what the shop says beyond the name and the price ---

  /**
   * A refresh takes everything the shop owns, including the three facts that used to arrive and be
   * dropped, and stamps when it asked.
   */
  @Test
  void aRefreshTakesTheBarcodeTheSizeAndWhetherItIsStillSold() {
    service.create(
        product("mercadona-oil", "MERCADONA", "Aceite")
            .onShelf(null)
            .refreshedWith(imported("4241", "Aceite", null)));
    mercadona.serve(imported("4241", "Aceite de oliva Hacendado", "8480000123456"));

    CatalogStoreProduct refreshed = service.refresh("mercadona-oil");

    assertThat(refreshed.ean()).isEqualTo("8480000123456");
    assertThat(refreshed.packageAmount()).isEqualByComparingTo("5.0");
    assertThat(refreshed.packageUnit()).isEqualTo("l");
    assertThat(refreshed.available()).isTrue();
    assertThat(refreshed.lastSyncedAt()).isNotNull();
  }

  /**
   * The brand is the one detail here no shop publishes separately, so a refresh must not blank what
   * somebody typed.
   */
  @Test
  void aRefreshLeavesTheBrandSomebodyTyped() {
    CatalogStoreProduct branded =
        service.create(
            new CatalogStoreProduct(
                "mercadona-oil",
                "MERCADONA",
                "Aceite",
                null,
                null,
                null,
                null,
                ShoppingCategory.GRASAS_Y_ACEITES,
                null,
                "4241",
                null,
                null,
                null,
                null,
                null,
                true,
                null,
                "Hacendado"));
    assertThat(branded.brand()).isEqualTo("Hacendado");
    mercadona.serve(imported("4241", "Aceite de oliva", "8480000123456"));

    assertThat(service.refresh("mercadona-oil").brand()).isEqualTo("Hacendado");
  }

  /**
   * A shop that stopped selling something says so, and the row records it rather than pretending.
   */
  @Test
  void recordsThatTheShopStoppedSellingIt() {
    service.create(
        new CatalogStoreProduct(
            "mercadona-oil",
            "MERCADONA",
            "Aceite",
            null,
            null,
            null,
            null,
            ShoppingCategory.GRASAS_Y_ACEITES,
            null,
            "4241",
            null,
            null,
            null,
            null,
            null,
            true,
            null,
            null));
    mercadona.serve(
        new ImportableProduct(
            "4241", "Aceite", null, null, null, null, null, null, null, null, false, null));

    assertThat(service.refresh("mercadona-oil").available()).isFalse();
  }

  private static ImportableProduct imported(String externalId, String name, String ean) {
    return new ImportableProduct(
        externalId,
        name,
        "Garrafa 5 l",
        new BigDecimal("17.75"),
        "https://tienda.mercadona.es/product/" + externalId,
        ean,
        null,
        null,
        new BigDecimal("5.0"),
        "l",
        true,
        null);
  }
}
