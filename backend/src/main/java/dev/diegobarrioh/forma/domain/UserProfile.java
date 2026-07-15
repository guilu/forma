package dev.diegobarrioh.forma.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * The single-user profile & preferences aggregate (FOR-107): profile fields, unit preferences,
 * default objectives, theme preference and onboarding progress.
 *
 * <p>Framework-free — no Spring, JDBC or HTTP types (ADR-001). This is a consolidated "stub"
 * aggregate per spec FOR-107: it establishes the aggregate and API shape closing the "no profile
 * backend yet" gap from FOR-58's Data Model Notes, and unblocks FOR-119 (profile/units edit),
 * FOR-120 (server-side theme) and FOR-121 (onboarding persistence). Deeper preference domains
 * (training/nutrition detail) can extend it later without a rewrite.
 *
 * <p><b>Persistence shape (spec FOR-107 Open Questions):</b> a single {@code user_profile} table
 * backs this whole aggregate (one row per {@code ownerId}) rather than three separate tables — the
 * MVP scale does not justify the extra join/consistency complexity, and the API surface is already
 * one read model either way. Documented here per the spec's request to record the final choice.
 *
 * <p>{@code ownerId} exists even though authorization is not enforced yet (ADR-002: "must not be
 * designed as if authorization does not matter") — every row is account-scoped in shape, ready for
 * a real account id once authentication lands.
 *
 * <p>All profile fields ({@code name} through {@code mainGoal}) are nullable: a fresh {@link
 * #defaults(String)} aggregate (no row saved yet) carries no profile data, only the default
 * preferences (spec FOR-107 Edge Cases: "First call before any profile row exists → return
 * defaults... not a 404").
 *
 * @param ownerId the owning account's identifier; required (ADR-002 account-scoping)
 * @param name display name; optional
 * @param email contact email; optional
 * @param birthDate date of birth; optional
 * @param sex biological sex; optional
 * @param heightCm height in centimeters; optional
 * @param activityLevel self-reported activity level; optional
 * @param mainGoal the user's main training/health goal; optional
 * @param unitPreferences weight/height/distance/energy unit preferences; defaults to {@link
 *     UnitPreferences#DEFAULT} when {@code null}
 * @param defaultObjectives default caloric deficit/protein/water targets; defaults to {@link
 *     DefaultObjectives#EMPTY} when {@code null}
 * @param themeMode light/dark/system theme preference; defaults to {@link ThemeMode#DARK} when
 *     {@code null} (spec FOR-107 tests: "Default construction... yields dark theme")
 * @param onboardingAnswers per-step onboarding draft answers; defaults to {@link
 *     OnboardingAnswers#EMPTY} when {@code null}
 * @param firstRunCompleted whether the onboarding flow has been completed at least once
 */
public record UserProfile(
    String ownerId,
    String name,
    String email,
    LocalDate birthDate,
    Sex sex,
    Double heightCm,
    ActivityLevel activityLevel,
    MainGoal mainGoal,
    UnitPreferences unitPreferences,
    DefaultObjectives defaultObjectives,
    ThemeMode themeMode,
    OnboardingAnswers onboardingAnswers,
    boolean firstRunCompleted) {

  public UserProfile {
    Objects.requireNonNull(ownerId, "ownerId must not be null");
    if (unitPreferences == null) {
      unitPreferences = UnitPreferences.DEFAULT;
    }
    if (defaultObjectives == null) {
      defaultObjectives = DefaultObjectives.EMPTY;
    }
    if (themeMode == null) {
      themeMode = ThemeMode.DARK;
    }
    if (onboardingAnswers == null) {
      onboardingAnswers = OnboardingAnswers.EMPTY;
    }
  }

  /**
   * Builds the sane-defaults profile returned before any row has been saved for {@code ownerId}
   * (spec FOR-107 Edge Cases): no profile fields set, metric unit preferences, no default
   * objectives, dark theme, empty onboarding answers, onboarding not completed.
   */
  public static UserProfile defaults(String ownerId) {
    return new UserProfile(
        ownerId,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        UnitPreferences.DEFAULT,
        DefaultObjectives.EMPTY,
        ThemeMode.DARK,
        OnboardingAnswers.EMPTY,
        false);
  }
}
