package dev.diegobarrioh.forma.delivery.planrequest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.BodyMeasurementRepository;
import dev.diegobarrioh.forma.application.UserProfileRepository;
import dev.diegobarrioh.forma.domain.ActivityLevel;
import dev.diegobarrioh.forma.domain.BodyMeasurement;
import dev.diegobarrioh.forma.domain.MainGoal;
import dev.diegobarrioh.forma.domain.MeasurementSource;
import dev.diegobarrioh.forma.domain.Sex;
import dev.diegobarrioh.forma.domain.UserProfile;
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
 * ADR-015 slice 2, end to end over the real HTTP layer and real beans (no mocking) — the two
 * behaviors the ADR's own implementation plan asks this slice to verify: a second request while one
 * is already open answers 409 with the new {@code CONFLICT} code, and one account's open request
 * never surfaces through another account's session (ADR-012).
 *
 * <p>Two REAL, FK-valid accounts authenticate over the SAME real Spring Security filter chain via
 * {@link AuthTestSupport#asUser(UUID, String)}, following {@code
 * ClassACrossUserIsolationEndToEndTest}'s pattern — proving the wiring end to end, not just at the
 * service layer (which {@code PlanRequestServiceTest} already covers with mocks).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlanRequestCaptureEndToEndTest {

  private static final UUID USER_A = UUID.randomUUID();
  private static final UUID USER_B = UUID.randomUUID();
  private static final String USER_A_EMAIL = "plan-request-a@test.local";
  private static final String USER_B_EMAIL = "plan-request-b@test.local";
  private static final String PATH = "/api/v1/plan-requests";

  private static final String VALID_BODY =
      """
      {"direction":"LOSE_FAT","trainingDaysPerWeek":5,"mealsPerDay":5,
       "dietPattern":"OMNIVORE","cuisineStyle":"ESPANOLA"}
      """;

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private UserProfileRepository profileRepository;
  @Autowired private BodyMeasurementRepository measurementRepository;

  @BeforeEach
  void seedUsersAndProfiles() {
    cleanUpRows();
    jdbcTemplate.update(
        "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)", USER_A, USER_A_EMAIL, "!");
    jdbcTemplate.update(
        "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)", USER_B, USER_B_EMAIL, "!");
    seedCompleteProfile(USER_A);
    seedCompleteProfile(USER_B);
  }

  @AfterEach
  void cleanUp() {
    cleanUpRows();
  }

  private void cleanUpRows() {
    jdbcTemplate.update("DELETE FROM plan_request WHERE user_id IN (?, ?)", USER_A, USER_B);
    jdbcTemplate.update("DELETE FROM body_measurements WHERE user_id IN (?, ?)", USER_A, USER_B);
    jdbcTemplate.update("DELETE FROM user_profile WHERE user_id IN (?, ?)", USER_A, USER_B);
    jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", USER_A, USER_B);
  }

  /** Enough profile + measurement for {@code EnergyRequirement.of} to resolve without error. */
  private void seedCompleteProfile(UUID userId) {
    profileRepository.save(
        new UserProfile(
            userId,
            null,
            null,
            LocalDate.of(1988, 3, 1),
            Sex.MALE,
            180.0,
            ActivityLevel.MODERATE,
            MainGoal.COMPOSICION,
            null,
            null,
            null,
            null,
            true,
            null,
            null));
    measurementRepository.save(
        userId,
        new BodyMeasurement(
            Instant.now(), MeasurementSource.MANUAL, 73.6, null, null, null, null, null));
  }

  @Test
  void secondOpenRequestWhileOneIsPendingAnswers409Conflict() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .with(AuthTestSupport.asUser(USER_A, USER_A_EMAIL))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post(PATH)
                .with(AuthTestSupport.asUser(USER_A, USER_A_EMAIL))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"));
  }

  @Test
  void userBNeverSeesUserAsOpenRequestAndCanStillOpenTheirOwn() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .with(AuthTestSupport.asUser(USER_A, USER_A_EMAIL))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
        .andExpect(status().isCreated());

    // User A sees their own open request.
    mockMvc
        .perform(
            get(PATH + "/current").with(AuthTestSupport.asUser(USER_A, USER_A_EMAIL)).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"));

    // User B has none of their own -- 404, never A's data, never 403 (ADR-012: no existence leak).
    mockMvc
        .perform(
            get(PATH + "/current").with(AuthTestSupport.asUser(USER_B, USER_B_EMAIL)).with(csrf()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));

    // User B can still open their own -- the one-open-request rule is scoped per user, A's open
    // request never blocks B.
    mockMvc
        .perform(
            post(PATH)
                .with(AuthTestSupport.asUser(USER_B, USER_B_EMAIL))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
        .andExpect(status().isCreated());
  }
}
