package dev.diegobarrioh.forma.delivery.food;

import static dev.diegobarrioh.forma.support.AuthTestSupport.asAdmin;
import static dev.diegobarrioh.forma.support.AuthTestSupport.asUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * A food's portions end to end (V49).
 *
 * <p>The rule worth exercising against a real database is the one enforced by a nullable sentinel
 * rather than by a partial index: promoting a portion has to demote the one that held the place,
 * and a fake repository can only pretend the unique index is there.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FoodServingAdminTest {

  private static final String PATH = "/api/v1/foods/banana/servings";
  private static final UUID SOMEONE = UUID.randomUUID();
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Autowired private MockMvc mockMvc;

  /** The migration gave every seeded food the portion it used to carry as a column. */
  @Test
  void aFoodStartsWithTheOnePortionItAlreadyHad() throws Exception {
    mockMvc
        .perform(get(PATH).with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].grams").value(120.0))
        .andExpect(jsonPath("$[0].isDefault").value(true))
        .andExpect(jsonPath("$[0].name").doesNotExist());
  }

  @Test
  void anOrdinaryUserCannotWriteAPortion() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .with(asUser(SOMEONE, "someone@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Grande\",\"grams\":150.0,\"isDefault\":false}"))
        .andExpect(status().isForbidden());
  }

  /**
   * The whole point of the table. Against a real database this also proves the unique index
   * tolerates several portions with no marker, which is what NULLS DISTINCT buys.
   */
  @Test
  void anAdminGivesAFoodSeveralSizes() throws Exception {
    addPortion("Pequeño", "90.0", false);
    addPortion("Grande", "150.0", false);

    mockMvc
        .perform(get(PATH).with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        // The default comes first whatever the names sort like.
        .andExpect(jsonPath("$[0].isDefault").value(true));
  }

  /**
   * Promoting has to demote, in that order. The other way round the database refuses it, so this
   * would be a 500 rather than a swap.
   */
  @Test
  void promotingAPortionDemotesTheOneThatHeldThePlace() throws Exception {
    String bigId = addPortion("Grande", "150.0", false);

    mockMvc
        .perform(
            put(PATH + "/" + bigId)
                .with(asAdmin(SOMEONE, "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Grande\",\"grams\":150.0,\"isDefault\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isDefault").value(true));

    mockMvc
        .perform(get(PATH).with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(jsonPath("$[?(@.isDefault==true)].name").value("Grande"))
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void refusesTwoPortionsOfOneFoodUnderTheSameName() throws Exception {
    addPortion("Grande", "150.0", false);

    mockMvc
        .perform(
            post(PATH)
                .with(asAdmin(SOMEONE, "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Grande\",\"grams\":160.0,\"isDefault\":false}"))
        .andExpect(status().isConflict());
  }

  @Test
  void refusesAPortionOfNothing() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .with(asAdmin(SOMEONE, "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Nada\",\"grams\":0,\"isDefault\":false}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void refusesAPortionOfAFoodThatIsNotInTheCatalog() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/foods/unicornio/servings")
                .with(asAdmin(SOMEONE, "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ala\",\"grams\":100.0,\"isDefault\":false}"))
        .andExpect(status().isBadRequest());
  }

  private String addPortion(String name, String grams, boolean isDefault) throws Exception {
    String body =
        mockMvc
            .perform(
                post(PATH)
                    .with(asAdmin(SOMEONE, "admin@forma.test"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\""
                            + name
                            + "\",\"grams\":"
                            + grams
                            + ",\"isDefault\":"
                            + isDefault
                            + "}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode json = MAPPER.readTree(body);
    return json.get("id").asText();
  }
}
