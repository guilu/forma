package dev.diegobarrioh.forma.delivery.food;

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
 * Authorisation on the catalog maintenance endpoints (FOR-190).
 *
 * <p>A full application context rather than a web slice on purpose: the point of these tests is the
 * security filter chain, and a slice with a mocked service would assert the controller's behaviour
 * while skipping the very rule under test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FoodCatalogAdminTest {

  private static final String PATH = "/api/v1/foods";
  private static final UUID SOMEONE = UUID.randomUUID();

  private static final String BODY =
      """
      {"id":"tempeh","name":"Tempeh","kcal":190,"proteinG":19.0,"carbsG":9.0,"fatG":11.0,
       "servingSizeG":100.0,"category":"PROTEINA"}
      """;

  @Autowired private MockMvc mockMvc;

  @Test
  void anOrdinaryUserCannotCreateAFood() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .with(asUser(SOMEONE, "someone@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isForbidden());
  }

  @Test
  void anOrdinaryUserCannotUpdateOrDeleteAFood() throws Exception {
    mockMvc
        .perform(
            put(PATH + "/oats")
                .with(asUser(SOMEONE, "someone@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(delete(PATH + "/oats").with(asUser(SOMEONE, "someone@forma.test")).with(csrf()))
        .andExpect(status().isForbidden());
  }

  /** Reading stays open to every authenticated account: the catalog is what the app runs on. */
  @Test
  void anOrdinaryUserCanStillReadTheCatalog() throws Exception {
    mockMvc
        .perform(get(PATH).with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(status().isOk());
  }

  @Test
  void anAdminCanCreateUpdateAndDeleteAFood() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .with(asAdmin(SOMEONE, "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("tempeh"))
        .andExpect(jsonPath("$.category").value("PROTEINA"));

    mockMvc
        .perform(
            put(PATH + "/tempeh")
                .with(asAdmin(SOMEONE, "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("Tempeh", "Tempeh ecológico")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Tempeh ecológico"));

    mockMvc
        .perform(delete(PATH + "/tempeh").with(asAdmin(SOMEONE, "admin@forma.test")).with(csrf()))
        .andExpect(status().isNoContent());
  }
}
