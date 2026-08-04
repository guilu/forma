package dev.diegobarrioh.forma.delivery.recipe;

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
import org.springframework.transaction.annotation.Transactional;

/**
 * Recipes end to end (V52): who may write, and that the macros come out of the catalog rather than
 * out of the request.
 *
 * <p>A full context rather than a web slice, for the same reason as {@code FoodCatalogAdminTest}:
 * one of the rules under test is the security filter chain, which a mocked slice would skip.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RecipeAdminTest {

  private static final String PATH = "/api/v1/recipes";
  private static final UUID SOMEONE = UUID.randomUUID();

  /**
   * 60 g of oats at 370 kcal/100 g plus 200 g of skimmed milk at 35 gives 222 + 70 = 292, and
   * nothing in this body says so.
   */
  private static final String OVERNIGHT_OATS =
      """
      {"id":"avena-overnight","name":"Avena overnight","servings":1,
       "ingredients":[{"foodId":"oats","grams":60.0},{"foodId":"skim-milk","grams":200.0}]}
      """;

  @Autowired private MockMvc mockMvc;

  @Test
  void anOrdinaryUserCannotWriteARecipe() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .with(asUser(SOMEONE, "someone@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(OVERNIGHT_OATS))
        .andExpect(status().isForbidden());
  }

  @Test
  void anAdminWritesADishAndGetsItsMacrosBack() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .with(asAdmin(SOMEONE, "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(OVERNIGHT_OATS))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.total.calories").value(292))
        // One serving, so the whole thing and a portion are the same figure.
        .andExpect(jsonPath("$.perServing.calories").value(292))
        .andExpect(jsonPath("$.ingredients.length()").value(2));

    mockMvc
        .perform(get(PATH).with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Avena overnight"));
  }

  /** A stew for four read as a meal for one makes every per-serving figure wrong fourfold. */
  @Test
  void dividesTheWholeByHowManyPortionsItMakes() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .with(asAdmin(SOMEONE, "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"id":"guiso","name":"Guiso de arroz","servings":4,
                     "ingredients":[{"foodId":"rice","grams":400.0}]}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.total.calories").value(1440))
        .andExpect(jsonPath("$.perServing.calories").value(360));
  }

  /** A dish with nothing in it totals zero and means nothing. */
  @Test
  void refusesADishWithNoIngredients() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .with(asAdmin(SOMEONE, "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"vacia\",\"name\":\"Vacía\",\"servings\":1,\"ingredients\":[]}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void refusesAnIngredientThatIsNotInTheCatalog() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .with(asAdmin(SOMEONE, "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"id":"rara","name":"Rara","servings":1,
                     "ingredients":[{"foodId":"unicornio","grams":60.0}]}
                    """))
        .andExpect(status().isBadRequest());
  }

  /** Editing replaces the list: what the form leaves out is what somebody removed. */
  @Test
  void editingReplacesTheIngredientsRatherThanAddingToThem() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .with(asAdmin(SOMEONE, "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(OVERNIGHT_OATS))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            put(PATH + "/avena-overnight")
                .with(asAdmin(SOMEONE, "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"id":"avena-overnight","name":"Avena overnight","servings":1,
                     "ingredients":[{"foodId":"oats","grams":60.0}]}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ingredients.length()").value(1))
        .andExpect(jsonPath("$.total.calories").value(222));
  }

  @Test
  void removingADishTakesItsIngredientsWithIt() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .with(asAdmin(SOMEONE, "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(OVERNIGHT_OATS))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            delete(PATH + "/avena-overnight")
                .with(asAdmin(SOMEONE, "admin@forma.test"))
                .with(csrf()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get(PATH + "/avena-overnight").with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(status().isNotFound());
  }
}
