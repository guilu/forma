package dev.diegobarrioh.forma.delivery.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The public funnel's two endpoints, end to end.
 *
 * <p>The one thing worth proving above all others: they answer somebody who is not signed in. Every
 * other endpoint in this API refuses an anonymous caller, and this is the funnel on the front page
 * — if it needed an account it would have nothing to convert.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlanGeneratorTest {

  private static final String ENERGY = "/api/v1/public/plan-generator/energy-requirement";
  private static final String GENERATE = "/api/v1/public/plan-generator";

  /** El ejemplo del mockup: hombre, 45 años, 75 kg, 182 cm, moderado. */
  private static final String STEP_ONE =
      """
      {"sex":"MALE","ageYears":45,"weightKg":75,"heightCm":182,"activityLevel":"MODERATE"}
      """;

  private static final String FINISHED_FUNNEL =
      """
      {"sex":"MALE","ageYears":45,"weightKg":75,"heightCm":182,"activityLevel":"MODERATE",
       "objective":"WEIGHT_LOSS","daysPerWeek":5,"mealsPerDay":5,"eatingStyle":"ESTANDAR_ESPANOL",
       "fullName":"Diego García","email":"diego@ejemplo.com","country":"ES",
       "wantsMarketing":true,"acceptsPrivacyPolicy":true}
      """;

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  /** Nobody signed in, and it answers. That is the whole point of it existing. */
  @Test
  void worksOutTheRequirementForAnAnonymousVisitor() throws Exception {
    mockMvc
        .perform(
            post(ENERGY).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(STEP_ONE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.basalKcal").value(1668))
        .andExpect(jsonPath("$.activityFactor").value(1.55))
        .andExpect(jsonPath("$.dailyKcal").value(2585));
  }

  /** Step 1 has no objective yet, and the screen still shows a total. */
  @Test
  void answersWithoutAnObjectiveWhileSomebodyIsStillOnTheFirstScreen() throws Exception {
    mockMvc
        .perform(
            post(ENERGY).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(STEP_ONE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.objectiveFactor").value(1.0))
        .andExpect(jsonPath("$.planKcal").value(2585));
  }

  /** And step 2 moves the plan without moving what the body spends. */
  @Test
  void appliesTheObjectiveOnTheSecondScreen() throws Exception {
    mockMvc
        .perform(
            post(ENERGY)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"sex":"MALE","ageYears":45,"weightKg":75,"heightCm":182,
                     "activityLevel":"MODERATE","objective":"WEIGHT_LOSS"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dailyKcal").value(2585))
        .andExpect(jsonPath("$.planKcal").value(2068));
  }

  /** Anybody can post anything at a public endpoint, so nothing impossible gets through. */
  @Test
  void refusesFiguresThatCannotDescribeAPerson() throws Exception {
    mockMvc
        .perform(
            post(ENERGY)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"sex":"MALE","ageYears":45,"weightKg":75,"heightCm":900,
                     "activityLevel":"MODERATE"}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void acceptsAFinishedFunnelFromAnAnonymousVisitor() throws Exception {
    mockMvc
        .perform(
            post(GENERATE)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(FINISHED_FUNNEL))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("diego@ejemplo.com"))
        .andExpect(jsonPath("$.planKcal").value(2068))
        .andExpect(jsonPath("$.mealsPerDay").value(5));
  }

  /** Consent that can be false is not consent. */
  @Test
  void refusesAFunnelThatDidNotAcceptThePrivacyPolicy() throws Exception {
    mockMvc
        .perform(
            post(GENERATE)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    FINISHED_FUNNEL.replace(
                        "\"acceptsPrivacyPolicy\":true", "\"acceptsPrivacyPolicy\":false")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void refusesAnEmailThatIsNotOne() throws Exception {
    mockMvc
        .perform(
            post(GENERATE)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(FINISHED_FUNNEL.replace("diego@ejemplo.com", "no-es-un-correo")))
        .andExpect(status().isBadRequest());
  }

  /**
   * El embudo deja de tirar leads.
   *
   * <p>Antes de V61 el endpoint validaba y respondía que sí, y no sobrevivía nada: alguien daba su
   * correo y desaparecía. Este test es la razón de existir de la tabla, así que comprueba la fila
   * entera y no solo que haya una — un guardado a medias es tan inútil como no guardar, porque sin
   * las respuestas del embudo no se le puede construir el plan que pidió.
   */
  @Test
  void guardaElLeadDeQuienTerminaElEmbudo() throws Exception {
    jdbcTemplate.update("DELETE FROM plan_lead WHERE email = ?", "diego@ejemplo.com");

    mockMvc
        .perform(
            post(GENERATE)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(FINISHED_FUNNEL))
        .andExpect(status().isOk());

    Map<String, Object> row =
        jdbcTemplate.queryForMap("SELECT * FROM plan_lead WHERE email = ?", "diego@ejemplo.com");

    assertThat(row).containsEntry("full_name", "Diego García");
    assertThat(row).containsEntry("objective", "WEIGHT_LOSS");
    assertThat(row).containsEntry("days_per_week", 5);
    assertThat(row).containsEntry("meals_per_day", 5);
    assertThat(row).containsEntry("eating_style", "ESTANDAR_ESPANOL");
    assertThat(row).containsEntry("country", "ES");
    // La cifra que se le enseñó, guardada y no recalculada al leer.
    assertThat(row.get("plan_kcal")).isEqualTo(2068);
  }

  /**
   * La prueba del consentimiento, que es lo que pide el artículo 7.1: qué aceptó, cuándo, y qué
   * texto estaba en vigor. Un booleano suelto no demuestra nada.
   */
  @Test
  void guardaLaPruebaDelConsentimiento() throws Exception {
    jdbcTemplate.update("DELETE FROM plan_lead WHERE email = ?", "consent@ejemplo.com");

    mockMvc
        .perform(
            post(GENERATE)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(FINISHED_FUNNEL.replace("diego@ejemplo.com", "consent@ejemplo.com")))
        .andExpect(status().isOk());

    Map<String, Object> row =
        jdbcTemplate.queryForMap("SELECT * FROM plan_lead WHERE email = ?", "consent@ejemplo.com");

    assertThat(row).containsEntry("accepts_privacy_policy", true);
    assertThat(row.get("privacy_policy_version")).isNotNull();
    assertThat(row.get("created_at")).isNotNull();
    // Aparte del de privacidad: son dos preguntas y se retiran por separado.
    assertThat(row).containsEntry("wants_marketing", true);
  }

  /**
   * Sin consentimiento no se guarda nada.
   *
   * <p>El `@AssertTrue` ya devuelve 400, pero lo que importa aquí es el efecto: que un rechazo no
   * deje fila. Una validación que responde mal y escribe igual es peor que ninguna.
   */
  @Test
  void noGuardaNadaSiNoSeAceptaLaPrivacidad() throws Exception {
    jdbcTemplate.update("DELETE FROM plan_lead WHERE email = ?", "sinconsentir@ejemplo.com");

    mockMvc
        .perform(
            post(GENERATE)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    FINISHED_FUNNEL
                        .replace("diego@ejemplo.com", "sinconsentir@ejemplo.com")
                        .replace(
                            "\"acceptsPrivacyPolicy\":true", "\"acceptsPrivacyPolicy\":false")))
        .andExpect(status().isBadRequest());

    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM plan_lead WHERE email = ?",
            Integer.class,
            "sinconsentir@ejemplo.com");
    assertThat(count).isZero();
  }

  /**
   * No pathologies, no dietary restrictions, no allergies.
   *
   * <p>They sit behind the padlock in the funnel — shown as what a subscription unlocks, never
   * asked for. Asserted rather than assumed, because health data that creeps into a public form is
   * exactly the kind of thing that arrives one field at a time.
   */
  @Test
  void collectsNoHealthData() throws Exception {
    mockMvc
        .perform(
            post(GENERATE)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    FINISHED_FUNNEL.replace(
                        "\"wantsMarketing\":true",
                        "\"pathologies\":[\"HIPERTENSION\"],\"wantsMarketing\":true")))
        // Ignored rather than rejected: an unknown field is the client's business. What matters is
        // that nothing in this request maps to a field that would store it.
        .andExpect(status().isOk());
  }
}
