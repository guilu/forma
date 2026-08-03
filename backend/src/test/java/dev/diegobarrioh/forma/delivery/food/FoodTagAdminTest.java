package dev.diegobarrioh.forma.delivery.food;

import static dev.diegobarrioh.forma.support.AuthTestSupport.asAdmin;
import static dev.diegobarrioh.forma.support.AuthTestSupport.asUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * Labels end to end (V50): who may set them, and that setting them replaces rather than adds.
 *
 * <p>A full context rather than a web slice, for the same reason as {@code FoodCatalogAdminTest}:
 * one of the rules under test is the security filter chain, which a mocked slice would skip.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FoodTagAdminTest {

  private static final String FOOD_TAGS = "/api/v1/foods/salad/tags";
  private static final UUID SOMEONE = UUID.randomUUID();

  @Autowired private MockMvc mockMvc;

  /** The vocabulary is seeded; the labelling is not. */
  @Test
  void everyoneReadsTheVocabularyAndNoFoodStartsLabelled() throws Exception {
    mockMvc
        .perform(get("/api/v1/tags").with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(12))
        .andExpect(jsonPath("$[0].id").value("vegano"));

    mockMvc
        .perform(get(FOOD_TAGS).with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void anOrdinaryUserCannotLabelAFood() throws Exception {
    mockMvc
        .perform(
            put(FOOD_TAGS)
                .with(asUser(SOMEONE, "someone@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tagIds\":[\"vegano\"]}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void anAdminLabelsAFoodAndReadsItBackInTheVocabularysOrder() throws Exception {
    mockMvc
        .perform(
            put(FOOD_TAGS)
                .with(asAdmin(SOMEONE, "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                // Sent out of order on purpose: the answer comes back in the vocabulary's.
                .content("{\"tagIds\":[\"fresco\",\"vegano\"]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value("vegano"))
        .andExpect(jsonPath("$[1].id").value("fresco"));
  }

  /** Setting is replacing: what the form did not send is what somebody unticked. */
  @Test
  void settingLabelsReplacesTheWholeSet() throws Exception {
    setTags("[\"vegano\",\"fresco\"]");

    setTags("[\"vegano\"]");

    mockMvc
        .perform(get(FOOD_TAGS).with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value("vegano"));
  }

  /** Unticking the last checkbox has to be possible. */
  @Test
  void anEmptyListClearsThem() throws Exception {
    setTags("[\"vegano\"]");

    setTags("[]");

    mockMvc
        .perform(get(FOOD_TAGS).with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(jsonPath("$").isEmpty());
  }

  /**
   * A label nobody defined is a typo, not a request to invent one — and the valid half of the
   * request is not applied either, so a caller never has to guess what landed.
   */
  @Test
  void refusesAnUnknownLabelWithoutApplyingTheRest() throws Exception {
    setTags("[\"vegano\"]");

    mockMvc
        .perform(
            put(FOOD_TAGS)
                .with(asAdmin(SOMEONE, "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tagIds\":[\"fresco\",\"inventado\"]}"))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(get(FOOD_TAGS).with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value("vegano"));
  }

  @Test
  void refusesToLabelAFoodThatIsNotInTheCatalog() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/foods/unicornio/tags")
                .with(asAdmin(SOMEONE, "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tagIds\":[\"vegano\"]}"))
        .andExpect(status().isBadRequest());
  }

  private void setTags(String tagIdsJson) throws Exception {
    mockMvc
        .perform(
            put(FOOD_TAGS)
                .with(asAdmin(SOMEONE, "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tagIds\":" + tagIdsJson + "}"))
        .andExpect(status().isOk());
  }
}
