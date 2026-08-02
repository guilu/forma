package dev.diegobarrioh.forma.delivery.store;

import static dev.diegobarrioh.forma.support.AuthTestSupport.asAdmin;
import static dev.diegobarrioh.forma.support.AuthTestSupport.asUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The store catalog endpoints end to end (FOR-191): who may write, and that the V36 seed is really
 * there.
 *
 * <p>A full context rather than a web slice, for the same reason as {@code FoodCatalogAdminTest}:
 * the rule under test is the security filter chain, which a mocked slice would skip.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StoreProductAdminTest {

  private static final String PATH = "/api/v1/store-products";
  private static final UUID SOMEONE = UUID.randomUUID();

  private static final String BODY =
      """
      {"id":"mercadona-tempeh","store":"MERCADONA","name":"Tempeh Hacendado","foodId":null,
       "packageSize":"250 g","priceEur":2.45,"url":"https://tienda.mercadona.es/product/1",
       "category":"PROTEINAS","notes":null}
      """;

  @Autowired private MockMvc mockMvc;

  @Test
  void anOrdinaryUserCannotWriteToTheCatalog() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .with(asUser(SOMEONE, "someone@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            put(PATH + "/mercadona-oats")
                .with(asUser(SOMEONE, "someone@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            delete(PATH + "/mercadona-oats")
                .with(asUser(SOMEONE, "someone@forma.test"))
                .with(csrf()))
        .andExpect(status().isForbidden());
  }

  /**
   * Reading stays open to any signed-in account: the catalog is what a shopping list will be built
   * from, so every user needs it even though only an admin curates it.
   */
  @Test
  void anyoneSignedInCanReadTheCatalogAndFilterItByStore() throws Exception {
    mockMvc
        .perform(get(PATH).with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id=='mercadona-oats')].name").value("Copos de avena Brüggen"))
        // V36 seeds the product's own price, not the prorated weekly cost V22 stored.
        .andExpect(jsonPath("$[?(@.id=='mercadona-oats')].priceEur").value(1.55))
        .andExpect(jsonPath("$[?(@.id=='mercadona-oats')].foodId").value("oats"));

    mockMvc
        .perform(get(PATH + "?store=CARREFOUR").with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void anAdminCreatesUpdatesAndDeletesAProduct() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .with(asAdmin(UUID.randomUUID(), "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("mercadona-tempeh"))
        .andExpect(jsonPath("$.store").value("MERCADONA"));

    mockMvc
        .perform(
            put(PATH + "/mercadona-tempeh")
                .with(asAdmin(UUID.randomUUID(), "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("Tempeh Hacendado", "Tempeh ecológico")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Tempeh ecológico"));

    mockMvc
        .perform(
            delete(PATH + "/mercadona-tempeh")
                .with(asAdmin(UUID.randomUUID(), "admin@forma.test"))
                .with(csrf()))
        .andExpect(status().isNoContent());
  }

  /** An unknown store is a client mistake, not an empty catalog. */
  @Test
  void rejectsAnUnknownStoreFilter() throws Exception {
    mockMvc
        .perform(get(PATH + "?store=ALCAMPO").with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(status().isBadRequest());
  }
}
