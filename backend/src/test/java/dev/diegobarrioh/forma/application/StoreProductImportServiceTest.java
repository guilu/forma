package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
  private final FakeSource mercadona = new FakeSource("MERCADONA");
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
        null,
        "https://cdn/" + id + ".jpg");
  }

  @Test
  void ranksTheProductsWhoseNameMatchesTheFood() {
    foods.add("oats", "Copos de avena");
    mercadona.serve(
        product("1", "Copos de avena Brüggen"),
        product("2", "Detergente líquido"),
        product("3", "Avena instantánea"));

    List<ImportableProduct> found = service.suggestionsFor("oats", "MERCADONA");

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

    assertThat(service.suggestionsFor("banana", "MERCADONA"))
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

    assertThat(service.suggestionsFor("olive-oil", "MERCADONA"))
        .extracting(ImportableProduct::name)
        .containsExactly("Aceite de girasol");
  }

  @Test
  void returnsNothingWhenTheShelfHasNoCandidate() {
    foods.add("tempeh", "Tempeh");
    mercadona.serve(product("1", "Detergente líquido"));

    assertThat(service.suggestionsFor("tempeh", "MERCADONA")).isEmpty();
  }

  @Test
  void refusesAFoodThatIsNotInTheCatalog() {
    assertThatThrownBy(() -> service.suggestionsFor("ghost", "MERCADONA"))
        .isInstanceOf(NotFoundException.class);
  }

  /** Only Mercadona has a source today; asking for another chain must say so, not answer empty. */
  @Test
  void refusesAStoreWithNoSource() {
    foods.add("oats", "Copos de avena");

    assertThatThrownBy(() -> service.suggestionsFor("oats", "CARREFOUR"))
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

    assertThat(service.suggestionsFor("oats", "MERCADONA")).hasSize(10);
  }

  private static final class FakeSource implements StoreCatalogSource {
    private final String store;
    private List<ImportableProduct> products = List.of();

    FakeSource(String store) {
      this.store = store;
    }

    void serve(ImportableProduct... served) {
      products = List.of(served);
    }

    @Override
    public String store() {
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
              "CARBOHIDRATO",
              null));
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

  /**
   * Searching by text, for the products our food catalog cannot name (FOR-199). The seeded rows
   * that matched nothing — whey protein, boniato — are exactly the case: there is no food to start
   * from, so the admin types what they are looking for.
   */
  @Test
  void searchesTheShelfByText() {
    mercadona.serve(
        product("1", "Almendra natural Hacendado"),
        product("2", "Almendra molida Hacendado"),
        product("3", "Detergente líquido"));

    assertThat(service.searchFor("almendra", "MERCADONA"))
        .extracting(ImportableProduct::name)
        .containsExactly("Almendra molida Hacendado", "Almendra natural Hacendado");
  }

  /** Same normalisation as the food match: a shelf label is not written like a search box. */
  @Test
  void searchIgnoresAccentsAndCase() {
    mercadona.serve(product("1", "Plátano de Canarias IGP"));

    assertThat(service.searchFor("PLATANO", "MERCADONA")).hasSize(1);
  }

  /** Two words mean both, not either: "aceite oliva" must not return every oil in the shop. */
  @Test
  void searchRequiresEveryWordTyped() {
    mercadona.serve(
        product("1", "Aceite de oliva virgen extra Hacendado"),
        product("2", "Aceite de girasol Hacendado"));

    assertThat(service.searchFor("aceite oliva", "MERCADONA"))
        .extracting(ImportableProduct::name)
        .containsExactly("Aceite de oliva virgen extra Hacendado");
  }

  /** A blank search is not a request for the whole shop. */
  @Test
  void refusesAnEmptySearch() {
    mercadona.serve(product("1", "Cualquier cosa"));

    assertThat(service.searchFor("   ", "MERCADONA")).isEmpty();
  }

  @Test
  void searchRefusesAStoreWithNoSource() {
    assertThatThrownBy(() -> service.searchFor("avena", "CARREFOUR"))
        .isInstanceOf(NotFoundException.class);
  }
}
