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
        // The link to our own food catalog, which is ours and no migration overwrites it.
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

  /**
   * Editing an imported product keeps it imported, and keeps whatever photo the edit carries. The
   * form has no field for the shop's id — it is provenance, not data somebody types — so a save
   * used to hand back a body without it and the row quietly lost both that id and its picture: no
   * thumbnail in the list, and no refresh action, since the screen only offers one where a source
   * exists.
   */
  @Test
  void anEditKeepsTheShopsIdAndStoresTheImage() throws Exception {
    String imported =
        """
        {"id":"mercadona-seitan","store":"MERCADONA","name":"Seitán","foodId":null,
         "packageSize":"250 g","priceEur":2.45,"url":"https://tienda.mercadona.es/product/77",
         "category":"PROTEINAS","notes":null,"externalId":"77","imageUrl":null}
        """;
    mockMvc
        .perform(
            post(PATH)
                .with(asAdmin(UUID.randomUUID(), "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(imported))
        .andExpect(status().isCreated());

    // What the edit form sends: no externalId, and a photo the admin pasted.
    mockMvc
        .perform(
            put(PATH + "/mercadona-seitan")
                .with(asAdmin(UUID.randomUUID(), "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    imported
                        .replace("\"externalId\":\"77\"", "\"externalId\":null")
                        .replace(
                            "\"imageUrl\":null", "\"imageUrl\":\"https://cdn.test/seitan.jpg\"")
                        .replace("Seitán", "Seitán ecológico")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Seitán ecológico"))
        .andExpect(jsonPath("$.externalId").value("77"))
        .andExpect(jsonPath("$.imageUrl").value("https://cdn.test/seitan.jpg"));

    mockMvc
        .perform(
            delete(PATH + "/mercadona-seitan")
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

  /**
   * V40 matched the transcribed seed against Mercadona's own catalogue, which is what makes those
   * rows refreshable at all: without a shop id there is nothing to re-read them from.
   */
  @Test
  void theSeededProductsCarryTheShopsIdAndPhoto() throws Exception {
    mockMvc
        .perform(get(PATH).with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id=='mercadona-oats')].externalId").value("86341"))
        // Stored at the size the list draws it, not at the 300px the shop hands out.
        .andExpect(
            jsonPath("$[?(@.id=='mercadona-oats')].imageUrl")
                .value(
                    org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.containsString("h=24&w=24"))))
        // And the figures are the shop's own now, not the spreadsheet's.
        .andExpect(jsonPath("$[?(@.id=='mercadona-oats')].priceEur").value(1.30));
  }

  /**
   * Mercadona sells neither whey protein nor boniato, so those two keep no shop id — and the screen
   * offers no refresh where there is no source. A link invented to fill the column would point at a
   * product nobody chose.
   */
  @Test
  void leavesUnmatchableProductsWithoutAShopId() throws Exception {
    mockMvc
        .perform(get(PATH).with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$[?(@.id=='mercadona-whey-protein')].externalId")
                .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.nullValue())))
        .andExpect(
            jsonPath("$[?(@.id=='mercadona-sweet-potato')].externalId")
                .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.nullValue())));
  }

  /**
   * The photo lookup makes this server fetch a URL somebody typed, so it is admin-only for the same
   * reason the import is: an open endpoint here is a way to make our server read what only it can
   * reach.
   */
  @Test
  void anOrdinaryUserCannotMakeTheServerReadALink() throws Exception {
    mockMvc
        .perform(
            get(PATH + "/link-image")
                .param("url", "https://tienda.example/producto")
                .with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(status().isForbidden());
  }

  /** A private address is refused before anything is fetched, and it reads as a bad request. */
  @Test
  void refusesToReadAnInternalAddress() throws Exception {
    mockMvc
        .perform(
            get(PATH + "/link-image")
                .param("url", "http://169.254.169.254/latest/meta-data/")
                .with(asAdmin(UUID.randomUUID(), "admin@forma.test")))
        .andExpect(status().isBadRequest());
  }
}
