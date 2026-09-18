package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.ActivityLevel;
import dev.diegobarrioh.forma.domain.BodyMeasurement;
import dev.diegobarrioh.forma.domain.CuisineStyle;
import dev.diegobarrioh.forma.domain.DietPattern;
import dev.diegobarrioh.forma.domain.EnergyRequirement;
import dev.diegobarrioh.forma.domain.MainGoal;
import dev.diegobarrioh.forma.domain.PlanDirection;
import dev.diegobarrioh.forma.domain.PlanObjective;
import dev.diegobarrioh.forma.domain.PlanRequestStatus;
import dev.diegobarrioh.forma.domain.Sex;
import dev.diegobarrioh.forma.domain.TrainingEquipment;
import dev.diegobarrioh.forma.domain.UserProfile;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * ADR-015 slice 2 (capture): turns the wizard's answers into a {@link PlanRequest} row.
 *
 * <p>Every input {@link EnergyRequirement#of} needs — sex, birth date, height, activity level, the
 * resolved goal — is read from {@link UserProfileRepository} and the newest {@link
 * BodyMeasurementRepository} row, at request time, and frozen onto the row (ADR-015 decision 7):
 * nothing here is re-derived later, so a request stays comparable against the numbers that produced
 * it even after the profile changes. {@code plan_objective} is the one input that never comes from
 * the profile: it is resolved from the wizard's explicit {@link PlanDirection} answer, never from
 * {@code main_goal} (ADR-015 decision 5, amended — guessing somebody's calories is exactly where
 * guessing is unacceptable).
 *
 * <p>{@code request_payload} and {@code catalog_version} are deliberately left {@code null} here:
 * they describe the outbound HTTP call to the plan agent, and that call happens at dispatch time
 * (ADR-015 slice 6), not at capture. {@code contract_version} is different — it is the shape THIS
 * build's outbound message would speak once dispatched, known already, so it is stamped at capture
 * (ADR-015 decision 13: "the outbound one is stored on the row").
 */
@Service
public class PlanRequestService {

  /**
   * The shape of the outbound plan-request message this build speaks (ADR-015 decision 13, {@code
   * docs/FORMA_Contrato_Agente_Plan.md}). FORMA has no other DTO versioning scheme; this is the
   * first, and the value is a literal until a second contract shape exists to distinguish it from.
   */
  static final String CONTRACT_VERSION = "1";

  private final PlanRequestRepository repository;
  private final UserProfileRepository profileRepository;
  private final BodyMeasurementRepository measurementRepository;
  private final CurrentUserProvider currentUserProvider;
  private final Clock clock;

  public PlanRequestService(
      PlanRequestRepository repository,
      UserProfileRepository profileRepository,
      BodyMeasurementRepository measurementRepository,
      CurrentUserProvider currentUserProvider,
      Clock clock) {
    this.repository = repository;
    this.profileRepository = profileRepository;
    this.measurementRepository = measurementRepository;
    this.currentUserProvider = currentUserProvider;
    this.clock = clock;
  }

  /**
   * Captures a new plan request for the caller.
   *
   * @param direction the wizard's explicit lose/gain/maintain answer; resolves {@code
   *     plan_objective} (ADR-015 decision 5)
   * @param trainingDaysPerWeek 0-7; the funnel's step allows 0 because the wizard's is optional
   * @param trainingWeekdays {@code null} means nobody said; an empty set is a different, deliberate
   *     answer (ADR-015 decision 6) — passed through unchanged
   * @param equipment {@code null} means nobody said; carried unused by this slice — passed through
   *     unchanged
   * @param mealsPerDay 3-6
   * @param dietPattern the exclusion rule; {@code UNSPECIFIED} is itself a stored answer
   * @param cuisineStyle the cuisine; {@code UNSPECIFIED} is itself a stored answer
   * @throws ConflictException when the caller already has an open request (ADR-015 decision 4) —
   *     checked here so the caller gets a clean 409 instead of the {@code
   *     ux_plan_request_user_open} constraint violation leaking as a 500
   * @throws ValidationException when the caller's profile or body measurements are not complete
   *     enough to compute {@code plan_kcal}
   */
  public PlanRequest request(
      PlanDirection direction,
      int trainingDaysPerWeek,
      Set<DayOfWeek> trainingWeekdays,
      Set<TrainingEquipment> equipment,
      int mealsPerDay,
      DietPattern dietPattern,
      CuisineStyle cuisineStyle) {
    UUID userId = currentUserProvider.currentUserId();

    if (repository.findOpenByUser(userId).isPresent()) {
      throw new ConflictException("Ya tienes una petición de plan en curso.");
    }

    UserProfile profile =
        profileRepository.find(userId).orElseGet(() -> UserProfile.defaults(userId));
    Sex sex = require(profile.sex(), "tu sexo");
    Double heightCm = require(profile.heightCm(), "tu altura");
    ActivityLevel activityLevel = require(profile.activityLevel(), "tu nivel de actividad");
    LocalDate birthDate = require(profile.birthDate(), "tu fecha de nacimiento");
    MainGoal mainGoal = resolveMainGoal(profile);

    List<BodyMeasurement> measurements = measurementRepository.list(userId);
    if (measurements.isEmpty()) {
      throw new ValidationException(
          "Necesitas registrar al menos una medición corporal antes de pedir un plan.");
    }
    double weightKg = measurements.get(0).weightKg();

    int ageYears = Period.between(birthDate, LocalDate.now(clock)).getYears();
    if (ageYears < 14 || ageYears > 120) {
      throw new ValidationException(
          "Tu fecha de nacimiento no da una edad válida para calcular un plan.");
    }
    if (heightCm <= 0) {
      throw new ValidationException("Tu altura registrada no es válida para calcular un plan.");
    }

    PlanObjective planObjective = direction.toPlanObjective();
    EnergyRequirement energy =
        EnergyRequirement.of(sex, ageYears, weightKg, heightCm, activityLevel, planObjective);

    OffsetDateTime now = OffsetDateTime.now(clock);
    PlanRequest planRequest =
        new PlanRequest(
            UUID.randomUUID(),
            userId,
            PlanRequestStatus.PENDING,
            CONTRACT_VERSION,
            null, // catalog_version: stamped at dispatch (slice 6), not at capture
            sex,
            ageYears,
            weightKg,
            heightCm,
            activityLevel,
            mainGoal,
            planObjective,
            trainingDaysPerWeek,
            trainingWeekdays,
            equipment,
            mealsPerDay,
            dietPattern,
            cuisineStyle,
            energy.planKcal(),
            profile.defaultObjectives().proteinTargetG(),
            profile.personalTargets().carbsTargetG(),
            profile.personalTargets().fatTargetG(),
            null, // request_payload: the outbound agent call's audit trail, written at dispatch
            null, // validation_report: written by the ingest (slice 6)
            null, // nutrition_plan_id: NULL until READY (slice 6) -- see the "known trap" this
            // slice must not write
            null,
            null,
            0,
            now,
            null,
            null,
            now);

    repository.insert(planRequest);
    return planRequest;
  }

  /**
   * The caller's currently open request.
   *
   * @throws NotFoundException when the caller has none — mirrors every other singular-resource read
   *     in this codebase ({@code NutritionPlanService#findById}, {@code
   *     WeeklyTrackingRecordService}'s GET-by-week): absence is a 404, not a special empty shape.
   */
  public PlanRequest currentOpen() {
    UUID userId = currentUserProvider.currentUserId();
    return repository
        .findOpenByUser(userId)
        .orElseThrow(() -> new NotFoundException("No tienes ninguna petición de plan en curso."));
  }

  /**
   * {@code main_goal} from the profile's canonical field, falling back to the onboarding draft
   * because the wizard never promotes the draft into the canonical field (ADR-015 Context, point 3,
   * verified against {@code UserProfileService#submitOnboardingAnswers}).
   */
  private MainGoal resolveMainGoal(UserProfile profile) {
    if (profile.mainGoal() != null) {
      return profile.mainGoal();
    }
    String draft = profile.onboardingAnswers().goal().selected();
    if (draft != null) {
      try {
        return MainGoal.valueOf(draft);
      } catch (IllegalArgumentException notAValue) {
        // fall through to the same "missing" error as no draft at all
      }
    }
    throw new ValidationException("Necesitas indicar tu objetivo antes de pedir un plan.");
  }

  private static <T> T require(T value, String fieldDescription) {
    if (value == null) {
      throw new ValidationException(
          "Completa " + fieldDescription + " en tu perfil antes de pedir un plan.");
    }
    return value;
  }
}
