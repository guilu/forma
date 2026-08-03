package dev.diegobarrioh.forma.delivery.food;

import static dev.diegobarrioh.forma.support.AuthTestSupport.asAdmin;
import static dev.diegobarrioh.forma.support.AuthTestSupport.asUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * The substitution endpoints end to end (V47): who may write, and that the grams really are worked
 * out from the catalog rather than echoed back from the request.
 *
 * <p>A full context rather than a web slice, for the same reason as {@code FoodCatalogAdminTest}:
 * one of the rules under test is the security filter chain, which a mocked slice would skip.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FoodEquivalenceAdminTest {

  private static final String PATH = "/api/v1/food-equivalences";
  private static final UUID SOMEONE = UUID.randomUUID();

  private static final String RICE_TO_POTATO =
      """
      {"sourceFoodId":"rice","targetFoodId":"potato","basis":"CARBS",
       "sourceReferenceG":100.0,"maxMacroDeviationPct":25.0,"notes":null}
      """;

  @Autowired private MockMvc mockMvc;

  @Test
  void anOrdinaryUserCannotStateASubstitution() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .with(asUser(SOMEONE, "someone@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(RICE_TO_POTATO))
        .andExpect(status().isForbidden());
  }

  /**
   * The grams come out of the catalog, not out of the request: 100 g of rice carries 79 g of
   * carbohydrate and potato carries 17 g per 100 g, so the answer is 464.7 g — and nothing in the
   * body said so.
   */
  @Test
  void anAdminStatesASubstitutionAndGetsTheGramsWorkedOut() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .with(asAdmin(SOMEONE, "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(RICE_TO_POTATO))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.targetName").value("Patata"))
        .andExpect(jsonPath("$.targetReferenceG").value(464.7))
        // The nutrient being held equal has no drift to report.
        .andExpect(jsonPath("$.carbsDeviationPct").doesNotExist())
        .andExpect(jsonPath("$.proteinDeviationPct").value(32.8))
        .andExpect(jsonPath("$.fatDeviationPct").value(-53.5))
        // Over the stated 25%, and stored anyway: the swap delivers the carbohydrate it promised.
        .andExpect(jsonPath("$.exceedsTolerance").value(true));

    mockMvc
        .perform(get(PATH + "/rice").with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].targetFoodId").value("potato"));
  }

  /** Advice in one direction is not advice in the other, and the API does not conjure it. */
  @Test
  void doesNotOfferTheOppositeDirection() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .with(asAdmin(SOMEONE, "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(RICE_TO_POTATO))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get(PATH + "/potato").with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }

  /** No amount of hake makes up the carbohydrate in rice, and the request says so plainly. */
  @Test
  void refusesASubstitutionThatCannotBeWorkedOut() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .with(asAdmin(SOMEONE, "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"sourceFoodId":"rice","targetFoodId":"fish","basis":"CARBS",
                     "sourceReferenceG":100.0}
                    """))
        .andExpect(status().isBadRequest());
  }

  /** An equivalence with no stated criterion is not an equivalence. */
  @Test
  void refusesASubstitutionWithNoGrounds() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .with(asAdmin(SOMEONE, "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"sourceFoodId":"rice","targetFoodId":"potato","sourceReferenceG":100.0}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void anOrdinaryUserCannotRemoveASubstitution() throws Exception {
    mockMvc
        .perform(
            delete(PATH + "/" + UUID.randomUUID())
                .with(asUser(SOMEONE, "someone@forma.test"))
                .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void removingSomethingNobodyStatedSaysSo() throws Exception {
    mockMvc
        .perform(
            delete(PATH + "/" + UUID.randomUUID())
                .with(asAdmin(SOMEONE, "admin@forma.test"))
                .with(csrf()))
        .andExpect(status().isNotFound());
  }
}
