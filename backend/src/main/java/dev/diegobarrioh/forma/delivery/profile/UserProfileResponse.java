package dev.diegobarrioh.forma.delivery.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.diegobarrioh.forma.domain.DefaultObjectives;
import dev.diegobarrioh.forma.domain.OnboardingAnswers;
import dev.diegobarrioh.forma.domain.PersonalTargets;
import dev.diegobarrioh.forma.domain.UnitPreferences;
import dev.diegobarrioh.forma.domain.UserProfile;
import java.time.LocalDate;
import java.util.List;

/**
 * Response body for the profile & preferences API (FOR-107), used by {@code GET} and every {@code
 * PATCH} endpoint under {@code /api/v1/profile}.
 *
 * <p>A delivery-layer read model, distinct from the FOR-107 domain aggregate and persistence row
 * (ADR-005). {@code null} profile fields (unset on a fresh/default profile) are omitted from the
 * JSON, matching the {@link dev.diegobarrioh.forma.delivery.error.ApiError} convention — the client
 * sees "no value yet", never a fabricated one. Preferences, theme and onboarding are never {@code
 * null} (the domain aggregate always defaults them), so those nested objects are always present.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserProfileResponse(
    String name,
    String email,
    LocalDate birthDate,
    String sex,
    Double heightCm,
    String activityLevel,
    String mainGoal,
    UnitPreferencesResponse unitPreferences,
    DefaultObjectivesResponse defaultObjectives,
    String themeMode,
    OnboardingAnswersResponse onboardingAnswers,
    boolean firstRunCompleted,
    PersonalTargetsResponse personalTargets) {

  /** Maps the domain aggregate to its API read model. */
  public static UserProfileResponse from(UserProfile profile) {
    return new UserProfileResponse(
        profile.name(),
        profile.email(),
        profile.birthDate(),
        profile.sex() == null ? null : profile.sex().name(),
        profile.heightCm(),
        profile.activityLevel() == null ? null : profile.activityLevel().name(),
        profile.mainGoal() == null ? null : profile.mainGoal().name(),
        UnitPreferencesResponse.from(profile.unitPreferences()),
        DefaultObjectivesResponse.from(profile.defaultObjectives()),
        profile.themeMode().name(),
        OnboardingAnswersResponse.from(profile.onboardingAnswers()),
        profile.firstRunCompleted(),
        PersonalTargetsResponse.from(profile.personalTargets(), profile.defaultObjectives()));
  }

  /** Weight/height/distance/energy unit preferences (FOR-107). */
  public record UnitPreferencesResponse(
      String weightUnit, String heightUnit, String distanceUnit, String energyUnit) {

    static UnitPreferencesResponse from(UnitPreferences prefs) {
      return new UnitPreferencesResponse(
          prefs.weightUnit().name(),
          prefs.heightUnit().name(),
          prefs.distanceUnit().name(),
          prefs.energyUnit().name());
    }
  }

  /** Default caloric deficit/protein/water objectives (FOR-107); {@code null} when unset. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record DefaultObjectivesResponse(
      Double caloricDeficitKcal, Double proteinTargetG, Double dailyWaterMl) {

    static DefaultObjectivesResponse from(DefaultObjectives objectives) {
      return new DefaultObjectivesResponse(
          objectives.caloricDeficitKcal(), objectives.proteinTargetG(), objectives.dailyWaterMl());
    }
  }

  /**
   * Personal plan targets from the *Perfil* sheet (FOR-149): base kcal, body-fat/weight target
   * ranges and fat/carb macros, all {@code null} on an unseeded profile (never a fabricated value).
   * {@code proteinTargetG} is sourced from {@link DefaultObjectives#proteinTargetG()} (spec
   * FOR-149: the protein target reuses that existing field/column rather than duplicating it), so
   * it travels alongside the other targets in this single read-model block per api.md.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record PersonalTargetsResponse(
      Double baseCaloriesKcal,
      Double bodyFatTargetMinPct,
      Double bodyFatTargetMaxPct,
      Double weightTargetMinKg,
      Double weightTargetMaxKg,
      Double proteinTargetG,
      Double fatTargetG,
      Double carbsTargetG) {

    static PersonalTargetsResponse from(PersonalTargets targets, DefaultObjectives objectives) {
      return new PersonalTargetsResponse(
          targets.baseCaloriesKcal(),
          targets.bodyFatTargetMinPct(),
          targets.bodyFatTargetMaxPct(),
          targets.weightTargetMinKg(),
          targets.weightTargetMaxKg(),
          objectives.proteinTargetG(),
          targets.fatTargetG(),
          targets.carbsTargetG());
    }
  }

  /** Per-step onboarding draft answers, mirroring the frontend's {@code OnboardingAnswers}. */
  public record OnboardingAnswersResponse(
      ProfileDraftResponse profile,
      MetricsDraftResponse metrics,
      GoalDraftResponse goal,
      TrainingDraftResponse training,
      EquipmentDraftResponse equipment,
      NutritionDraftResponse nutrition) {

    static OnboardingAnswersResponse from(OnboardingAnswers answers) {
      return new OnboardingAnswersResponse(
          new ProfileDraftResponse(
              answers.profile().name(),
              answers.profile().birthDate(),
              answers.profile().sex(),
              answers.profile().heightCm()),
          new MetricsDraftResponse(
              answers.metrics().choice(), answers.metrics().measurementSaved()),
          new GoalDraftResponse(answers.goal().selected()),
          new TrainingDraftResponse(answers.training().days()),
          new EquipmentDraftResponse(answers.equipment().items()),
          new NutritionDraftResponse(
              answers.nutrition().preference(), answers.nutrition().restrictions()));
    }

    public record ProfileDraftResponse(
        String name, String birthDate, String sex, String heightCm) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MetricsDraftResponse(String choice, boolean measurementSaved) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GoalDraftResponse(String selected) {}

    public record TrainingDraftResponse(List<String> days) {}

    public record EquipmentDraftResponse(List<String> items) {}

    public record NutritionDraftResponse(String preference, String restrictions) {}
  }
}
