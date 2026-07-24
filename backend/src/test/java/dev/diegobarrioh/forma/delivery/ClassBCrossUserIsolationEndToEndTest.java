package dev.diegobarrioh.forma.delivery;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.support.AuthTestSupport;
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
 * FOR-145b-2 (ADR-012): cross-user isolation proof for the "Class B" domains wired to real per-user
 * scoping (via {@code CurrentUserProvider}) and de-guarded (the 145b-1 interim {@code
 * requireLegacyOwner()} guards removed) in this slice: {@code user_profile}, {@code integrations}
 * (connection status), {@code achievements}, {@code streak}, {@code adherence} and {@code
 * weekly-history}. Mirrors {@link ClassACrossUserIsolationEndToEndTest}'s pattern: two REAL,
 * FK-valid accounts authenticate over the SAME real Spring Security filter chain (no service
 * mocking) via {@link AuthTestSupport#asUser(UUID, String)}.
 *
 * <p>Each test asserts user B always gets THEIR OWN (empty/default) data — never user A's, and
 * never the legacy placeholder's — after user A writes real data. Streak/weekly-history/adherence
 * (NUTRITION category) are proven via the already-scoped {@code meal_log_entry} table (Class A,
 * 145b-1); achievements are proven via a directly-seeded {@code earned_achievement} row (bypassing
 * {@code AchievementService#evaluate()}, whose {@code body_measurements}-backed rules are a
 * documented 145c gap — see {@code AchievementService} javadoc — so this test does not rely on
 * their evaluation being isolated, only on the per-user PK read/write of the achievement table
 * itself).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClassBCrossUserIsolationEndToEndTest {

  private static final UUID USER_A = UUID.randomUUID();
  private static final UUID USER_B = UUID.randomUUID();

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void seedUsersAndClearTables() {
    clearClassBRows();
    jdbcTemplate.update("DELETE FROM meal_log_entry WHERE user_id IN (?, ?)", USER_A, USER_B);
    jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", USER_A, USER_B);
    jdbcTemplate.update(
        "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)",
        USER_A,
        "classb-isolation-user-a@test.local",
        "!");
    jdbcTemplate.update(
        "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)",
        USER_B,
        "classb-isolation-user-b@test.local",
        "!");
  }

  /**
   * Leaves no live Class-B/meal_log_entry rows referencing {@code USER_A}/{@code USER_B} after the
   * last test in this class runs (ADR-007 shared named in-memory H2 across the whole test run).
   */
  @AfterEach
  void cleanUp() {
    clearClassBRows();
    jdbcTemplate.update("DELETE FROM meal_log_entry WHERE user_id IN (?, ?)", USER_A, USER_B);
    jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", USER_A, USER_B);
  }

  private void clearClassBRows() {
    jdbcTemplate.update("DELETE FROM user_profile WHERE user_id IN (?, ?)", USER_A, USER_B);
    jdbcTemplate.update(
        "DELETE FROM integration_connection WHERE user_id IN (?, ?)", USER_A, USER_B);
    jdbcTemplate.update("DELETE FROM earned_achievement WHERE user_id IN (?, ?)", USER_A, USER_B);
  }

  @Test
  void userBsProfileNeverReflectsUserAsSavedName() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/profile")
                .with(AuthTestSupport.asUser(USER_A, "classb-isolation-user-a@test.local"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Perfil de A\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Perfil de A"));

    // User A reads their own saved name back.
    mockMvc
        .perform(
            get("/api/v1/profile")
                .with(AuthTestSupport.asUser(USER_A, "classb-isolation-user-a@test.local"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Perfil de A"));

    // User B never saved a profile: sane defaults, never A's name.
    mockMvc
        .perform(
            get("/api/v1/profile")
                .with(AuthTestSupport.asUser(USER_B, "classb-isolation-user-b@test.local"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").doesNotExist())
        .andExpect(jsonPath("$.themeMode").value("DARK"));
  }

  @Test
  void userBsIntegrationStatusNeverReflectsUserAsConnection() throws Exception {
    // GOOGLE_FIT has no registered OAuth gateway -- FOR-126 mock immediate-connect (no code/state
    // exchange needed), so this proves the isolation without a real Withings round trip.
    mockMvc
        .perform(
            post("/api/v1/integrations/google_fit/connect")
                .with(AuthTestSupport.asUser(USER_A, "classb-isolation-user-a@test.local"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONNECTED"));

    // User A sees their own connection as CONNECTED.
    mockMvc
        .perform(
            get("/api/v1/integrations")
                .with(AuthTestSupport.asUser(USER_A, "classb-isolation-user-a@test.local"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.providers[?(@.provider=='GOOGLE_FIT')].status").value("CONNECTED"));

    // User B never connected GOOGLE_FIT: still DISCONNECTED, never A's CONNECTED status.
    mockMvc
        .perform(
            get("/api/v1/integrations")
                .with(AuthTestSupport.asUser(USER_B, "classb-isolation-user-b@test.local"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.providers[?(@.provider=='GOOGLE_FIT')].status").value("DISCONNECTED"));
  }

  @Test
  void userBsEarnedAchievementsNeverIncludeUserAsDirectlySeededAward() throws Exception {
    // FIRST_GOAL_CREATED is picked deliberately over a measurement-based achievement: its rule is
    // backed by GoalRepository (Class A, properly user_id-scoped since 145b-1), unlike the
    // measurement-based rules whose backing body_measurements table is a documented, still-global
    // 145c gap (see AchievementService javadoc) -- seeding neither user with a goal here keeps this
    // assertion deterministic regardless of that gap or other tests' shared-database state
    // (ADR-007).
    jdbcTemplate.update(
        "INSERT INTO earned_achievement (user_id, achievement_id, earned_at) VALUES (?, ?, ?)",
        USER_A,
        "FIRST_GOAL_CREATED",
        Instant.now());

    // User A sees their own earned achievement.
    mockMvc
        .perform(
            get("/api/v1/progress/achievements")
                .with(AuthTestSupport.asUser(USER_A, "classb-isolation-user-a@test.local"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.earned[?(@.id=='FIRST_GOAL_CREATED')]").exists());

    // User B never earned it themselves (a row keyed to USER_A's PK can never surface here, and
    // B created no goal): it must be absent from B's earned list, never A's.
    mockMvc
        .perform(
            get("/api/v1/progress/achievements")
                .with(AuthTestSupport.asUser(USER_B, "classb-isolation-user-b@test.local"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.earned[?(@.id=='FIRST_GOAL_CREATED')]").doesNotExist());
  }

  @Test
  void userBsStreakNeverReflectsUserAsLoggedMealDays() throws Exception {
    logMealFor(USER_A, "classb-isolation-user-a@test.local", LocalDate.now());

    // User A's streak reflects today's logged meal.
    mockMvc
        .perform(
            get("/api/v1/progress/streak")
                .with(AuthTestSupport.asUser(USER_A, "classb-isolation-user-a@test.local"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentStreakDays").value(1));

    // User B never logged a meal: a zero streak, never A's.
    mockMvc
        .perform(
            get("/api/v1/progress/streak")
                .with(AuthTestSupport.asUser(USER_B, "classb-isolation-user-b@test.local"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentStreakDays").value(0));
  }

  @Test
  void userBsAdherenceAndWeeklyHistoryNeverReflectUserAsLoggedMealDays() throws Exception {
    logMealFor(USER_A, "classb-isolation-user-a@test.local", LocalDate.now());

    // User A's 1-day nutrition adherence shows the logged day as completed.
    mockMvc
        .perform(
            get("/api/v1/progress/adherence")
                .param("days", "1")
                .with(AuthTestSupport.asUser(USER_A, "classb-isolation-user-a@test.local"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categories[?(@.category=='NUTRITION')].completed").value(1));

    // User B's same-window nutrition adherence is unaffected -- zero completed, never A's. TRAINING
    // is asserted at zero too: FOR-145b-2's post-review security fix means a real, non-placeholder
    // caller never has TRAINING computed from the still-unscoped global training_session_status
    // table (145c gap) — see AdherenceService's INTERIM security guard javadoc — regardless of that
    // table's global state.
    mockMvc
        .perform(
            get("/api/v1/progress/adherence")
                .param("days", "1")
                .with(AuthTestSupport.asUser(USER_B, "classb-isolation-user-b@test.local"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categories[?(@.category=='NUTRITION')].completed").value(0))
        .andExpect(jsonPath("$.categories[?(@.category=='TRAINING')].planned").value(0))
        .andExpect(jsonPath("$.categories[?(@.category=='TRAINING')].completed").value(0));

    // User A's current-week bucket in weekly-history shows the logged day as completed.
    mockMvc
        .perform(
            get("/api/v1/progress/weekly-history")
                .param("weeks", "1")
                .with(AuthTestSupport.asUser(USER_A, "classb-isolation-user-a@test.local"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.weeks[0].completed").value(1));

    // User B's current-week bucket stays at zero completed days -- never A's.
    mockMvc
        .perform(
            get("/api/v1/progress/weekly-history")
                .param("weeks", "1")
                .with(AuthTestSupport.asUser(USER_B, "classb-isolation-user-b@test.local"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.weeks[0].completed").value(0));
  }

  private void logMealFor(UUID userId, String email, LocalDate date) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/nutrition/log")
                .with(AuthTestSupport.asUser(userId, email))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"date\":\""
                        + date
                        + "\",\"mealType\":\"LUNCH\",\"name\":\"Comida\",\"kcal\":500,"
                        + "\"proteinG\":30.0,\"carbsG\":50.0,\"fatG\":10.0}"))
        .andExpect(status().isCreated());
  }
}
