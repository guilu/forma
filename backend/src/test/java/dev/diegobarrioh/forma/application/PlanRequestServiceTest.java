package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.diegobarrioh.forma.domain.ActivityLevel;
import dev.diegobarrioh.forma.domain.BodyMeasurement;
import dev.diegobarrioh.forma.domain.CuisineStyle;
import dev.diegobarrioh.forma.domain.DefaultObjectives;
import dev.diegobarrioh.forma.domain.DietPattern;
import dev.diegobarrioh.forma.domain.MainGoal;
import dev.diegobarrioh.forma.domain.MeasurementSource;
import dev.diegobarrioh.forma.domain.OnboardingAnswers;
import dev.diegobarrioh.forma.domain.PersonalTargets;
import dev.diegobarrioh.forma.domain.PlanDirection;
import dev.diegobarrioh.forma.domain.PlanObjective;
import dev.diegobarrioh.forma.domain.PlanRequestStatus;
import dev.diegobarrioh.forma.domain.Sex;
import dev.diegobarrioh.forma.domain.TrainingEquipment;
import dev.diegobarrioh.forma.domain.UserProfile;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Application tests for {@link PlanRequestService} (ADR-015 slice 2, capture).
 *
 * <p>Written before {@link PlanRequestService#request} and {@link PlanRequestService#currentOpen}
 * had real bodies (Strict TDD) — every test here was red against the {@code
 * UnsupportedOperationException} skeleton before the implementation existed.
 */
@ExtendWith(MockitoExtension.class)
class PlanRequestServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-18T10:00:00Z"), ZoneOffset.UTC);

  @Mock private PlanRequestRepository repository;
  @Mock private UserProfileRepository profileRepository;
  @Mock private BodyMeasurementRepository measurementRepository;
  @Mock private CurrentUserProvider currentUserProvider;

  private PlanRequestService service;

  @BeforeEach
  void setUp() {
    service =
        new PlanRequestService(
            repository, profileRepository, measurementRepository, currentUserProvider, CLOCK);
  }

  /** A complete profile: sex, height, activity level and birth date all set, goal set. */
  private static UserProfile fullProfile() {
    return new UserProfile(
        USER_ID,
        "Diego",
        "diego@example.com",
        LocalDate.of(1988, 3, 1), // 38 on the fixed clock's date (2026-09-18)
        Sex.MALE,
        180.0,
        ActivityLevel.MODERATE,
        MainGoal.COMPOSICION,
        null,
        new DefaultObjectives(null, 160.0, null),
        null,
        OnboardingAnswers.EMPTY,
        true,
        null,
        new PersonalTargets(null, null, null, null, null, 70.0, 260.0));
  }

  private static BodyMeasurement measurement(double weightKg, Instant measuredAt) {
    return new BodyMeasurement(
        measuredAt, MeasurementSource.MANUAL, weightKg, null, null, null, null, null);
  }

  @Test
  void requestComputesAndFreezesPlanKcalFromTheProfileAndNewestMeasurement() {
    when(currentUserProvider.currentUserId()).thenReturn(USER_ID);
    when(repository.findOpenByUser(USER_ID)).thenReturn(Optional.empty());
    when(profileRepository.find(USER_ID)).thenReturn(Optional.of(fullProfile()));
    // Newest first (BodyMeasurementRepository#list's documented order): 73.6 is the one that must
    // win, 80.0 is a stale older reading that must be ignored.
    when(measurementRepository.list(USER_ID))
        .thenReturn(
            List.of(
                measurement(73.6, Instant.parse("2026-09-10T08:00:00Z")),
                measurement(80.0, Instant.parse("2026-01-01T08:00:00Z"))));

    PlanRequest created =
        service.request(
            PlanDirection.LOSE_FAT,
            5,
            Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
            Set.of(TrainingEquipment.DUMBBELLS),
            5,
            DietPattern.OMNIVORE,
            CuisineStyle.ESPANOLA);

    ArgumentCaptor<PlanRequest> captor = ArgumentCaptor.forClass(PlanRequest.class);
    verify(repository).insert(captor.capture());
    PlanRequest stored = captor.getValue();

    assertThat(stored).isEqualTo(created);
    assertThat(stored.userId()).isEqualTo(USER_ID);
    assertThat(stored.status()).isEqualTo(PlanRequestStatus.PENDING);
    assertThat(stored.contractVersion()).isEqualTo("1");
    assertThat(stored.catalogVersion()).isNull();
    assertThat(stored.sex()).isEqualTo(Sex.MALE);
    assertThat(stored.ageYears()).isEqualTo(38);
    assertThat(stored.weightKg()).isEqualTo(73.6);
    assertThat(stored.heightCm()).isEqualTo(180.0);
    assertThat(stored.activityLevel()).isEqualTo(ActivityLevel.MODERATE);
    assertThat(stored.mainGoal()).isEqualTo(MainGoal.COMPOSICION);
    assertThat(stored.planObjective()).isEqualTo(PlanObjective.WEIGHT_LOSS);
    assertThat(stored.trainingDaysPerWeek()).isEqualTo(5);
    assertThat(stored.trainingWeekdays())
        .containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.TUESDAY);
    assertThat(stored.equipment()).containsExactly(TrainingEquipment.DUMBBELLS);
    assertThat(stored.mealsPerDay()).isEqualTo(5);
    assertThat(stored.dietPattern()).isEqualTo(DietPattern.OMNIVORE);
    assertThat(stored.cuisineStyle()).isEqualTo(CuisineStyle.ESPANOLA);
    // 10*73.6 + 6.25*180 - 5*38 + 5 = 1676 basal; x1.55 = 2598 daily; x0.80 = 2078 plan
    // (docs/FORMA_Contrato_Agente_Plan.md's own worked example).
    assertThat(stored.planKcal()).isEqualTo(2078);
    assertThat(stored.targetProteinG()).isEqualTo(160.0);
    assertThat(stored.targetCarbsG()).isEqualTo(260.0);
    assertThat(stored.targetFatG()).isEqualTo(70.0);
    // block_length_weeks/block_number/next_review_date: the programme's shape (ADR-015 decision
    // 14). This service always writes the first block of a fresh programme today -- there is no
    // successor-block logic yet -- and leaves the review date null until a block start exists to
    // measure it from.
    assertThat(stored.blockLengthWeeks()).isEqualTo(4);
    assertThat(stored.blockNumber()).isEqualTo(1);
    assertThat(stored.nextReviewDate()).isNull();
    // request_payload is the outbound agent call's audit trail (ADR-015 decision 8/13); that call
    // happens at dispatch, slice 6 — this slice must leave it null, not invent a serialisation.
    assertThat(stored.requestPayload()).isNull();
    assertThat(stored.validationReport()).isNull();
    assertThat(stored.nutritionPlanId()).isNull();
    assertThat(stored.failureCode()).isNull();
    assertThat(stored.failureDetail()).isNull();
    assertThat(stored.attemptCount()).isZero();
    assertThat(stored.dispatchedAt()).isNull();
    assertThat(stored.completedAt()).isNull();
  }

  @Test
  void requestRejectsASecondOpenRequestWithConflictAndNeverInserts() {
    when(currentUserProvider.currentUserId()).thenReturn(USER_ID);
    when(repository.findOpenByUser(USER_ID)).thenReturn(Optional.of(existingOpenRequest()));

    assertThatThrownBy(
            () ->
                service.request(
                    PlanDirection.MAINTAIN,
                    3,
                    null,
                    null,
                    3,
                    DietPattern.UNSPECIFIED,
                    CuisineStyle.UNSPECIFIED))
        .isInstanceOf(ConflictException.class);

    verify(repository, never()).insert(any());
  }

  @Test
  void requestFallsBackToTheOnboardingGoalDraftWhenTheProfileHasNoMainGoal() {
    when(currentUserProvider.currentUserId()).thenReturn(USER_ID);
    when(repository.findOpenByUser(USER_ID)).thenReturn(Optional.empty());
    UserProfile noMainGoal =
        new UserProfile(
            USER_ID,
            null,
            null,
            LocalDate.of(1988, 3, 1),
            Sex.FEMALE,
            165.0,
            ActivityLevel.LIGHT,
            null, // main_goal never promoted from the draft (ADR-015, verified in Context)
            null,
            null,
            null,
            new OnboardingAnswers(
                null, null, new OnboardingAnswers.GoalDraft("HABITO"), null, null, null),
            true,
            null,
            null);
    when(profileRepository.find(USER_ID)).thenReturn(Optional.of(noMainGoal));
    when(measurementRepository.list(USER_ID))
        .thenReturn(List.of(measurement(60.0, Instant.parse("2026-09-10T08:00:00Z"))));

    service.request(
        PlanDirection.MAINTAIN,
        0,
        null,
        null,
        3,
        DietPattern.UNSPECIFIED,
        CuisineStyle.UNSPECIFIED);

    ArgumentCaptor<PlanRequest> captor = ArgumentCaptor.forClass(PlanRequest.class);
    verify(repository).insert(captor.capture());
    assertThat(captor.getValue().mainGoal()).isEqualTo(MainGoal.HABITO);
  }

  @Test
  void requestRejectsAnIncompleteProfileAsAValidationError() {
    when(currentUserProvider.currentUserId()).thenReturn(USER_ID);
    when(repository.findOpenByUser(USER_ID)).thenReturn(Optional.empty());
    when(profileRepository.find(USER_ID)).thenReturn(Optional.of(UserProfile.defaults(USER_ID)));

    assertThatThrownBy(
            () ->
                service.request(
                    PlanDirection.LOSE_FAT,
                    3,
                    null,
                    null,
                    3,
                    DietPattern.OMNIVORE,
                    CuisineStyle.ESPANOLA))
        .isInstanceOf(ValidationException.class);

    verify(repository, never()).insert(any());
  }

  @Test
  void requestRejectsWhenThereIsNoBodyMeasurementYet() {
    when(currentUserProvider.currentUserId()).thenReturn(USER_ID);
    when(repository.findOpenByUser(USER_ID)).thenReturn(Optional.empty());
    when(profileRepository.find(USER_ID)).thenReturn(Optional.of(fullProfile()));
    when(measurementRepository.list(USER_ID)).thenReturn(List.of());

    assertThatThrownBy(
            () ->
                service.request(
                    PlanDirection.LOSE_FAT,
                    3,
                    null,
                    null,
                    3,
                    DietPattern.OMNIVORE,
                    CuisineStyle.ESPANOLA))
        .isInstanceOf(ValidationException.class);

    verify(repository, never()).insert(any());
  }

  @Test
  void currentOpenReturnsTheCallersOpenRequest() {
    when(currentUserProvider.currentUserId()).thenReturn(USER_ID);
    PlanRequest open = existingOpenRequest();
    when(repository.findOpenByUser(USER_ID)).thenReturn(Optional.of(open));

    assertThat(service.currentOpen()).isEqualTo(open);
  }

  @Test
  void currentOpenAnswersNotFoundWhenTheCallerHasNoOpenRequest() {
    when(currentUserProvider.currentUserId()).thenReturn(USER_ID);
    when(repository.findOpenByUser(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.currentOpen()).isInstanceOf(NotFoundException.class);
  }

  private static PlanRequest existingOpenRequest() {
    return new PlanRequest(
        UUID.randomUUID(),
        USER_ID,
        PlanRequestStatus.PENDING,
        "1",
        null,
        Sex.MALE,
        38,
        73.6,
        180.0,
        ActivityLevel.MODERATE,
        MainGoal.COMPOSICION,
        PlanObjective.WEIGHT_LOSS,
        5,
        Set.of(DayOfWeek.MONDAY),
        Set.of(TrainingEquipment.DUMBBELLS),
        5,
        DietPattern.OMNIVORE,
        CuisineStyle.ESPANOLA,
        2078,
        160.0,
        260.0,
        70.0,
        4,
        1,
        null,
        null,
        null,
        null,
        null,
        null,
        0,
        java.time.OffsetDateTime.now(CLOCK),
        null,
        null,
        java.time.OffsetDateTime.now(CLOCK));
  }
}
