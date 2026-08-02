package dev.diegobarrioh.forma.delivery.store;

import static dev.diegobarrioh.forma.support.AuthTestSupport.asAdmin;
import static dev.diegobarrioh.forma.support.AuthTestSupport.asUser;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.adapter.mercadona.MercadonaHttpTransport;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The import endpoint end to end (FOR-194): who may ask, and what the answer looks like.
 *
 * <p>The HTTP transport is stubbed, so this suite never reaches Mercadona — a test that crawls
 * somebody else's shop is both rude and flaky.
 *
 * <p>Stubbing the transport rather than the {@link StoreCatalogSource} bean is deliberate. The
 * first attempt supplied a @Primary fixture source, which does NOT displace the real adapter:
 * Spring injects every bean of the type into the service's {@code List<StoreCatalogSource>}, so the
 * real crawler stayed in the list and this test spent twenty seconds reading Mercadona's actual
 * shop. One seam lower there is only one implementation to stub, and the parsing gets exercised
 * too.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StoreImportControllerTest {

  private static final String PATH = "/api/v1/store-products/suggestions";
  private static final UUID SOMEONE = UUID.randomUUID();

  private static final String CATEGORIES =
      """
      {"results":[{"id":1,"name":"Despensa","categories":[{"id":11,"name":"Cereales"}]}]}
      """;

  private static final String SHELF =
      """
      {"id":11,"name":"Cereales","categories":[{"id":111,"name":"Cereales","products":[
        {"id":"4241","packaging":"Caja","display_name":"Copos de avena Brüggen","ean":"8480000123456",
         "share_url":"https://tienda.mercadona.es/product/4241",
         "price_instructions":{"unit_price":"1.55","unit_size":500.0,"size_format":"g"}},
        {"id":"9999","packaging":"Botella","display_name":"Detergente líquido",
         "share_url":"https://tienda.mercadona.es/product/9999",
         "price_instructions":{"unit_price":"4.20"}}]}]}
      """;

  @Autowired private MockMvc mockMvc;

  @MockBean private MercadonaHttpTransport transport;

  @BeforeEach
  void serveTheShelf() {
    when(transport.get("https://tienda.mercadona.es/api/categories/")).thenReturn(CATEGORIES);
    when(transport.get("https://tienda.mercadona.es/api/categories/11/")).thenReturn(SHELF);
  }

  @Test
  void anOrdinaryUserCannotMakeTheServerCrawlAShop() throws Exception {
    mockMvc
        .perform(
            get(PATH)
                .param("foodId", "oats")
                .param("store", "MERCADONA")
                .with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(status().isForbidden());
  }

  @Test
  void anAdminGetsTheMatchingProductsOnly() throws Exception {
    mockMvc
        .perform(
            get(PATH)
                .param("foodId", "oats")
                .param("store", "MERCADONA")
                .with(asAdmin(UUID.randomUUID(), "admin@forma.test")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Copos de avena Brüggen"))
        .andExpect(jsonPath("$[0].externalId").value("4241"))
        .andExpect(jsonPath("$[0].priceEur").value(1.55))
        // The shop's own aisle travels as a hint; it is never mapped onto ours.
        .andExpect(jsonPath("$[0].storeCategory").value("Cereales"));
  }

  @Test
  void anUnknownFoodIsNotFound() throws Exception {
    mockMvc
        .perform(
            get(PATH)
                .param("foodId", "no-existe")
                .param("store", "MERCADONA")
                .with(asAdmin(UUID.randomUUID(), "admin@forma.test")))
        .andExpect(status().isNotFound());
  }

  /** A chain with no source behind it is a different answer from "the shop stocks nothing". */
  @Test
  void aChainWithNoSourceIsNotFound() throws Exception {
    mockMvc
        .perform(
            get(PATH)
                .param("foodId", "oats")
                .param("store", "CARREFOUR")
                .with(asAdmin(UUID.randomUUID(), "admin@forma.test")))
        .andExpect(status().isNotFound());
  }
}
