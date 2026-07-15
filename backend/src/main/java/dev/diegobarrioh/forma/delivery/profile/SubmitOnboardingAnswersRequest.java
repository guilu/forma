package dev.diegobarrioh.forma.delivery.profile;

import dev.diegobarrioh.forma.domain.OnboardingAnswers;
import java.util.List;

/**
 * Request body for {@code PATCH /api/v1/profile/onboarding} (FOR-107): the full onboarding draft
 * plus the {@code completed} flag.
 *
 * <p>Unlike the other profile PATCH endpoints, this is a full replace of the stored onboarding
 * answers, not a per-field merge — it mirrors the frontend's own save behavior ({@code
 * saveOnboardingProgress} in {@code onboardingStorage.ts} always writes the complete draft,
 * FOR-59), so the client is expected to send the whole current draft each time, not just a delta. A
 * group omitted from the payload resets to its blank/empty form (via {@link OnboardingAnswers}'s
 * own null-defaulting), not "left unchanged" — this is a deliberate difference from the other PATCH
 * endpoints, documented since it could otherwise look like the "partial update must not clobber"
 * rule was violated.
 *
 * <p>Re-submitting after {@code completed = true} is allowed and treated as a profile edit, never
 * locked (spec FOR-107 Edge Cases) — this endpoint enforces no state-machine transition.
 *
 * @param profile the profile-step draft answers
 * @param metrics the body-metrics-step draft answers
 * @param goal the goal-step draft answer
 * @param training the training-days-step draft answer
 * @param equipment the equipment-step draft answer
 * @param nutrition the nutrition-preference-step draft answers
 * @param completed whether onboarding is complete after this submission
 */
public record SubmitOnboardingAnswersRequest(
    ProfileDraftRequest profile,
    MetricsDraftRequest metrics,
    GoalDraftRequest goal,
    TrainingDraftRequest training,
    EquipmentDraftRequest equipment,
    NutritionDraftRequest nutrition,
    boolean completed) {

  /** Maps this request to the domain {@link OnboardingAnswers} value. */
  public OnboardingAnswers toDomain() {
    return new OnboardingAnswers(
        profile == null
            ? null
            : new OnboardingAnswers.ProfileDraft(
                profile.name(), profile.birthDate(), profile.sex(), profile.heightCm()),
        metrics == null
            ? null
            : new OnboardingAnswers.MetricsDraft(metrics.choice(), metrics.measurementSaved()),
        goal == null ? null : new OnboardingAnswers.GoalDraft(goal.selected()),
        training == null ? null : new OnboardingAnswers.TrainingDraft(training.days()),
        equipment == null ? null : new OnboardingAnswers.EquipmentDraft(equipment.items()),
        nutrition == null
            ? null
            : new OnboardingAnswers.NutritionDraft(
                nutrition.preference(), nutrition.restrictions()));
  }

  public record ProfileDraftRequest(String name, String birthDate, String sex, String heightCm) {}

  public record MetricsDraftRequest(String choice, boolean measurementSaved) {}

  public record GoalDraftRequest(String selected) {}

  public record TrainingDraftRequest(List<String> days) {}

  public record EquipmentDraftRequest(List<String> items) {}

  public record NutritionDraftRequest(String preference, String restrictions) {}
}
