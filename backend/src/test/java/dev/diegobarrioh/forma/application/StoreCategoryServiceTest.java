package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Keeping a shop's aisles in step with the shop (V46). Hand-rolled fakes, no Spring (ADR-007).
 *
 * <p>The interesting behaviour is what a re-crawl does to aisles that were there last time.
 * Deleting them is not available — products point at them through a foreign key — and neither is
 * leaving them as if the shop still sold them. They are retired instead, which is what {@code
 * enabled} is for.
 */
class StoreCategoryServiceTest {

  private final InMemoryCategories repository = new InMemoryCategories();
  private final FakeSource mercadona = new FakeSource("MERCADONA");
  private final InMemoryStores stores = new InMemoryStores();
  private final StoreCategoryService service =
      new StoreCategoryService(repository, List.of(mercadona), stores);

  private static StoreCategoryNode node(String id, String name, StoreCategoryNode... kids) {
    return new StoreCategoryNode(id, name, List.of(kids));
  }

  @Test
  void writesTheShopsTreeTheFirstTime() {
    mercadona.publish(node("112", "Cereales", node("113", "Avena")));

    int written = service.sync("MERCADONA");

    assertThat(written).isEqualTo(2);
    assertThat(service.findByStore("MERCADONA"))
        .extracting(StoreCategory::name)
        .containsExactly("Cereales", "Avena");
  }

  /** A shop renaming a shelf is a rename, not a new shelf: the row is the same row. */
  @Test
  void updatesAnAisleTheShopRenamed() {
    mercadona.publish(node("112", "Cereales"));
    service.sync("MERCADONA");

    mercadona.publish(node("112", "Cereales y galletas"));
    service.sync("MERCADONA");

    assertThat(service.findByStore("MERCADONA"))
        .singleElement()
        .satisfies(c -> assertThat(c.name()).isEqualTo("Cereales y galletas"));
  }

  /**
   * An aisle the shop stopped publishing is retired, never deleted. Products bought from it still
   * point at it, and a shopping history that loses where something came from is worse than one that
   * says "this shelf no longer exists".
   */
  @Test
  void retiresAnAisleTheShopStoppedPublishing() {
    mercadona.publish(node("112", "Cereales"), node("200", "Congelados"));
    service.sync("MERCADONA");

    mercadona.publish(node("112", "Cereales"));
    service.sync("MERCADONA");

    assertThat(repository.byId("MERCADONA:200"))
        .get()
        .satisfies(c -> assertThat(c.enabled()).isFalse());
    assertThat(service.findByStore("MERCADONA"))
        .extracting(StoreCategory::externalId)
        .containsExactly("112");
  }

  /** And it comes back enabled if the shop lists it again, rather than staying retired forever. */
  @Test
  void bringsARetiredAisleBackWhenTheShopListsItAgain() {
    mercadona.publish(node("112", "Cereales"), node("200", "Congelados"));
    service.sync("MERCADONA");
    mercadona.publish(node("112", "Cereales"));
    service.sync("MERCADONA");

    mercadona.publish(node("112", "Cereales"), node("200", "Congelados"));
    service.sync("MERCADONA");

    assertThat(service.findByStore("MERCADONA")).hasSize(2);
  }

  /** A shop with no catalogue behind it has no aisles to ask for. */
  @Test
  void refusesToSyncAShopWithNoCatalogue() {
    assertThatThrownBy(() -> service.sync("OTRAS"))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("OTRAS");
  }

  @Test
  void refusesAShopThatIsNotOneOfOurs() {
    assertThatThrownBy(() -> service.sync("LIDL"))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("LIDL");
    assertThatThrownBy(() -> service.findByStore("LIDL")).isInstanceOf(ValidationException.class);
  }

  /**
   * A source that publishes nothing has to mean "no aisles I can see", not "the shop closed":
   * wiping a tree because one crawl came back thin would retire every aisle at once.
   */
  @Test
  void leavesTheTreeAloneWhenTheShopPublishesNothing() {
    mercadona.publish(node("112", "Cereales"));
    service.sync("MERCADONA");

    mercadona.publish();
    int written = service.sync("MERCADONA");

    assertThat(written).isZero();
    assertThat(service.findByStore("MERCADONA")).hasSize(1);
  }

  private static final class FakeSource implements StoreCatalogSource {
    private final String store;
    private List<StoreCategoryNode> roots = List.of();

    FakeSource(String store) {
      this.store = store;
    }

    void publish(StoreCategoryNode... nodes) {
      roots = List.of(nodes);
    }

    @Override
    public String store() {
      return store;
    }

    @Override
    public List<StoreCategoryNode> categories() {
      return roots;
    }

    @Override
    public List<ImportableProduct> products() {
      return List.of();
    }

    @Override
    public Optional<ImportableProduct> findByExternalId(String externalId) {
      return Optional.empty();
    }
  }

  private static final class InMemoryCategories implements StoreCategoryRepository {
    private final Map<String, StoreCategory> rows = new LinkedHashMap<>();

    Optional<StoreCategory> byId(String id) {
      return Optional.ofNullable(rows.get(id));
    }

    @Override
    public List<StoreCategory> findByStore(String storeId, boolean includeRetired) {
      List<StoreCategory> found = new ArrayList<>();
      for (StoreCategory row : rows.values()) {
        if (row.storeId().equals(storeId) && (includeRetired || row.enabled())) {
          found.add(row);
        }
      }
      return found;
    }

    @Override
    public Optional<StoreCategory> find(String id) {
      return byId(id);
    }

    @Override
    public void save(StoreCategory category) {
      rows.put(category.id(), category);
    }

    @Override
    public void retire(String id) {
      StoreCategory row = rows.get(id);
      if (row != null) {
        rows.put(
            id,
            new StoreCategory(
                row.id(),
                row.storeId(),
                row.parentId(),
                row.externalId(),
                row.name(),
                row.slug(),
                row.level(),
                row.sortOrder(),
                false));
      }
    }
  }

  private static final class InMemoryStores implements StoreRepository {
    private final Map<String, Store> rows =
        new LinkedHashMap<>(
            Map.of(
                "MERCADONA", new Store("MERCADONA", "Mercadona", null, null, 1, true),
                "OTRAS", new Store("OTRAS", "Otras", null, null, 99, true)));

    @Override
    public List<Store> findAll() {
      return List.copyOf(rows.values());
    }

    @Override
    public Optional<Store> find(String id) {
      return Optional.ofNullable(rows.get(id));
    }
  }
}
