package dev.diegobarrioh.forma.delivery.plan;

import static dev.diegobarrioh.forma.support.AuthTestSupport.asAdmin;
import static dev.diegobarrioh.forma.support.AuthTestSupport.asUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Importing a file of plans written elsewhere.
 *
 * <p>The point of the format is that a language model can write it, so the tests are about the two
 * things that decides: that a good file lands whole, and that a bad one comes back with EVERY fault
 * at once rather than the first — because one fault per attempt turns a five-mistake file into five
 * round trips.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PlanImportTest {

  private static final String IMPORT = "/api/v1/nutrition/plans/import";
  private static final String CATALOG = IMPORT + "/catalog";
  private static final String PLANS = "/api/v1/nutrition/plans";

  /** The legacy account, the one V26 seeded and every other migration writes for. */
  private static final UUID SOMEBODY = UUID.fromString("00000000-0000-0000-0000-000000000000");

  private static final String THEIR_EMAIL = "legacy@forma.local";

  /** Two accounts, one file — the shape the whole format exists for. */
  private static final String TWO_PLANS =
      """
      {"plans":[
        {"forUserEmail":"%s","plan":{
          "name":"Recomposición","objective":"COMPOSICION",
          "targets":{"kcalMin":2200,"kcalMax":2400},
          "generation":{"by":"AI","prompt":"Una semana de recomposición"},
          "days":[{"weekNumber":1,"dayNumber":1,"dayType":"RUNNING",
            "targets":{"calories":2320,"proteinG":165},
            "meals":[{"mealType":"BREAKFAST","name":"Desayuno",
              "items":[{"foodId":"oats","amount":60},
                       {"foodId":"banana","servingId":"banana","amount":1}]}]}]}},
        {"forUserEmail":"%s","plan":{
          "name":"Volumen",
          "days":[{"weekNumber":1,"dayNumber":2,
            "meals":[{"mealType":"LUNCH","name":"Comida",
              "items":[{"foodId":"rice","amount":80}]}]}]}}
      ]}
      """
          .formatted(THEIR_EMAIL, THEIR_EMAIL);

  @Autowired private MockMvc mockMvc;

  @Test
  void anAdminImportsAFileOfPlans() throws Exception {
    mockMvc
        .perform(
            post(IMPORT)
                .with(asAdmin(SOMEBODY, THEIR_EMAIL))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TWO_PLANS))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.imported.length()").value(2))
        .andExpect(jsonPath("$.imported[0].name").value("Recomposición"))
        .andExpect(jsonPath("$.imported[0].forUserEmail").value(THEIR_EMAIL));
  }

  /** Everything the file said survives the round trip, computed figures included. */
  @Test
  void whatWasImportedReadsBackAsAPlan() throws Exception {
    mockMvc
        .perform(
            post(IMPORT)
                .with(asAdmin(SOMEBODY, THEIR_EMAIL))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TWO_PLANS))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get(PLANS).with(asUser(SOMEBODY, THEIR_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$[?(@.name == 'Recomposición')].targets.kcalMin")
                .value(Matchers.hasItem(2200)));
  }

  /**
   * A file never activates anything.
   *
   * <p>Somebody's diet changing because a file was uploaded would be a surprising thing for an
   * import to do; the account activates its own from the plans screen.
   */
  @Test
  void importsEverythingAsADraft() throws Exception {
    mockMvc
        .perform(
            post(IMPORT)
                .with(asAdmin(SOMEBODY, THEIR_EMAIL))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TWO_PLANS))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get(PLANS).with(asUser(SOMEBODY, THEIR_EMAIL)))
        .andExpect(
            jsonPath("$[?(@.name == 'Recomposición')].status").value(Matchers.hasItem("DRAFT")))
        .andExpect(jsonPath("$[?(@.name == 'Volumen')].status").value(Matchers.hasItem("DRAFT")));
  }

  /** A file can carry plans for other people, which is what makes it an administrator's act. */
  @Test
  void refusesAnOrdinaryAccount() throws Exception {
    mockMvc
        .perform(
            post(IMPORT)
                .with(asUser(SOMEBODY, THEIR_EMAIL))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(TWO_PLANS))
        .andExpect(status().isForbidden());
  }

  /**
   * EVERY fault at once, not the first.
   *
   * <p>The file below is wrong three separate ways, in two different plans. All three come back
   * together, each with the path to the exact line, so whatever wrote the file can fix them in one
   * pass.
   */
  @Test
  void reportsEveryProblemInTheFileAtOnce() throws Exception {
    mockMvc
        .perform(
            post(IMPORT)
                .with(asAdmin(SOMEBODY, THEIR_EMAIL))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"plans":[
                      {"forUserEmail":"%s","plan":{"name":"Con erratas",
                        "days":[{"weekNumber":1,"dayNumber":1,
                          "meals":[{"mealType":"LUNCH","name":"Comida","items":[
                            {"foodId":"quinoa","amount":80},
                            {"foodId":"oats","servingId":"banana","amount":1}]}]}]}},
                      {"forUserEmail":"nadie@forma.test","plan":{"name":"Sin dueño",
                        "days":[{"weekNumber":1,"dayNumber":1,
                          "meals":[{"mealType":"LUNCH","name":"Comida",
                            "items":[{"foodId":"rice","amount":80}]}]}]}}
                    ]}
                    """
                        .formatted(THEIR_EMAIL)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details.length()").value(3))
        .andExpect(
            jsonPath("$.details[*].field")
                .value(Matchers.hasItem("plans[0].days[0].meals[0].items[0]")))
        .andExpect(
            jsonPath("$.details[*].message")
                .value(Matchers.hasItem("No existe el alimento: quinoa")))
        .andExpect(
            jsonPath("$.details[*].message")
                .value(Matchers.hasItem(Matchers.containsString("no es de oats"))))
        .andExpect(jsonPath("$.details[*].field").value(Matchers.hasItem("plans[1].forUserEmail")));
  }

  /** One bad line and nothing at all is written — not even the plans that were fine. */
  @Test
  void writesNothingWhenAnythingIsWrong() throws Exception {
    mockMvc
        .perform(
            post(IMPORT)
                .with(asAdmin(SOMEBODY, THEIR_EMAIL))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"plans":[
                      {"forUserEmail":"%s","plan":{"name":"Buena",
                        "days":[{"weekNumber":1,"dayNumber":1,
                          "meals":[{"mealType":"LUNCH","name":"Comida",
                            "items":[{"foodId":"rice","amount":80}]}]}]}},
                      {"forUserEmail":"%s","plan":{"name":"Mala",
                        "days":[{"weekNumber":1,"dayNumber":1,
                          "meals":[{"mealType":"LUNCH","name":"Comida",
                            "items":[{"foodId":"unicornio","amount":80}]}]}]}}
                    ]}
                    """
                        .formatted(THEIR_EMAIL, THEIR_EMAIL)))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(get(PLANS).with(asUser(SOMEBODY, THEIR_EMAIL)))
        .andExpect(jsonPath("$[?(@.name == 'Buena')]").isEmpty());
  }

  /** A line that is both a food and a dish is malformed, and says so with its path. */
  @Test
  void reportsALineThatIsNeitherAFoodNorARecipe() throws Exception {
    mockMvc
        .perform(
            post(IMPORT)
                .with(asAdmin(SOMEBODY, THEIR_EMAIL))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"plans":[{"forUserEmail":"%s","plan":{"name":"Rota",
                      "days":[{"weekNumber":1,"dayNumber":1,
                        "meals":[{"mealType":"LUNCH","name":"Comida",
                          "items":[{"amount":80}]}]}]}}]}
                    """
                        .formatted(THEIR_EMAIL)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0].field").value("plans[0]"));
  }

  @Test
  void refusesAFileWithNoPlansAtAll() throws Exception {
    mockMvc
        .perform(
            post(IMPORT)
                .with(asAdmin(SOMEBODY, THEIR_EMAIL))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"plans\":[]}"))
        .andExpect(status().isBadRequest());
  }

  // --- The vocabulary a file may use ---

  /**
   * Every food, with the two things a file needs to name it: its id and its portions' ids.
   *
   * <p>Without this a model writes plausible-looking ids and every one is rejected.
   */
  @Test
  void handsOutTheCatalogAFileMayName() throws Exception {
    mockMvc
        .perform(get(CATALOG).with(asUser(SOMEBODY, THEIR_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.foods[?(@.id == 'oats')].name").value(Matchers.hasItem("Copos de avena")))
        .andExpect(jsonPath("$.foods[?(@.id == 'oats')].per100g.kcal").value(Matchers.hasItem(370)))
        // Its portions, which a file names in `servingId`. The banana's is the 120 g V49
        // backfilled from the catalog, under the food's own id.
        .andExpect(
            jsonPath("$..servings[?(@.id == 'banana')].grams").value(Matchers.hasItem(120.0)))
        .andExpect(
            jsonPath("$..servings[?(@.id == 'banana')].isDefault").value(Matchers.hasItem(true)))
        // Nobody has classified these as raw or cooked yet (V51 deliberately backfilled nothing),
        // and the file says so rather than guessing.
        .andExpect(
            jsonPath("$.foods[?(@.id == 'oats')].preparation")
                .value(Matchers.hasItem((Object) null)));
  }

  /** Read from the database, so a food added from the admin screen appears without a redeploy. */
  @Test
  void handsOutEveryFoodTheCatalogHolds() throws Exception {
    mockMvc
        .perform(get(CATALOG).with(asUser(SOMEBODY, THEIR_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.foods.length()").value(Matchers.greaterThanOrEqualTo(23)));
  }

  @Test
  void refusesTheCatalogToAnAnonymousCaller() throws Exception {
    mockMvc.perform(get(CATALOG)).andExpect(status().isUnauthorized());
  }

  /**
   * THE EXAMPLE IN THE DOCUMENTATION IMPORTS.
   *
   * <p>Read out of {@code docs/FORMA_Formato_Plan_JSON.md} itself rather than copied here, because
   * a copy is a second home for one fact and this one would rot silently: the document exists to be
   * pasted into a prompt, and a documented example that no longer imports is worse than no example
   * at all — it teaches a model to write files the app rejects.
   *
   * <p>Only the email is substituted, since the document names a person who is not in this
   * database.
   */
  @Test
  void theExampleInTheDocumentationImports() throws Exception {
    String example = lastJsonBlockOf(Path.of("..", "docs", "FORMA_Formato_Plan_JSON.md"));

    mockMvc
        .perform(
            post(IMPORT)
                .with(asAdmin(SOMEBODY, THEIR_EMAIL))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(example.replace("diego@ejemplo.com", THEIR_EMAIL)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.imported.length()").value(1))
        .andExpect(jsonPath("$.imported[0].name").value("Dieta semanal — recomposición"));
  }

  /** The last fenced ```json block in a markdown file — the document's "ejemplo completo". */
  private static String lastJsonBlockOf(Path markdown) throws Exception {
    String text = Files.readString(markdown);
    int open = text.lastIndexOf("```json");
    int start = text.indexOf('\n', open) + 1;
    int end = text.indexOf("```", start);
    return text.substring(start, end);
  }
}
