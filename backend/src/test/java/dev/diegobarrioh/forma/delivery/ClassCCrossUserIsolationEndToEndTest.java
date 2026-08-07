package dev.diegobarrioh.forma.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.diegobarrioh.forma.support.AuthTestSupport;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * FOR-145c (ADR-012): cross-user isolation proof for the 5 "gap table" domains scoped in this slice
 * — {@code body_measurements} (V30), {@code training_session_status} (V31), {@code
 * shopping_products} (V32), {@code shopping_lists}/{@code shopping_list_items} (V33), {@code
 * insight_history}/{@code insight_history_recommendation} (V34) — plus a deterministic proof that
 * removing the 145b-2 INTERIM security guard on {@link
 * dev.diegobarrioh.forma.application.AdherenceService}/{@link
 * dev.diegobarrioh.forma.application.AchievementService} is safe: a real, non-placeholder caller's
 * MEASUREMENTS-based adherence and achievements now reflect ONLY their own {@code
 * body_measurements} rows, never another account's.
 *
 * <p>Mirrors {@link ClassACrossUserIsolationEndToEndTest}/{@link
 * ClassBCrossUserIsolationEndToEndTest}'s pattern: two REAL, FK-valid accounts authenticate over
 * the SAME real Spring Security filter chain (no service mocking) via {@link
 * AuthTestSupport#asUser(UUID, String)}. {@code shopping_products}/{@code shopping_lists} have no
 * runtime "create a list" endpoint (FOR-37's own spec deferred algorithmic generation, see {@code
 * ShoppingListService} javadoc), so that fixture is seeded directly via JDBC, matching the FOR-5
 * migration's own seeding style — the isolation proof itself still runs entirely over the real HTTP
 * endpoints.
 *
 * <p>TRAINING adherence/achievements are deliberately NOT asserted via a real-clock-dependent
 * {@code /progress/adherence} call here (planned counts depend on which real weekday the suite
 * happens to run on, see {@code ClassBCrossUserIsolationEndToEndTest}'s fixed comment) — {@link
 * #userBsTrainingSessionStatusNeverReflectsOrOverwritesUserAsCompletion()} instead proves TRAINING
 * isolation deterministically at the {@code training_session_status} table level, which is exactly
 * what migration V31's composite-PK fix targets.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClassCCrossUserIsolationEndToEndTest {

  private static final UUID USER_A = UUID.randomUUID();
  private static final UUID USER_B = UUID.randomUUID();
  private static final String EMAIL_A = "classc-isolation-user-a@test.local";
  private static final String EMAIL_B = "classc-isolation-user-b@test.local";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void seedUsersAndClearTables() {
    clearClassCRows();
    jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", USER_A, USER_B);
    jdbcTemplate.update(
        "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)", USER_A, EMAIL_A, "!");
    jdbcTemplate.update(
        "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)", USER_B, EMAIL_B, "!");
  }

  /**
   * Leaves no live Class-C rows referencing {@code USER_A}/{@code USER_B} after the last test in
   * this class runs (ADR-007 shared named in-memory H2 across the whole test run).
   */
  @AfterEach
  void cleanUp() {
    clearClassCRows();
    jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", USER_A, USER_B);
  }

  private void clearClassCRows() {
    jdbcTemplate.update("DELETE FROM body_measurements WHERE user_id IN (?, ?)", USER_A, USER_B);
    jdbcTemplate.update(
        "DELETE FROM training_session_status WHERE user_id IN (?, ?)", USER_A, USER_B);
    jdbcTemplate.update(
        "DELETE FROM shopping_list_items WHERE shopping_list_id IN"
            + " (SELECT id FROM shopping_lists WHERE user_id IN (?, ?))",
        USER_A,
        USER_B);
    jdbcTemplate.update("DELETE FROM shopping_lists WHERE user_id IN (?, ?)", USER_A, USER_B);
    jdbcTemplate.update("DELETE FROM shopping_products WHERE user_id IN (?, ?)", USER_A, USER_B);
    jdbcTemplate.update(
        "DELETE FROM insight_history_recommendation WHERE user_id IN (?, ?)", USER_A, USER_B);
    jdbcTemplate.update("DELETE FROM insight_history WHERE user_id IN (?, ?)", USER_A, USER_B);
    jdbcTemplate.update("DELETE FROM earned_achievement WHERE user_id IN (?, ?)", USER_A, USER_B);
  }

  @Test
  void userBsBodyMeasurementsListNeverIncludesUserAsEntries() throws Exception {
    createMeasurement(USER_A, EMAIL_A, 80.0);

    mockMvc
        .perform(
            get("/api/v1/body/measurements")
                .with(AuthTestSupport.asUser(USER_A, EMAIL_A))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));

    // User B never recorded a measurement: an empty 200, never A's entry.
    mockMvc
        .perform(
            get("/api/v1/body/measurements")
                .with(AuthTestSupport.asUser(USER_B, EMAIL_B))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void userBCannotSeeOrMutateUserAsShoppingProduct() throws Exception {
    String createJson =
        mockMvc
            .perform(
                post("/api/v1/shopping/products")
                    .with(AuthTestSupport.asUser(USER_A, EMAIL_A))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Producto de A","estimatedPriceEur":3.50}
                        """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String productId = objectMapper.readTree(createJson).get("id").asText();

    // User B's own catalog never includes A's product.
    mockMvc
        .perform(
            get("/api/v1/shopping/products")
                .with(AuthTestSupport.asUser(USER_B, EMAIL_B))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

    // A direct PUT by id is a 404 for B -- never 403, no existence leak.
    mockMvc
        .perform(
            put("/api/v1/shopping/products/" + productId)
                .with(AuthTestSupport.asUser(USER_B, EMAIL_B))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Hijacked","estimatedPriceEur":1.00}
                    """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));

    // User A can still update their own product (sanity: the fix didn't lock out the real owner).
    mockMvc
        .perform(
            put("/api/v1/shopping/products/" + productId)
                .with(AuthTestSupport.asUser(USER_A, EMAIL_A))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Producto de A actualizado","estimatedPriceEur":4.00}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Producto de A actualizado"));
  }

  @Test
  void userBCannotSeeOrMutateUserAsShoppingList() throws Exception {
    UUID listId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    UUID productId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO shopping_products (id, user_id, name, estimated_price_eur)"
            + " VALUES (?, ?, ?, ?)",
        productId,
        USER_A,
        "Producto lista A",
        new BigDecimal("2.00"));
    jdbcTemplate.update(
        "INSERT INTO shopping_lists (id, user_id, week_start_date, status) VALUES (?, ?, ?, ?)",
        listId,
        USER_A,
        LocalDate.of(2026, 7, 20),
        "ACTIVE");
    jdbcTemplate.update(
        "INSERT INTO shopping_list_items"
            + " (id, shopping_list_id, product_id, quantity, estimated_cost_eur, checked)"
            + " VALUES (?, ?, ?, ?, ?, ?)",
        itemId,
        listId,
        productId.toString(),
        1,
        new BigDecimal("2.00"),
        false);

    // User A reads their own seeded list.
    mockMvc
        .perform(
            get("/api/v1/shopping/list").with(AuthTestSupport.asUser(USER_A, EMAIL_A)).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1));

    // User B has no list of their own, and gets an EMPTY one -- never A's data leaking as a
    // fabricated list. The status changed from 404 to 200 (an account without a list is an ordinary
    // state, not a missing resource); what this test guards did not: B's week comes back with zero
    // items, and none of them are A's.
    mockMvc
        .perform(
            get("/api/v1/shopping/list").with(AuthTestSupport.asUser(USER_B, EMAIL_B)).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0));

    // User B cannot toggle A's item by id -- 404, never mutates it.
    mockMvc
        .perform(
            patch("/api/v1/shopping/list/items/" + itemId + "/checked")
                .with(AuthTestSupport.asUser(USER_B, EMAIL_B))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"checked\":true}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));

    // A's item is unchanged by B's attempted mutation.
    Boolean checked =
        jdbcTemplate.queryForObject(
            "SELECT checked FROM shopping_list_items WHERE id = ?", Boolean.class, itemId);
    assertThat(checked).isFalse();

    // User A can still toggle their own item (sanity).
    mockMvc
        .perform(
            patch("/api/v1/shopping/list/items/" + itemId + "/checked")
                .with(AuthTestSupport.asUser(USER_A, EMAIL_A))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"checked\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.checked").value(true));
  }

  /**
   * FOR-145c migration V31's core purpose: {@code training_session_status}'s primary key was
   * rebuilt from a bare {@code session_id} (colliding across every account) to a composite {@code
   * (user_id, session_id)}. Proven directly at the table level (deterministic, no real-clock
   * dependency) rather than via the day-of-week-sensitive {@code /training/week} read model.
   */
  @Test
  void userBsTrainingSessionStatusNeverReflectsOrOverwritesUserAsCompletion() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/training/sessions/SATURDAY:RUNNING/status")
                .with(AuthTestSupport.asUser(USER_A, EMAIL_A))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\"}"))
        .andExpect(status().isOk());

    // User B marks the SAME session id as PLANNED (a no-op vs. the schedule's own default) -- under
    // the pre-V31 bare session_id PK this would have silently overwritten A's row.
    mockMvc
        .perform(
            patch("/api/v1/training/sessions/SATURDAY:RUNNING/status")
                .with(AuthTestSupport.asUser(USER_B, EMAIL_B))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PLANNED\"}"))
        .andExpect(status().isOk());

    String statusA =
        jdbcTemplate.queryForObject(
            "SELECT status FROM training_session_status WHERE user_id = ? AND session_id = ?",
            String.class,
            USER_A,
            "SATURDAY:RUNNING");
    String statusB =
        jdbcTemplate.queryForObject(
            "SELECT status FROM training_session_status WHERE user_id = ? AND session_id = ?",
            String.class,
            USER_B,
            "SATURDAY:RUNNING");

    // Two independent rows -- B's write never touched A's.
    assertThat(statusA).isEqualTo("COMPLETED");
    assertThat(statusB).isEqualTo("PLANNED");
  }

  @Test
  void userBsInsightHistoryNeverReflectsUserAsGeneratedPeriod() throws Exception {
    // Generating the current week's insights persists a row (FOR-110, migration V34).
    mockMvc
        .perform(
            get("/api/v1/insights/weekly")
                .with(AuthTestSupport.asUser(USER_A, EMAIL_A))
                .with(csrf()))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/insights/history")
                .with(AuthTestSupport.asUser(USER_A, EMAIL_A))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));

    // User B's history stays empty -- A's generated period never leaks into it, and generating B's
    // own current-week insights (implicitly, via the GET below) never collides with A's row (V34's
    // composite (user_id, week_start_date) PK).
    mockMvc
        .perform(
            get("/api/v1/insights/history")
                .with(AuthTestSupport.asUser(USER_B, EMAIL_B))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  /**
   * FOR-145c removed the 145b-2 INTERIM security guard on {@code AdherenceService}/{@code
   * AchievementService} now that {@code body_measurements} (V30) carries {@code user_id}. This
   * proves the removal is safe: user B's MEASUREMENTS adherence and measurement-based achievements
   * reflect ONLY their own recorded measurement, never user A's (nor the seeded legacy
   * placeholder's, which this test never touches). A 1-day window keeps MEASUREMENTS deterministic
   * regardless of the real weekday the suite runs on (unlike TRAINING, see class javadoc).
   */
  @Test
  void userBsMeasurementAdherenceAndAchievementsReflectOnlyTheirOwnDataNeverUserAsOrThePlaceholder()
      throws Exception {
    createMeasurement(USER_A, EMAIL_A, 80.0);

    // User B never recorded a measurement yet -- MEASUREMENTS is real per-user data, not zeroed by
    // an INTERIM guard, but also not A's: zero completed.
    mockMvc
        .perform(
            get("/api/v1/progress/adherence")
                .param("days", "1")
                .with(AuthTestSupport.asUser(USER_B, EMAIL_B))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categories[?(@.category=='MEASUREMENTS')].completed").value(0));
    mockMvc
        .perform(
            get("/api/v1/progress/achievements")
                .with(AuthTestSupport.asUser(USER_B, EMAIL_B))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.earned[?(@.id=='FIRST_MEASUREMENT')]").doesNotExist());

    // User B now records their OWN measurement.
    createMeasurement(USER_B, EMAIL_B, 65.0);

    // MEASUREMENTS now reflects exactly B's own single entry -- not A's (seeded above).
    mockMvc
        .perform(
            get("/api/v1/progress/adherence")
                .param("days", "1")
                .with(AuthTestSupport.asUser(USER_B, EMAIL_B))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categories[?(@.category=='MEASUREMENTS')].completed").value(1));
    mockMvc
        .perform(
            get("/api/v1/progress/achievements")
                .with(AuthTestSupport.asUser(USER_B, EMAIL_B))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.earned[?(@.id=='FIRST_MEASUREMENT')]").exists());

    // User A's own MEASUREMENTS/achievements are unaffected by B's later write.
    mockMvc
        .perform(
            get("/api/v1/progress/adherence")
                .param("days", "1")
                .with(AuthTestSupport.asUser(USER_A, EMAIL_A))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categories[?(@.category=='MEASUREMENTS')].completed").value(1));
  }

  /**
   * {@code measuredAt} is stamped to "now" (not a fixed past date) so {@link
   * #userBsMeasurementAdherenceAndAchievementsReflectOnlyTheirOwnDataNeverUserAsOrThePlaceholder()}'s
   * 1-day adherence window (real system clock, {@code [today, today]}) always includes it.
   */
  private void createMeasurement(UUID userId, String email, double weightKg) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/body/measurements")
                .with(AuthTestSupport.asUser(userId, email))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"measuredAt\":\""
                        + Instant.now()
                        + "\",\"weightKg\":"
                        + weightKg
                        + ",\"bodyFatPercentage\":20.0,\"bmi\":24.0}"))
        .andExpect(status().isCreated());
  }
}
