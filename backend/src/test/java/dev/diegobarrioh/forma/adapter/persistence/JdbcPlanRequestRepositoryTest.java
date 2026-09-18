package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.application.PlanRequest;
import dev.diegobarrioh.forma.application.PlanRequestRepository;
import dev.diegobarrioh.forma.bootstrap.LegacyUserBootstrap;
import dev.diegobarrioh.forma.domain.ActivityLevel;
import dev.diegobarrioh.forma.domain.CuisineStyle;
import dev.diegobarrioh.forma.domain.DietPattern;
import dev.diegobarrioh.forma.domain.MainGoal;
import dev.diegobarrioh.forma.domain.PlanObjective;
import dev.diegobarrioh.forma.domain.PlanRequestStatus;
import dev.diegobarrioh.forma.domain.Sex;
import dev.diegobarrioh.forma.domain.TrainingEquipment;
import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link JdbcPlanRequestRepository} (migration V63, ADR-015). Runs against the
 * in-memory PostgreSQL-mode H2 with Flyway migrations applied — {@code MODE=PostgreSQL} (see {@code
 * application-test.yml}) — the same suite that proves V63 applies cleanly at all, because a
 * {@code @SpringBootTest} context does not start if Flyway fails.
 *
 * <p>This is the "genuinely verified rather than merely applied" round-trip ADR-015's slice 1 asks
 * for: a full domain object through every typed column, plus the two invariants the schema exists
 * to enforce — the open-marker sentinel and the READY-needs-a-plan CHECK. It does not verify
 * PostgreSQL 17 itself; see the PR description for what remains unverified there.
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcPlanRequestRepositoryTest {

  @Autowired private PlanRequestRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearTestRows() {
    jdbcTemplate.update(
        "DELETE FROM plan_request WHERE user_id = ?", LegacyUserBootstrap.PLACEHOLDER_USER_ID);
  }

  private static PlanRequest fullRequest(UUID id, PlanRequestStatus status, UUID nutritionPlanId) {
    OffsetDateTime now =
        OffsetDateTime.parse("2026-09-18T10:00:00Z").withOffsetSameInstant(ZoneOffset.UTC);
    return new PlanRequest(
        id,
        LegacyUserBootstrap.PLACEHOLDER_USER_ID,
        status,
        "1",
        "2026-09-18",
        Sex.MALE,
        38,
        73.6,
        180.0,
        ActivityLevel.MODERATE,
        MainGoal.COMPOSICION,
        PlanObjective.WEIGHT_LOSS,
        5,
        Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
        Set.of(TrainingEquipment.DUMBBELLS, TrainingEquipment.BARBELL),
        5,
        DietPattern.OMNIVORE,
        CuisineStyle.ESPANOLA,
        2078,
        160.0,
        260.0,
        70.0,
        "{\"contractVersion\":\"1\"}",
        null,
        nutritionPlanId,
        null,
        null,
        0,
        now,
        null,
        null,
        now);
  }

  @Test
  void insertThenFindByIdRoundTripsEveryColumn() {
    UUID id = UUID.randomUUID();

    repository.insert(fullRequest(id, PlanRequestStatus.PENDING, null));
    Optional<PlanRequest> found = repository.findById(id);

    assertThat(found).isPresent();
    PlanRequest request = found.orElseThrow();
    assertThat(request.userId()).isEqualTo(LegacyUserBootstrap.PLACEHOLDER_USER_ID);
    assertThat(request.status()).isEqualTo(PlanRequestStatus.PENDING);
    assertThat(request.contractVersion()).isEqualTo("1");
    assertThat(request.catalogVersion()).isEqualTo("2026-09-18");
    assertThat(request.sex()).isEqualTo(Sex.MALE);
    assertThat(request.ageYears()).isEqualTo(38);
    assertThat(request.weightKg()).isEqualTo(73.6);
    assertThat(request.heightCm()).isEqualTo(180.0);
    assertThat(request.activityLevel()).isEqualTo(ActivityLevel.MODERATE);
    assertThat(request.mainGoal()).isEqualTo(MainGoal.COMPOSICION);
    assertThat(request.planObjective()).isEqualTo(PlanObjective.WEIGHT_LOSS);
    assertThat(request.trainingDaysPerWeek()).isEqualTo(5);
    assertThat(request.trainingWeekdays())
        .containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY);
    assertThat(request.equipment())
        .containsExactlyInAnyOrder(TrainingEquipment.DUMBBELLS, TrainingEquipment.BARBELL);
    assertThat(request.mealsPerDay()).isEqualTo(5);
    assertThat(request.dietPattern()).isEqualTo(DietPattern.OMNIVORE);
    assertThat(request.cuisineStyle()).isEqualTo(CuisineStyle.ESPANOLA);
    assertThat(request.planKcal()).isEqualTo(2078);
    assertThat(request.targetProteinG()).isEqualTo(160.0);
    assertThat(request.targetCarbsG()).isEqualTo(260.0);
    assertThat(request.targetFatG()).isEqualTo(70.0);
    assertThat(request.requestPayload()).contains("contractVersion");
    assertThat(request.validationReport()).isNull();
    assertThat(request.nutritionPlanId()).isNull();
    assertThat(request.failureCode()).isNull();
    assertThat(request.failureDetail()).isNull();
    assertThat(request.attemptCount()).isZero();
    assertThat(request.requestedAt()).isNotNull();
    assertThat(request.dispatchedAt()).isNull();
    assertThat(request.completedAt()).isNull();
  }

  @Test
  void nullWeekdaysAndEquipmentRoundTripAsNull() {
    UUID id = UUID.randomUUID();
    PlanRequest withNulls =
        new PlanRequest(
            id,
            LegacyUserBootstrap.PLACEHOLDER_USER_ID,
            PlanRequestStatus.FAILED,
            "1",
            null,
            Sex.FEMALE,
            29,
            60.0,
            165.0,
            ActivityLevel.LIGHT,
            MainGoal.HABITO,
            PlanObjective.HEALTHY_EATING,
            0,
            null,
            null,
            3,
            DietPattern.UNSPECIFIED,
            CuisineStyle.UNSPECIFIED,
            1800,
            null,
            null,
            null,
            null,
            null,
            null,
            "UNREACHABLE",
            "El agente no respondió a tiempo.",
            1,
            OffsetDateTime.now(ZoneOffset.UTC),
            null,
            null,
            OffsetDateTime.now(ZoneOffset.UTC));

    repository.insert(withNulls);
    PlanRequest found = repository.findById(id).orElseThrow();

    assertThat(found.trainingWeekdays()).isNull();
    assertThat(found.equipment()).isNull();
    assertThat(found.catalogVersion()).isNull();
    assertThat(found.targetProteinG()).isNull();
    assertThat(found.failureCode()).isEqualTo("UNREACHABLE");
  }

  /**
   * The open-marker sentinel (ADR-015 decision 4): any number of closed requests coexist, and a
   * second open one for the same user is rejected by {@code ux_plan_request_user_open} — never by
   * application code, so the guarantee holds even against a bug that forgets to check first.
   */
  @Test
  void uniqueIndexAdmitsManyClosedRequestsAndExactlyOneOpenOne() {
    repository.insert(fullRequest(UUID.randomUUID(), PlanRequestStatus.FAILED, null));
    repository.insert(fullRequest(UUID.randomUUID(), PlanRequestStatus.FAILED, null));
    repository.insert(fullRequest(UUID.randomUUID(), PlanRequestStatus.PENDING, null));

    assertThatThrownBy(
            () ->
                repository.insert(
                    fullRequest(UUID.randomUUID(), PlanRequestStatus.GENERATING, null)))
        .isInstanceOf(DataIntegrityViolationException.class);

    Integer openCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM plan_request WHERE user_id = ? AND open_marker IS NOT NULL",
            Integer.class,
            LegacyUserBootstrap.PLACEHOLDER_USER_ID);
    assertThat(openCount).isEqualTo(1);
  }

  /**
   * ADR-015 decision 2: a READY row always points at a stored plan, and the CHECK — not just
   * discipline in the code that writes it — is what makes that true.
   */
  @Test
  void checkRejectsReadyWithNoNutritionPlan() {
    PlanRequest readyWithNoPlan = fullRequest(UUID.randomUUID(), PlanRequestStatus.READY, null);

    assertThatThrownBy(() -> repository.insert(readyWithNoPlan))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  /**
   * ADR-015 slice 2: {@code findOpenByUser} is what {@code PlanRequestService#request} uses to
   * enforce the one-open-request rule as a clean {@code ConflictException} instead of letting the
   * {@code ux_plan_request_user_open} constraint leak as a 500, and what {@code GET
   * /api/v1/plan-requests/current} reads to show the caller their open request.
   */
  @Test
  void findOpenByUserFindsAPendingRequest() {
    UUID id = UUID.randomUUID();
    repository.insert(fullRequest(id, PlanRequestStatus.PENDING, null));

    Optional<PlanRequest> open = repository.findOpenByUser(LegacyUserBootstrap.PLACEHOLDER_USER_ID);

    assertThat(open).isPresent();
    assertThat(open.orElseThrow().id()).isEqualTo(id);
  }

  @Test
  void findOpenByUserFindsAGeneratingRequest() {
    UUID id = UUID.randomUUID();
    repository.insert(fullRequest(id, PlanRequestStatus.GENERATING, null));

    Optional<PlanRequest> open = repository.findOpenByUser(LegacyUserBootstrap.PLACEHOLDER_USER_ID);

    assertThat(open).isPresent();
    assertThat(open.orElseThrow().id()).isEqualTo(id);
  }

  @Test
  void findOpenByUserIgnoresTerminalRequests() {
    // Two closed rows for the same user (the open-marker unique index admits any number of them,
    // decision 4) -- neither is "open", so both must be invisible to findOpenByUser.
    repository.insert(fullRequest(UUID.randomUUID(), PlanRequestStatus.FAILED, null));
    repository.insert(fullRequest(UUID.randomUUID(), PlanRequestStatus.FAILED, null));

    Optional<PlanRequest> open = repository.findOpenByUser(LegacyUserBootstrap.PLACEHOLDER_USER_ID);

    assertThat(open).isEmpty();
  }

  @Test
  void findOpenByUserReturnsEmptyWhenTheUserHasNoRequestsAtAll() {
    Optional<PlanRequest> open = repository.findOpenByUser(UUID.randomUUID());

    assertThat(open).isEmpty();
  }
}
