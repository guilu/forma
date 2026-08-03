package dev.diegobarrioh.forma.adapter.mercadona;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.diegobarrioh.forma.application.ImportableProduct;
import dev.diegobarrioh.forma.application.StoreCatalogSource;
import dev.diegobarrioh.forma.application.StoreCatalogUnavailableException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Reads Mercadona's own catalogue off the JSON their shop serves at {@code
 * tienda.mercadona.es/api/} (FOR-194).
 *
 * <p><strong>This is not a public API.</strong> It is the shop's own, undocumented, unversioned and
 * free to change or refuse at any time. Everything here is written to that fact: one snapshot per
 * {@link #CACHE_TTL}, a subcategory that fails is skipped rather than fatal, and failures surface
 * as {@link StoreCatalogUnavailableException} so the import degrades on its own without taking a
 * catalog screen with it. It is a convenience for filling our catalog, never a dependency of it.
 *
 * <p><strong>Why a full crawl.</strong> Their API has no search endpoint — {@code
 * /api/products/?q=}, {@code /api/search/} and {@code /api/products/search/} all answer 404; the
 * shop's own search runs on a separate Algolia service with keys embedded in their frontend, which
 * is not something to build on. So answering "which products look like oats" means holding the
 * shelf: 26 categories, 151 subcategories, ~4,600 products, one request each. Once per day, not
 * once per question.
 *
 * <p><strong>What it cannot give us.</strong> Their {@code nutrition_information} carries allergens
 * and an ingredients string — no kcal, no macros. This fills {@code store_product}; {@code
 * food_catalog} stays hand-curated, and which food a product *is* remains an admin's judgement.
 */
@Component
public class MercadonaCatalogAdapter implements StoreCatalogSource {

  private static final Logger log = LoggerFactory.getLogger(MercadonaCatalogAdapter.class);

  private static final String CATEGORIES_URL = "https://tienda.mercadona.es/api/categories/";
  private static final String PRODUCT_URL = "https://tienda.mercadona.es/api/products/";
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Long enough that browsing the admin screen never triggers a second crawl, short enough that a
   * price is at worst a day old. The screen shows when the snapshot was taken, so a stale figure
   * looks stale rather than authoritative.
   */
  private static final Duration CACHE_TTL = Duration.ofHours(24);

  private final MercadonaHttpTransport transport;

  private List<ImportableProduct> snapshot;
  private Instant snapshotTakenAt;

  public MercadonaCatalogAdapter(MercadonaHttpTransport transport) {
    this.transport = transport;
  }

  @Override
  public String store() {
    return "MERCADONA";
  }

  @Override
  public synchronized List<ImportableProduct> products() {
    if (snapshot == null || Instant.now().isAfter(snapshotTakenAt.plus(CACHE_TTL))) {
      snapshot = crawl();
      snapshotTakenAt = Instant.now();
    }
    return snapshot;
  }

  /**
   * One product, straight from the shop.
   *
   * <p>Deliberately not answered from the snapshot: a refresh is asked for precisely when the held
   * copy might be stale, and this is one request rather than 151.
   *
   * <p>A product the shop has stopped listing answers 404, which the transport turns into an
   * unavailable-catalogue failure. Here that is not a failure but the answer — the product is gone
   * — so it becomes an empty Optional and the screen can say so.
   */
  @Override
  public Optional<ImportableProduct> findByExternalId(String externalId) {
    JsonNode product;
    try {
      product = parse(transport.get(PRODUCT_URL + externalId + "/"));
    } catch (StoreCatalogUnavailableException ex) {
      log.info("Mercadona: el producto {} ya no está disponible ({})", externalId, ex.getMessage());
      return Optional.empty();
    }
    JsonNode shelf = product.path("categories");
    String shelfName = shelf.isEmpty() ? null : shelf.get(shelf.size() - 1).path("name").asText();
    return Optional.of(toImportable(product, shelfName));
  }

  /** When the held snapshot was taken; empty until the first successful crawl. */
  public synchronized Instant snapshotTakenAt() {
    return snapshotTakenAt;
  }

  private List<ImportableProduct> crawl() {
    JsonNode index = parse(transport.get(CATEGORIES_URL));
    List<ImportableProduct> found = new ArrayList<>();
    for (JsonNode root : index.path("results")) {
      for (JsonNode subcategory : root.path("categories")) {
        int id = subcategory.path("id").asInt();
        String shelfName = subcategory.path("name").asText();
        try {
          collectProducts(parse(transport.get(CATEGORIES_URL + id + "/")), shelfName, found);
        } catch (StoreCatalogUnavailableException ex) {
          // One shelf out of 151 is not worth failing the import for: a partial
          // catalogue still finds most foods, and the alternative makes every
          // import depend on the least reliable corner of their API.
          log.warn("Mercadona: no se pudo leer la subcategoría {} ({})", id, ex.getMessage());
        }
      }
    }
    return List.copyOf(found);
  }

  /** Products hang off a third level of nesting, one per shelf within the subcategory. */
  private static void collectProducts(
      JsonNode subcategory, String shelfName, List<ImportableProduct> into) {
    for (JsonNode shelf : subcategory.path("categories")) {
      for (JsonNode product : shelf.path("products")) {
        into.add(toImportable(product, shelfName));
      }
    }
  }

  private static ImportableProduct toImportable(JsonNode product, String shelfName) {
    JsonNode prices = product.path("price_instructions");
    return new ImportableProduct(
        product.path("id").asText(),
        product.path("display_name").asText(),
        packaging(product, prices),
        // Absent, not zero: a listing with no price is one that cannot be
        // ordered today, which is a different fact from a free product.
        prices.path("unit_price").isMissingNode() || prices.path("unit_price").isNull()
            ? null
            : new BigDecimal(prices.path("unit_price").asText()),
        product.path("share_url").asText(null),
        product.path("ean").asText(null),
        shelfName,
        product.path("thumbnail").asText(null));
  }

  /**
   * "Garrafa 5 l" rather than "Garrafa": the container alone says nothing about how much is in it,
   * and the size is what a price hangs off.
   */
  private static String packaging(JsonNode product, JsonNode prices) {
    String container = product.path("packaging").asText(null);
    JsonNode size = prices.path("unit_size");
    String format = prices.path("size_format").asText(null);
    if (size.isMissingNode() || size.isNull() || format == null) {
      return container;
    }
    String amount = trimTrailingZero(size.asDouble());
    return container == null ? amount + " " + format : container + " " + amount + " " + format;
  }

  private static String trimTrailingZero(double value) {
    return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
  }

  private static JsonNode parse(String body) {
    try {
      return MAPPER.readTree(body);
    } catch (JsonProcessingException ex) {
      throw new StoreCatalogUnavailableException("Mercadona respondió algo que no es JSON", ex);
    }
  }
}
