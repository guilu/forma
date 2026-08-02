package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.FoodCategory;
import dev.diegobarrioh.forma.domain.Store;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Ranking the store's shelf against a food in our catalog (FOR-194).
 *
 * <p>The admin confirms every match, so the job here is to put the right product within reach —
 * never to decide. Hand-rolled fakes, no Spring, no network (ADR-007).
 */
class StoreProductImportServiceTest {

  private final InMemoryFoods foods = new InMemoryFoods();
  private final FakeSource mercadona = new FakeSource(Store.MERCADONA);
  private final StoreProductImportService service =
      new StoreProductImportService(foods, List.of(mercadona));

  private static ImportableProduct product(String id, String name) {
    return new ImportableProduct(
        id,
        name,
        "500 g",
        new BigDecimal("1.55"),
        "https://tienda.mercadona.es/product/" + id,
        "8480000000000",
        "Cereales",
        "https://cdn/" + id + ".jpg");
  }

  @Test
  void ranksTheProductsWhoseNameMatchesTheFood() {
    foods.add("oats", "Copos de avena");
    mercadona.serve(
        product("1", "Copos de avena Brüggen"),
        product("2", "Detergente líquido"),
        product("3", "Avena instantánea"));

    List<ImportableProduct> found = service.suggestionsFor("oats", Store.MERCADONA);

    assertThat(found)
        .extracting(ImportableProduct::name)
        .containsExactly("Copos de avena Brüggen", "Avena instantánea");
  }

  /**
   * "Plátano" must find "Platanos": accents and case are how a shelf label differs from a catalog
   * entry, not what makes them different products.
   */
  @Test
  void ignoresAccentsCaseAndPluralsWhenMatching() {
    foods.add("banana", "Plátano");
    mercadona.serve(product("1", "PLATANOS DE CANARIAS"));

    assertThat(service.suggestionsFor("banana", Store.MERCADONA))
        .extracting(ImportableProduct::name)
        .containsExactly("PLATANOS DE CANARIAS");
  }

  /**
   * "Aceite de oliva virgen extra" and "Leche de avena" share only "de". Matching on a word every
   * Spanish label contains would rank the whole shelf as a candidate.
   */
  @Test
  void doesNotMatchOnFillerWordsAlone() {
    foods.add("olive-oil", "Aceite de oliva virgen extra");
    mercadona.serve(product("1", "Leche de avena"), product("2", "Aceite de girasol"));

    assertThat(service.suggestionsFor("olive-oil", Store.MERCADONA))
        .extracting(ImportableProduct::name)
        .containsExactly("Aceite de girasol");
  }

  @Test
  void returnsNothingWhenTheShelfHasNoCandidate() {
    foods.add("tempeh", "Tempeh");
    mercadona.serve(product("1", "Detergente líquido"));

    assertThat(service.suggestionsFor("tempeh", Store.MERCADONA)).isEmpty();
  }

  @Test
  void refusesAFoodThatIsNotInTheCatalog() {
    assertThatThrownBy(() -> service.suggestionsFor("ghost", Store.MERCADONA))
        .isInstanceOf(NotFoundException.class);
  }

  /** Only Mercadona has a source today; asking for another chain must say so, not answer empty. */
  @Test
  void refusesAStoreWithNoSource() {
    foods.add("oats", "Copos de avena");

    assertThatThrownBy(() -> service.suggestionsFor("oats", Store.CARREFOUR))
        .isInstanceOf(NotFoundException.class);
  }

  /**
   * A shelf of thousands is not a list anyone reads. The cap is what keeps this a confirmation step
   * rather than a second catalog to browse.
   */
  @Test
  void capsTheNumberOfSuggestions() {
    foods.add("oats", "Avena");
    ImportableProduct[] many = new ImportableProduct[30];
    for (int i = 0; i < many.length; i++) {
      many[i] = product(String.valueOf(i), "Avena " + i);
    }
    mercadona.serve(many);

    assertThat(service.suggestionsFor("oats", Store.MERCADONA)).hasSize(10);
  }

  private static final class FakeSource implements StoreCatalogSource {
    private final Store store;
    private List<ImportableProduct> products = List.of();

    FakeSource(Store store) {
      this.store = store;
    }

    void serve(ImportableProduct... served) {
      products = List.of(served);
    }

    @Override
    public Store store() {
      return store;
    }

    @Override
    public List<ImportableProduct> products() {
      return products;
    }

    @Override
    public Optional<ImportableProduct> findByExternalId(String externalId) {
      return products.stream().filter(p -> p.externalId().equals(externalId)).findFirst();
    }
  }

  private static final class InMemoryFoods implements FoodCatalogRepository {
    private final Map<String, CatalogFood> rows = new LinkedHashMap<>();

    void add(String id, String name) {
      rows.put(
          id,
          new CatalogFood(
              id,
              name,
              null,
              100,
              BigDecimal.ONE,
              BigDecimal.ONE,
              BigDecimal.ONE,
              null,
              null,
              null,
              null,
              FoodCategory.CARBOHIDRATO));
    }

    @Override
    public List<CatalogFood> findAll() {
      return new ArrayList<>(rows.values());
    }

    @Override
    public Optional<CatalogFood> findById(String id) {
      return Optional.ofNullable(rows.get(id));
    }

    @Override
    public void insert(CatalogFood food) {
      rows.put(food.id(), food);
    }

    @Override
    public void update(CatalogFood food) {
      rows.put(food.id(), food);
    }

    @Override
    public boolean delete(String id) {
      return rows.remove(id) != null;
    }
  }
}
