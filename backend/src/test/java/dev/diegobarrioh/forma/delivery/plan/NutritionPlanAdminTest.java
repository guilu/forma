package dev.diegobarrioh.forma.delivery.plan;

import static dev.diegobarrioh.forma.support.AuthTestSupport.asUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
 * Nutrition plans end to end (V53/V54): who may read and write, and that the macros come out of the
 * catalog rather than out of the request.
 *
 * <p>A full context rather than a web slice, for the same reason as {@code RecipeAdminTest}:
 * several of the rules under test are the security filter chain and the database's own constraints,
 * both of which a mocked slice would skip.
 *
 * <p>Note who the caller is throughout: an ordinary user, not an admin. A plan is somebody's own
 * diet, not shared reference data like the food catalog.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NutritionPlanAdminTest {

  private static final String PATH = "/api/v1/nutrition/plans";
  private static final UUID SOMEBODY = UUID.fromString("00000000-0000-0000-0000-000000000000");
  private static final UUID SOMEBODY_ELSE = UUID.randomUUID();

  /** 60 g of oats at 370 kcal/100 g is 222, and nothing in this body says so. */
  private static final String ONE_DAY =
      """
      {"name":"Mi semana","days":[
        {"weekNumber":1,"dayNumber":1,"dayType":"RUNNING","meals":[
          {"mealType":"BREAKFAST","name":"Desayuno","scheduledTime":"08:00:00",
           "items":[{"foodId":"oats","amount":60.0}]}]}]}
      """;

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void anOrdinaryUserWritesTheirOwnPlanAndGetsItsMacrosBack() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .with(asUser(SOMEBODY, "somebody@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(ONE_DAY))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Mi semana"))
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.days[0].meals[0].items[0].label").value("Copos de avena"))
        .andExpect(jsonPath("$.days[0].meals[0].items[0].grams").value(60.0))
        .andExpect(jsonPath("$.days[0].meals[0].items[0].totals.calories").value(222))
        .andExpect(jsonPath("$.days[0].totals.calories").value(222));
  }

  /** A plan is not shared reference data; the caller is the only reader of their own. */
  @Test
  void doesNotShowAPlanToAnotherAccount() throws Exception {
    UUID id = create(ONE_DAY);

    mockMvc
        .perform(get(PATH + "/" + id).with(asUser(SOMEBODY_ELSE, "otro@forma.test")))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get(PATH).with(asUser(SOMEBODY_ELSE, "otro@forma.test")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void refusesAnAnonymousCaller() throws Exception {
    mockMvc
        .perform(post(PATH).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(ONE_DAY))
        .andExpect(status().isUnauthorized());
  }

  /** The stored side comes back beside the worked-out side, or the editor could not put it back. */
  @Test
  void returnsWhatThePlanSaysAsWellAsWhatItComesTo() throws Exception {
    UUID id = create(ONE_DAY);

    mockMvc
        .perform(get(PATH + "/" + id).with(asUser(SOMEBODY, "somebody@forma.test")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.days[0].meals[0].items[0].foodId").value("oats"))
        .andExpect(jsonPath("$.days[0].meals[0].items[0].amount").value(60.0))
        .andExpect(jsonPath("$.days[0].meals[0].items[0].servingId").doesNotExist())
        .andExpect(jsonPath("$.days[0].meals[0].items[0].label").value("Copos de avena"));
  }

  /** A target nobody set stays null all the way to the client, so a form can show it empty. */
  @Test
  void keepsAnUnsetTargetNullRatherThanZero() throws Exception {
    UUID id = create(ONE_DAY);

    mockMvc
        .perform(get(PATH + "/" + id).with(asUser(SOMEBODY, "somebody@forma.test")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.days[0].targets.calories").doesNotExist())
        .andExpect(jsonPath("$.targets.kcalMin").doesNotExist());
  }

  @Test
  void replacesTheDaysOnUpdateRatherThanAddingToThem() throws Exception {
    UUID id =
        create(
            """
            {"name":"Dos días","days":[
              {"weekNumber":1,"dayNumber":1,"meals":[
                {"mealType":"BREAKFAST","name":"Desayuno","items":[{"foodId":"oats","amount":60.0}]}]},
              {"weekNumber":1,"dayNumber":2,"meals":[
                {"mealType":"LUNCH","name":"Comida","items":[{"foodId":"rice","amount":80.0}]}]}]}
            """);

    mockMvc
        .perform(
            put(PATH + "/" + id)
                .with(asUser(SOMEBODY, "somebody@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(ONE_DAY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.days.length()").value(1))
        .andExpect(jsonPath("$.name").value("Mi semana"));
  }

  /** Two plans somebody could both be following is a question with two answers. */
  @Test
  void activatingAPlanStandsDownTheOneBefore() throws Exception {
    UUID first = create(ONE_DAY);
    UUID second = create(ONE_DAY.replace("Mi semana", "Otra semana"));

    activate(first);
    mockMvc
        .perform(
            post(PATH + "/" + second + "/activation")
                .with(asUser(SOMEBODY, "s@forma.test"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.active").value(true));

    mockMvc
        .perform(get(PATH + "/" + first).with(asUser(SOMEBODY, "somebody@forma.test")))
        .andExpect(jsonPath("$.status").value("COMPLETED"));
  }

  @Test
  void refusesToActivateThroughThePlainStatusChange() throws Exception {
    UUID id = create(ONE_DAY);

    mockMvc
        .perform(
            put(PATH + "/" + id + "/status")
                .with(asUser(SOMEBODY, "somebody@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACTIVE\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void archivesAPlan() throws Exception {
    UUID id = create(ONE_DAY);

    mockMvc
        .perform(
            put(PATH + "/" + id + "/status")
                .with(asUser(SOMEBODY, "somebody@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ARCHIVED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ARCHIVED"))
        .andExpect(jsonPath("$.active").value(false));
  }

  @Test
  void refusesAnItemNamingAFoodTheCatalogDoesNotHave() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .with(asUser(SOMEBODY, "somebody@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(ONE_DAY.replace("\"oats\"", "\"unicornio\"")))
        .andExpect(status().isBadRequest());
  }

  /** A line is a food or a dish. Both is two things in one row; neither is nothing. */
  @Test
  void refusesALineThatIsNeitherAFoodNorADish() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .with(asUser(SOMEBODY, "somebody@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(ONE_DAY.replace("\"foodId\":\"oats\",", "")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void removesAPlanAndEverythingUnderIt() throws Exception {
    UUID id = create(ONE_DAY);

    mockMvc
        .perform(delete(PATH + "/" + id).with(asUser(SOMEBODY, "somebody@forma.test")).with(csrf()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get(PATH + "/" + id).with(asUser(SOMEBODY, "somebody@forma.test")))
        .andExpect(status().isNotFound());
  }

  /** And another account's plan survives the attempt. */
  @Test
  void refusesToDeleteAnotherAccountsPlan() throws Exception {
    UUID id = create(ONE_DAY);

    mockMvc
        .perform(
            delete(PATH + "/" + id).with(asUser(SOMEBODY_ELSE, "otro@forma.test")).with(csrf()))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(get(PATH + "/" + id).with(asUser(SOMEBODY, "somebody@forma.test")))
        .andExpect(status().isOk());
  }

  private UUID create(String body) throws Exception {
    String json =
        mockMvc
            .perform(
                post(PATH)
                    .with(asUser(SOMEBODY, "somebody@forma.test"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode node = objectMapper.readTree(json);
    return UUID.fromString(node.get("id").asText());
  }

  private void activate(UUID id) throws Exception {
    mockMvc
        .perform(
            post(PATH + "/" + id + "/activation")
                .with(asUser(SOMEBODY, "somebody@forma.test"))
                .with(csrf()))
        .andExpect(status().isOk());
  }
}
