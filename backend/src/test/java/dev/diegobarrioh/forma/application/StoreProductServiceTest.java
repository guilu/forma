package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.ShoppingCategory;
import dev.diegobarrioh.forma.domain.Store;
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
  private final StoreProductService service =
      new StoreProductService(repository, java.util.List.of(mercadona));

  private static CatalogStoreProduct product(String id, Store store, String name) {
    return new CatalogStoreProduct(
        id,
        store,
        name,
        "oats",
        "500 g",
        new BigDecimal("1.55"),
        "https://example.test/producto",
        ShoppingCategory.CEREALES_Y_LEGUMBRES,
        null);
  }

  @Test
  void createsAProductAndReadsItBack() {
    CatalogStoreProduct created =
        service.create(product("mercadona-oats", Store.MERCADONA, "Copos de avena"));

    assertThat(created.id()).isEqualTo("mercadona-oats");
    assertThat(service.getById("mercadona-oats").name()).isEqualTo("Copos de avena");
  }

  @Test
  void refusesToCreateAProductWhoseIdIsTaken() {
    service.create(product("mercadona-oats", Store.MERCADONA, "Copos de avena"));

    assertThatThrownBy(
            () -> service.create(product("mercadona-oats", Store.MERCADONA, "Otra avena")))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void updatesAnExistingProduct() {
    service.create(product("mercadona-oats", Store.MERCADONA, "Copos de avena"));

    CatalogStoreProduct updated =
        service.update(
            "mercadona-oats", product("mercadona-oats", Store.MERCADONA, "Copos de avena Brüggen"));

    assertThat(updated.name()).isEqualTo("Copos de avena Brüggen");
    assertThat(service.getById("mercadona-oats").name()).isEqualTo("Copos de avena Brüggen");
  }

  /** The path id wins over the body id, so an edit can never rename a row out from under a link. */
  @Test
  void ignoresTheIdInTheBodyWhenUpdating() {
    service.create(product("mercadona-oats", Store.MERCADONA, "Copos de avena"));

    service.update("mercadona-oats", product("otro-id", Store.MERCADONA, "Copos de avena"));

    assertThat(service.findAll(null))
        .extracting(CatalogStoreProduct::id)
        .containsOnly("mercadona-oats");
  }

  @Test
  void refusesToUpdateAProductThatDoesNotExist() {
    assertThatThrownBy(() -> service.update("ghost", product("ghost", Store.MERCADONA, "Fantasma")))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void deletesAProduct() {
    service.create(product("mercadona-oats", Store.MERCADONA, "Copos de avena"));

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
    service.create(product("mercadona-oats", Store.MERCADONA, "Copos de avena"));
    service.create(product("carrefour-oats", Store.CARREFOUR, "Avena"));

    assertThat(service.findAll(Store.MERCADONA))
        .extracting(CatalogStoreProduct::id)
        .containsExactly("mercadona-oats");
    assertThat(service.findAll(null)).hasSize(2);
  }

  private static final class InMemoryRepository implements StoreProductRepository {
    private final Map<String, CatalogStoreProduct> rows = new LinkedHashMap<>();

    @Override
    public List<CatalogStoreProduct> findAll(Store store) {
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
            Store.MERCADONA,
            "Copos de avena Brüggen",
            "oats",
            "Caja 500 g",
            new BigDecimal("1.55"),
            "https://tienda.mercadona.es/product/4241",
            ShoppingCategory.CEREALES_Y_LEGUMBRES,
            "Comprar dos si hay oferta",
            "4241",
            "https://cdn/old.jpg");
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
    service.create(product("propio", Store.MERCADONA, "Pan de mi panadería"));

    assertThatThrownBy(() -> service.refresh("propio")).isInstanceOf(ConflictException.class);
  }

  /** The shop dropping a product is an answer, not a failure — and it must not blank our row. */
  @Test
  void reportsAProductTheShopNoLongerLists() {
    service.create(
        new CatalogStoreProduct(
            "mercadona-9",
            Store.MERCADONA,
            "Descatalogado",
            null,
            null,
            new BigDecimal("1.00"),
            null,
            ShoppingCategory.OTROS,
            null,
            "9",
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
    public Store store() {
      return Store.MERCADONA;
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
}
