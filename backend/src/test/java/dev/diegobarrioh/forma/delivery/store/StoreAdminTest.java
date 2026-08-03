package dev.diegobarrioh.forma.delivery.store;

import static dev.diegobarrioh.forma.support.AuthTestSupport.asAdmin;
import static dev.diegobarrioh.forma.support.AuthTestSupport.asUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The store endpoints end to end (V45, V46): that the seeded chains are really there, and who may
 * ask a shop for its aisles.
 *
 * <p>A full context rather than a web slice, for the same reason as {@code StoreProductAdminTest}:
 * the rule under test is the security filter chain, which a mocked slice would skip.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StoreAdminTest {

  private static final String PATH = "/api/v1/stores";
  private static final UUID SOMEONE = UUID.randomUUID();

  @Autowired private MockMvc mockMvc;

  /** Every screen that lets someone say where a product was bought needs the list. */
  @Test
  void anyoneSignedInReadsTheChains() throws Exception {
    mockMvc
        .perform(get(PATH).with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id=='MERCADONA')].name").value("Mercadona"))
        .andExpect(jsonPath("$[?(@.id=='OTRAS')].website").value(org.hamcrest.Matchers.hasSize(1)))
        // "Otras" belongs last however the names sort.
        .andExpect(jsonPath("$[2].id").value("OTRAS"));
  }

  /**
   * The aisles start empty and stay that way until somebody syncs: a shop's shelves are the shop's
   * to state, and nothing here invents them.
   */
  @Test
  void aChainsAislesAreEmptyUntilSynced() throws Exception {
    mockMvc
        .perform(get(PATH + "/MERCADONA/categories").with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void anUnknownChainIsRejectedRatherThanAnsweredEmpty() throws Exception {
    mockMvc
        .perform(get(PATH + "/LIDL/categories").with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(status().isBadRequest());
  }

  /** Syncing hits somebody else's server, so it is not something any signed-in account may do. */
  @Test
  void anOrdinaryUserCannotSyncAChainsAisles() throws Exception {
    mockMvc
        .perform(
            post(PATH + "/MERCADONA/categories/sync")
                .with(asUser(SOMEONE, "someone@forma.test"))
                .with(csrf()))
        .andExpect(status().isForbidden());
  }

  /**
   * OTRAS has no catalogue by definition — it is where things bought at a market stall go — so
   * there is nothing to ask, and saying so beats reporting a failure.
   */
  @Test
  void syncingAChainWithNoCatalogueSaysSoRatherThanFailing() throws Exception {
    mockMvc
        .perform(
            post(PATH + "/OTRAS/categories/sync")
                .with(asAdmin(SOMEONE, "admin@forma.test"))
                .with(csrf()))
        .andExpect(status().isNotFound());
  }
}
