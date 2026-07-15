package dev.diegobarrioh.forma.domain;

import java.util.List;

/**
 * Per-step onboarding draft answers plus the {@code firstRunCompleted} flag lives on {@link
 * UserProfile}, not here (FOR-107).
 *
 * <p>Structurally mirrors the frontend's {@code OnboardingAnswers} ({@code
 * frontend/src/pages/onboarding/onboardingStorage.ts}, FOR-59) field-for-field, per spec FOR-107
 * ("same shape as OnboardingAnswers"). This is intentionally a shallow, per-step draft — not a
 * normalized domain model — same as the frontend original: {@code goal.selected} and {@code
 * metrics.choice} are kept as plain strings here (not {@link MainGoal}/enum-typed) because they are
 * unvalidated in-progress answers, not the canonical profile value (which does use {@link
 * MainGoal}). A missing group on construction defaults to its blank/empty form rather than {@code
 * null}, so a partial onboarding submission never crashes downstream reads.
 *
 * @param profile the profile-step draft answers
 * @param metrics the body-metrics-step draft answers
 * @param goal the goal-step draft answer
 * @param training the training-days-step draft answer
 * @param equipment the equipment-step draft answer
 * @param nutrition the nutrition-preference-step draft answers
 */
public record OnboardingAnswers(
    ProfileDraft profile,
    MetricsDraft metrics,
    GoalDraft goal,
    TrainingDraft training,
    EquipmentDraft equipment,
    NutritionDraft nutrition) {

  public OnboardingAnswers {
    if (profile == null) {
      profile = ProfileDraft.EMPTY;
    }
    if (metrics == null) {
      metrics = MetricsDraft.EMPTY;
    }
    if (goal == null) {
      goal = GoalDraft.EMPTY;
    }
    if (training == null) {
      training = TrainingDraft.EMPTY;
    }
    if (equipment == null) {
      equipment = EquipmentDraft.EMPTY;
    }
    if (nutrition == null) {
      nutrition = NutritionDraft.EMPTY;
    }
  }

  /** No onboarding progress yet, used before any answer has been saved. */
  public static final OnboardingAnswers EMPTY =
      new OnboardingAnswers(null, null, null, null, null, null);

  /** The profile-step draft: name, birth date, sex and height as entered, unvalidated. */
  public record ProfileDraft(String name, String birthDate, String sex, String heightCm) {
    public static final ProfileDraft EMPTY = new ProfileDraft("", "", "", "");

    public ProfileDraft {
      name = name == null ? "" : name;
      birthDate = birthDate == null ? "" : birthDate;
      sex = sex == null ? "" : sex;
      heightCm = heightCm == null ? "" : heightCm;
    }
  }

  /** The body-metrics-step draft: whether the user chose manual entry or import, and progress. */
  public record MetricsDraft(String choice, boolean measurementSaved) {
    public static final MetricsDraft EMPTY = new MetricsDraft(null, false);
  }

  /** The goal-step draft: the selected {@code GoalOption}, or {@code null} if not yet chosen. */
  public record GoalDraft(String selected) {
    public static final GoalDraft EMPTY = new GoalDraft(null);
  }

  /** The training-days-step draft: the selected day labels. */
  public record TrainingDraft(List<String> days) {
    public static final TrainingDraft EMPTY = new TrainingDraft(List.of());

    public TrainingDraft {
      days = days == null ? List.of() : List.copyOf(days);
    }
  }

  /** The equipment-step draft: the selected equipment labels. */
  public record EquipmentDraft(List<String> items) {
    public static final EquipmentDraft EMPTY = new EquipmentDraft(List.of());

    public EquipmentDraft {
      items = items == null ? List.of() : List.copyOf(items);
    }
  }

  /** The nutrition-preference-step draft: free-text preference and restrictions. */
  public record NutritionDraft(String preference, String restrictions) {
    public static final NutritionDraft EMPTY = new NutritionDraft("", "");

    public NutritionDraft {
      preference = preference == null ? "" : preference;
      restrictions = restrictions == null ? "" : restrictions;
    }
  }
}
