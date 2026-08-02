package dev.diegobarrioh.forma.adapter.mercadona;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.application.ImportableProduct;
import dev.diegobarrioh.forma.application.StoreCatalogUnavailableException;
import dev.diegobarrioh.forma.domain.Store;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Crawling and parsing Mercadona's own catalogue (FOR-194), against a fake transport — this suite
 * never opens a socket, so it cannot go red because a shop changed its prices or its Wi-Fi.
 *
 * <p>The fixtures below are trimmed copies of real responses from {@code
 * tienda.mercadona.es/api/…}, so the parsing is written against what the API actually sends rather
 * than what its shape was assumed to be.
 */
class MercadonaCatalogAdapterTest {

  private static final String CATEGORIES =
      """
      {"count":1,"next":null,"previous":null,"results":[
        {"id":12,"name":"Aceite, especias y salsas","order":7,"is_extended":false,
         "categories":[
           {"id":112,"name":"Aceite, vinagre y sal","order":7,"published":true},
           {"id":115,"name":"Especias","order":8,"published":true}]}]}
      """;

  private static final String SUBCATEGORY_112 =
      """
      {"id":112,"name":"Aceite, vinagre y sal","categories":[
        {"id":789,"name":"Aceite","products":[
          {"id":"4241","slug":"aceite-oliva-04o-hacendado-garrafa","packaging":"Garrafa",
           "published":true,
           "share_url":"https://tienda.mercadona.es/product/4241/aceite-oliva-04o-hacendado-garrafa",
           "thumbnail":"https://prod-mercadona.imgix.net/images/x.jpg",
           "display_name":"Aceite de oliva 0,4º Hacendado",
           "price_instructions":{"unit_price":"17.75","bulk_price":"3.55","size_format":"l",
             "unit_size":5.0,"reference_price":"3.550","reference_format":"L"}},
          {"id":"4242","packaging":"Botella","published":true,
           "share_url":"https://tienda.mercadona.es/product/4242/aceite-girasol",
           "display_name":"Aceite de girasol Hacendado",
           "price_instructions":{"unit_price":"1.95"}}]}]}
      """;

  private static final String SUBCATEGORY_115 =
      """
      {"id":115,"name":"Especias","categories":[
        {"id":790,"name":"Especias","products":[
          {"id":"5000","packaging":"Bote","published":true,
           "share_url":"https://tienda.mercadona.es/product/5000/pimienta",
           "display_name":"Pimienta negra molida Hacendado",
           "price_instructions":{"unit_price":"0.95"}}]}]}
      """;

  private final RecordingTransport transport = new RecordingTransport();
  private final MercadonaCatalogAdapter adapter = new MercadonaCatalogAdapter(transport);

  @Test
  void speaksForMercadona() {
    assertThat(adapter.store()).isEqualTo(Store.MERCADONA);
  }

  @Test
  void crawlsEverySubcategoryAndMapsWhatTheShopSays() {
    transport.serve(
        "https://tienda.mercadona.es/api/categories/", CATEGORIES,
        "https://tienda.mercadona.es/api/categories/112/", SUBCATEGORY_112,
        "https://tienda.mercadona.es/api/categories/115/", SUBCATEGORY_115);

    List<ImportableProduct> products = adapter.products();

    assertThat(products).hasSize(3);
    assertThat(products)
        .first()
        .satisfies(
            product -> {
              assertThat(product.externalId()).isEqualTo("4241");
              assertThat(product.name()).isEqualTo("Aceite de oliva 0,4º Hacendado");
              // Packaging alone reads as "Garrafa"; the size is what makes it a fact.
              assertThat(product.packaging()).isEqualTo("Garrafa 5 l");
              assertThat(product.priceEur()).isEqualByComparingTo("17.75");
              assertThat(product.url())
                  .endsWith("/product/4241/aceite-oliva-04o-hacendado-garrafa");
              assertThat(product.storeCategory()).isEqualTo("Aceite, vinagre y sal");
            });
  }

  /** The catalogue is 151 requests. Answering a second question must not repeat them. */
  @Test
  void fetchesTheShelfOnceAndReusesIt() {
    transport.serve(
        "https://tienda.mercadona.es/api/categories/", CATEGORIES,
        "https://tienda.mercadona.es/api/categories/112/", SUBCATEGORY_112,
        "https://tienda.mercadona.es/api/categories/115/", SUBCATEGORY_115);

    adapter.products();
    adapter.products();

    assertThat(transport.callCount()).isEqualTo(3);
  }

  /**
   * One subcategory failing must not lose the other 150. A partial shelf still finds most foods; an
   * exception here would make the whole import depend on the least reliable corner of their API.
   */
  @Test
  void keepsGoingWhenOneSubcategoryFails() {
    transport.serve(
        "https://tienda.mercadona.es/api/categories/", CATEGORIES,
        "https://tienda.mercadona.es/api/categories/115/", SUBCATEGORY_115);

    assertThat(adapter.products())
        .extracting(ImportableProduct::externalId)
        .containsExactly("5000");
  }

  /**
   * The index failing is different: with no categories there is nothing to crawl, and answering
   * with an empty list would say "Mercadona sells nothing".
   */
  @Test
  void reportsUnavailableWhenTheIndexCannotBeRead() {
    assertThatThrownBy(adapter::products).isInstanceOf(StoreCatalogUnavailableException.class);
  }

  /**
   * A product with no price is listed but not orderable; it is still the product being looked for.
   */
  @Test
  void keepsAProductWhosePriceIsMissing() {
    transport.serve(
        "https://tienda.mercadona.es/api/categories/",
        """
        {"results":[{"id":1,"name":"X","categories":[{"id":2,"name":"Y"}]}]}
        """,
        "https://tienda.mercadona.es/api/categories/2/",
        """
        {"id":2,"name":"Y","categories":[{"id":3,"name":"Z","products":[
          {"id":"9","display_name":"Producto sin precio","published":true,
           "share_url":"https://tienda.mercadona.es/product/9","price_instructions":{}}]}]}
        """);

    assertThat(adapter.products())
        .singleElement()
        .satisfies(
            product -> {
              assertThat(product.name()).isEqualTo("Producto sin precio");
              assertThat(product.priceEur()).isNull();
            });
  }

  private static final class RecordingTransport implements MercadonaHttpTransport {
    private final Map<String, String> bodies = new LinkedHashMap<>();
    private final List<String> calls = new ArrayList<>();

    void serve(String... urlThenBody) {
      for (int i = 0; i < urlThenBody.length; i += 2) {
        bodies.put(urlThenBody[i], urlThenBody[i + 1]);
      }
    }

    int callCount() {
      return calls.size();
    }

    @Override
    public String get(String url) {
      calls.add(url);
      String body = bodies.get(url);
      if (body == null) {
        throw new StoreCatalogUnavailableException("Sin respuesta para " + url);
      }
      return body;
    }
  }
}
