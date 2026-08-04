package dev.diegobarrioh.forma.delivery.nutrition;

import static dev.diegobarrioh.forma.support.AuthTestSupport.asUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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
 * Logging a meal against the one the plan asked for (V55), end to end.
 *
 * <p>The point of the whole slice is here: the plan says what to eat, the log says what was eaten,
 * and the link between them lets the app answer "did I?" without either side storing a status.
 * Every state below — eaten, pending, skipped — is derived from the rows on each read.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PlannedMealLogTest {

  private static final String PLANS = "/api/v1/nutrition/plans";
  private static final String LOG = "/api/v1/nutrition/log";
  private static final String CONSUMPTION = "/api/v1/nutrition/consumption";

  private static final UUID SOMEBODY = UUID.fromString("00000000-0000-0000-0000-000000000000");
  private static final UUID SOMEBODY_ELSE = UUID.randomUUID();

  /** 2026-07-14 is a Tuesday, which the shared weekly policy classifies as a strength day. */
  private static final LocalDate A_PAST_TUESDAY = LocalDate.of(2026, 7, 14);

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void anEntryRemembersWhichPlannedMealItWas() throws Exception {
    String mealId = plannedStrengthMeal();

    mockMvc
        .perform(
            post(LOG)
                .with(asUser(SOMEBODY, "somebody@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"date":"2026-07-14","mealType":"LUNCH","foodItemId":"oats","portions":1,
                     "plannedMealId":"%s"}
                    """
                        .formatted(mealId)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            get(CONSUMPTION)
                .with(asUser(SOMEBODY, "somebody@forma.test"))
                .param("date", "2026-07-14"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.plannedMeals[0].id").value(mealId))
        .andExpect(jsonPath("$.plannedMeals[0].name").value("Comida"))
        .andExpect(jsonPath("$.plannedMeals[0].state").value("EATEN"));
  }

  /**
   * A planned meal nobody logged, on a day that has passed, was not eaten.
   *
   * <p>Derived from the clock and the absence of an entry, not from a column somebody would have
   * had to update at midnight.
   */
  @Test
  void aPlannedMealNobodyLoggedOnAPastDayReadsAsSkipped() throws Exception {
    plannedStrengthMeal();

    mockMvc
        .perform(
            get(CONSUMPTION)
                .with(asUser(SOMEBODY, "somebody@forma.test"))
                .param("date", "2026-07-14"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.plannedMeals[0].state").value("SKIPPED"));
  }

  /** An entry that answers no planned meal is the ordinary case and leaves the plan pending. */
  @Test
  void anUnplannedEntryLeavesThePlannedMealAlone() throws Exception {
    plannedStrengthMeal();

    mockMvc
        .perform(
            post(LOG)
                .with(asUser(SOMEBODY, "somebody@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"date":"2026-07-14","mealType":"SNACK","name":"Un puñado de almendras",
                     "kcal":180,"proteinG":6.0,"carbsG":6.0,"fatG":15.0}
                    """))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            get(CONSUMPTION)
                .with(asUser(SOMEBODY, "somebody@forma.test"))
                .param("date", "2026-07-14"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.consumed.kcal").value(180))
        .andExpect(jsonPath("$.plannedMeals[0].state").value("SKIPPED"));
  }

  /**
   * A planned meal of somebody else's is not the caller's to point at.
   *
   * <p>The foreign key would accept it: the database knows the row exists, not whose it is. Without
   * this check an entry could be attached to another account's plan and show up in their adherence.
   */
  @Test
  void refusesToLogAgainstAnotherAccountsPlannedMeal() throws Exception {
    String mealId = plannedStrengthMeal();

    mockMvc
        .perform(
            post(LOG)
                .with(asUser(SOMEBODY_ELSE, "otro@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"date":"2026-07-14","mealType":"LUNCH","foodItemId":"oats","portions":1,
                     "plannedMealId":"%s"}
                    """
                        .formatted(mealId)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void refusesToLogAgainstAPlannedMealThatDoesNotExist() throws Exception {
    mockMvc
        .perform(
            post(LOG)
                .with(asUser(SOMEBODY, "somebody@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"date":"2026-07-14","mealType":"LUNCH","foodItemId":"oats","portions":1,
                     "plannedMealId":"%s"}
                    """
                        .formatted(UUID.randomUUID())))
        .andExpect(status().isBadRequest());
  }

  /** No plan, nothing to be adherent to — and the day's own totals are still reported. */
  @Test
  void reportsNoPlannedMealsForAnAccountWithoutAPlan() throws Exception {
    mockMvc
        .perform(
            get(CONSUMPTION)
                .with(asUser(SOMEBODY_ELSE, "otro@forma.test"))
                .param("date", "2026-07-14"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.plannedMeals").isEmpty());
  }

  /** Creates a plan whose tuesday is a strength day with one meal, and returns that meal's id. */
  private String plannedStrengthMeal() throws Exception {
    String json =
        mockMvc
            .perform(
                post(PLANS)
                    .with(asUser(SOMEBODY, "somebody@forma.test"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Para el seguimiento","days":[
                          {"weekNumber":1,"dayNumber":2,"dayType":"STRENGTH","meals":[
                            {"mealType":"LUNCH","name":"Comida",
                             "items":[{"foodId":"oats","amount":60.0}]}]}]}
                        """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode plan = objectMapper.readTree(json);
    mockMvc
        .perform(
            post(PLANS + "/" + plan.get("id").asText() + "/activation")
                .with(asUser(SOMEBODY, "somebody@forma.test"))
                .with(csrf()))
        .andExpect(status().isOk());

    // The planned meal's id comes from the consumption read model rather than from the plan
    // response, which does not carry ids: a day is identified by where it falls and a meal by where
    // it sits, so the editor never needs one. The log does.
    String consumption =
        mockMvc
            .perform(
                get(CONSUMPTION)
                    .with(asUser(SOMEBODY, "somebody@forma.test"))
                    .param("date", A_PAST_TUESDAY.toString()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(consumption).get("plannedMeals").get(0).get("id").asText();
  }
}
