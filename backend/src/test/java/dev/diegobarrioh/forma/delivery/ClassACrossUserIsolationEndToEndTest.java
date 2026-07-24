package dev.diegobarrioh.forma.delivery;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.diegobarrioh.forma.support.AuthTestSupport;
import java.time.LocalDate;
import java.util.List;
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
 * FOR-145b-1 (ADR-012): cross-user isolation proof for the 5 Class-A domains wired to {@code
 * CurrentUserProvider} in this slice (goal, meal_log_entry, water_intake_entry, progress_photo,
 * weekly_tracking_record — progress_photo already has a dedicated {@code
 * ProgressPhotoEndToEndTest}, not duplicated here). Two REAL, FK-valid accounts (required since
 * migration V27 added {@code user_id UUID} FK-referencing {@code users(id)}) authenticate over the
 * SAME real Spring Security filter chain (no service mocking) via {@link
 * AuthTestSupport#asUser(UUID, String)}, proving the wiring end-to-end, not just at the repository
 * layer.
 *
 * <p><b>404, never 403, no existence leak</b> for domains with a by-id/by-key read (goal PATCH,
 * weekly-tracking GET-by-week) — spec FOR-145 "Cross-user isolation — 11 existing domains". {@code
 * meal_log_entry}/{@code water_intake_entry} have no by-id read endpoint (append-only, date-scoped
 * read models only), so their isolation proof is instead "user B's read model never reflects user
 * A's data" (a 200 with zeroed/absent totals, not a 404 — there is nothing to 404 on).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClassACrossUserIsolationEndToEndTest {

  private static final UUID USER_A = UUID.randomUUID();
  private static final UUID USER_B = UUID.randomUUID();

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void seedUsersAndClearTables() {
    jdbcTemplate.update("DELETE FROM goal_milestone");
    jdbcTemplate.update("DELETE FROM goal");
    jdbcTemplate.update("DELETE FROM meal_log_entry");
    jdbcTemplate.update("DELETE FROM water_intake_entry");
    jdbcTemplate.update("DELETE FROM weekly_tracking_record");
    jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", USER_A, USER_B);
    jdbcTemplate.update(
        "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)",
        USER_A,
        "isolation-user-a@test.local",
        "!");
    jdbcTemplate.update(
        "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)",
        USER_B,
        "isolation-user-b@test.local",
        "!");
  }

  /**
   * Leaves no live Class-A rows referencing {@code USER_A}/{@code USER_B} after the last test in
   * this class runs (ADR-007 shared named in-memory H2 across the whole test run) — otherwise a
   * later test class that blanket-deletes non-placeholder {@code users} rows (e.g. {@code
   * AuthenticationFlowIntegrationTest#clearTestUsers}) would hit an FK violation.
   */
  @AfterEach
  void cleanUp() {
    jdbcTemplate.update("DELETE FROM goal_milestone");
    jdbcTemplate.update("DELETE FROM goal");
    jdbcTemplate.update("DELETE FROM meal_log_entry");
    jdbcTemplate.update("DELETE FROM water_intake_entry");
    jdbcTemplate.update("DELETE FROM weekly_tracking_record");
    jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", USER_A, USER_B);
  }

  @Test
  void userBCannotUpdateUserAsGoalAndNeverSeesItInTheirList() throws Exception {
    String createJson =
        mockMvc
            .perform(
                post("/api/v1/goals")
                    .with(AuthTestSupport.asUser(USER_A, "isolation-user-a@test.local"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"title":"Meta de A","metric":"WEIGHT_KG","target":70.0}
                        """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String goalId = objectMapper.readTree(createJson).get("id").asText();

    // User B: the goal is invisible in the list...
    mockMvc
        .perform(
            get("/api/v1/goals")
                .with(AuthTestSupport.asUser(USER_B, "isolation-user-b@test.local"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.goals").isEmpty());

    // ...and a direct PATCH by id is a 404 (never 403 -- no existence leak), not a 200.
    mockMvc
        .perform(
            patch("/api/v1/goals/" + goalId)
                .with(AuthTestSupport.asUser(USER_B, "isolation-user-b@test.local"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Hijacked\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));

    // User A can still update their own goal (sanity: the fix didn't lock out the real owner).
    mockMvc
        .perform(
            patch("/api/v1/goals/" + goalId)
                .with(AuthTestSupport.asUser(USER_A, "isolation-user-a@test.local"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Meta de A actualizada\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Meta de A actualizada"));
  }

  @Test
  void userBCannotReadUserAsWeeklyTrackingRecordByWeek() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/tracking/weekly")
                .with(AuthTestSupport.asUser(USER_A, "isolation-user-a@test.local"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"week\":1,\"date\":\"2026-07-06\",\"weightKg\":73.6}"))
        .andExpect(status().isOk());

    // User A can read it back.
    mockMvc
        .perform(
            get("/api/v1/tracking/weekly/1")
                .with(AuthTestSupport.asUser(USER_A, "isolation-user-a@test.local"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.week").value(1));

    // User B gets 404 for the SAME week number -- never 403, no existence leak, and never A's data.
    mockMvc
        .perform(
            get("/api/v1/tracking/weekly/1")
                .with(AuthTestSupport.asUser(USER_B, "isolation-user-b@test.local"))
                .with(csrf()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));

    // User B's own list stays empty -- A's record never leaks into it.
    mockMvc
        .perform(
            get("/api/v1/tracking/weekly")
                .with(AuthTestSupport.asUser(USER_B, "isolation-user-b@test.local"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void userBsMealLogConsumptionNeverReflectsUserAsLoggedEntries() throws Exception {
    LocalDate day = LocalDate.of(2026, 7, 15);
    mockMvc
        .perform(
            post("/api/v1/nutrition/log")
                .with(AuthTestSupport.asUser(USER_A, "isolation-user-a@test.local"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"date":"2026-07-15","mealType":"LUNCH","name":"Comida de A",
                     "kcal":500,"proteinG":30.0,"carbsG":50.0,"fatG":10.0}
                    """))
        .andExpect(status().isCreated());

    // User A sees their own entry.
    mockMvc
        .perform(
            get("/api/v1/nutrition/consumption")
                .param("date", day.toString())
                .with(AuthTestSupport.asUser(USER_A, "isolation-user-a@test.local"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.consumed.kcal").value(500));

    // User B's SAME-DAY read model is completely unaffected by A's entry -- no by-id endpoint
    // exists here (append-only log), so the isolation proof is a zeroed 200, not a 404.
    mockMvc
        .perform(
            get("/api/v1/nutrition/consumption")
                .param("date", day.toString())
                .with(AuthTestSupport.asUser(USER_B, "isolation-user-b@test.local"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.consumed.kcal").value(0))
        .andExpect(jsonPath("$.entries").isEmpty());
  }

  @Test
  void userBsHydrationProgressNeverReflectsUserAsLoggedWaterIntake() throws Exception {
    LocalDate day = LocalDate.of(2026, 7, 15);
    mockMvc
        .perform(
            post("/api/v1/nutrition/hydration")
                .with(AuthTestSupport.asUser(USER_A, "isolation-user-a@test.local"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2026-07-15\",\"volumeMl\":750.0}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            get("/api/v1/nutrition/hydration")
                .param("date", day.toString())
                .with(AuthTestSupport.asUser(USER_A, "isolation-user-a@test.local"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalMl").value(750.0));

    // User B never sees A's logged volume -- a zeroed 200, no by-id endpoint to 404 on.
    mockMvc
        .perform(
            get("/api/v1/nutrition/hydration")
                .param("date", day.toString())
                .with(AuthTestSupport.asUser(USER_B, "isolation-user-b@test.local"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalMl").value(0));
  }

  /**
   * Mandatory security review of 145b-1 (HIGH cross-account disclosure, fixed same slice): {@code
   * AchievementService}/{@code AdherenceService}/{@code StreakService}/{@code WeeklyHistoryService}
   * were deferred to 145b-2 and still read only the legacy placeholder owner's data via a hardcoded
   * shim. Since {@code POST /api/v1/auth/register} is public, any self-registered real user could
   * call these 4 read endpoints and receive the placeholder account's private health data. Proven
   * here end to end over the real Spring Security filter chain (real beans, no mocking): a
   * non-placeholder authenticated caller gets 404 on all four, while the placeholder account's
   * behavior is unchanged (200, as before this slice).
   */
  @Test
  void userBCannotReadTheLegacyPlaceholderOwnersDeferredProgressEndpoints() throws Exception {
    for (String path :
        List.of(
            "/api/v1/progress/achievements",
            "/api/v1/progress/streak",
            "/api/v1/progress/adherence",
            "/api/v1/progress/weekly-history")) {
      mockMvc
          .perform(
              get(path)
                  .with(AuthTestSupport.asUser(USER_B, "isolation-user-b@test.local"))
                  .with(csrf()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("NOT_FOUND"));

      // The legacy placeholder account is unaffected -- unchanged 200 behavior.
      mockMvc
          .perform(get(path).with(AuthTestSupport.asPlaceholderUser()).with(csrf()))
          .andExpect(status().isOk());
    }
  }
}
