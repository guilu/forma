package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.ActivityLevel;
import dev.diegobarrioh.forma.domain.DefaultObjectives;
import dev.diegobarrioh.forma.domain.MainGoal;
import dev.diegobarrioh.forma.domain.OnboardingAnswers;
import dev.diegobarrioh.forma.domain.Sex;
import dev.diegobarrioh.forma.domain.ThemeMode;
import dev.diegobarrioh.forma.domain.UnitPreferences;
import dev.diegobarrioh.forma.domain.UserProfile;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * Application use cases for the single-user profile & preferences aggregate (FOR-107).
 *
 * <p>Real multi-user auth (FOR-145b-2, ADR-012, migration V28): every use case resolves the
 * caller's account id via {@link CurrentUserProvider} instead of the old fixed {@code OWNER_ID =
 * "default-user"} constant (removed by this slice) — {@code user_profile}'s primary key was rebuilt
 * from the legacy {@code owner_id VARCHAR} column to {@code user_id UUID}.
 *
 * <p>Every update use case reads the current row (or {@link UserProfile#defaults(java.util.UUID)}
 * when none exists yet), replaces only the fields the caller supplied, and saves the merged
 * aggregate — so a partial update never nulls out unrelated preferences (spec FOR-107 Edge Cases,
 * tests.md Application Tests). {@link UserProfileRepository#save} always receives a complete,
 * merged aggregate; merging is this service's responsibility, not the repository's.
 */
@Service
public class UserProfileService {

  private final UserProfileRepository repository;
  private final CurrentUserProvider currentUserProvider;

  public UserProfileService(
      UserProfileRepository repository, CurrentUserProvider currentUserProvider) {
    this.repository = repository;
    this.currentUserProvider = currentUserProvider;
  }

  /** Returns the profile, or sane defaults when no row has been saved yet. */
  public UserProfile get() {
    return current();
  }

  /**
   * Whether the user has completed onboarding at least once (FOR-169 first-run gate). Defaults to
   * {@code false} on a fresh install (no saved profile), so read models can withhold catalog-backed
   * plans/lists until the user has actually configured the app.
   */
  public boolean firstRunCompleted() {
    return current().firstRunCompleted();
  }

  /**
   * Updates the "Profile fields" section (name, email, birthDate, sex, heightCm, activityLevel,
   * mainGoal). A {@code null} argument leaves the corresponding stored field unchanged.
   */
  public UserProfile updateProfileFields(
      String name,
      String email,
      LocalDate birthDate,
      Sex sex,
      Double heightCm,
      ActivityLevel activityLevel,
      MainGoal mainGoal) {
    UserProfile current = current();
    UserProfile merged =
        new UserProfile(
            current.ownerId(),
            name != null ? name : current.name(),
            email != null ? email : current.email(),
            birthDate != null ? birthDate : current.birthDate(),
            sex != null ? sex : current.sex(),
            heightCm != null ? heightCm : current.heightCm(),
            activityLevel != null ? activityLevel : current.activityLevel(),
            mainGoal != null ? mainGoal : current.mainGoal(),
            current.unitPreferences(),
            current.defaultObjectives(),
            current.themeMode(),
            current.onboardingAnswers(),
            current.firstRunCompleted(),
            current.profileBaseline(),
            current.personalTargets());
    repository.save(merged);
    return merged;
  }

  /**
   * Updates unit preferences. A {@code null} field within {@code requested} leaves the
   * corresponding stored unit unchanged (each dimension's own compact constructor additionally
   * defaults a {@code null} to its metric value when nothing was ever stored).
   */
  public UserProfile updateUnitPreferences(UnitPreferences requested) {
    UserProfile current = current();
    UnitPreferences currentPrefs = current.unitPreferences();
    UnitPreferences merged =
        new UnitPreferences(
            requested.weightUnit() != null ? requested.weightUnit() : currentPrefs.weightUnit(),
            requested.heightUnit() != null ? requested.heightUnit() : currentPrefs.heightUnit(),
            requested.distanceUnit() != null
                ? requested.distanceUnit()
                : currentPrefs.distanceUnit(),
            requested.energyUnit() != null ? requested.energyUnit() : currentPrefs.energyUnit());
    UserProfile updated = withUnitPreferences(current, merged);
    repository.save(updated);
    return updated;
  }

  /**
   * Updates default objectives. A {@code null} field within {@code requested} leaves the
   * corresponding stored target unchanged.
   */
  public UserProfile updateDefaultObjectives(DefaultObjectives requested) {
    UserProfile current = current();
    DefaultObjectives currentObjectives = current.defaultObjectives();
    DefaultObjectives merged =
        new DefaultObjectives(
            requested.caloricDeficitKcal() != null
                ? requested.caloricDeficitKcal()
                : currentObjectives.caloricDeficitKcal(),
            requested.proteinTargetG() != null
                ? requested.proteinTargetG()
                : currentObjectives.proteinTargetG(),
            requested.dailyWaterMl() != null
                ? requested.dailyWaterMl()
                : currentObjectives.dailyWaterMl());
    UserProfile updated = withDefaultObjectives(current, merged);
    repository.save(updated);
    return updated;
  }

  /** Updates the theme preference (single-valued; always fully replaced). */
  public UserProfile updateThemeMode(ThemeMode themeMode) {
    UserProfile current = current();
    UserProfile updated = withThemeMode(current, themeMode);
    repository.save(updated);
    return updated;
  }

  /**
   * Submits onboarding answers and the {@code firstRunCompleted} flag. Upserts across repeated
   * calls (in-progress → completed); re-submitting after completion is allowed and treated as a
   * profile edit, never locked (spec FOR-107 Edge Cases).
   */
  public UserProfile submitOnboardingAnswers(OnboardingAnswers answers, boolean completed) {
    UserProfile current = current();
    UserProfile updated =
        new UserProfile(
            current.ownerId(),
            current.name(),
            current.email(),
            current.birthDate(),
            current.sex(),
            current.heightCm(),
            current.activityLevel(),
            current.mainGoal(),
            current.unitPreferences(),
            current.defaultObjectives(),
            current.themeMode(),
            answers,
            completed || current.firstRunCompleted(),
            current.profileBaseline(),
            current.personalTargets());
    repository.save(updated);
    return updated;
  }

  private UserProfile current() {
    var userId = currentUserProvider.currentUserId();
    return repository.find(userId).orElseGet(() -> UserProfile.defaults(userId));
  }

  private static UserProfile withUnitPreferences(UserProfile source, UnitPreferences prefs) {
    return new UserProfile(
        source.ownerId(),
        source.name(),
        source.email(),
        source.birthDate(),
        source.sex(),
        source.heightCm(),
        source.activityLevel(),
        source.mainGoal(),
        prefs,
        source.defaultObjectives(),
        source.themeMode(),
        source.onboardingAnswers(),
        source.firstRunCompleted(),
        source.profileBaseline(),
        source.personalTargets());
  }

  private static UserProfile withDefaultObjectives(
      UserProfile source, DefaultObjectives objectives) {
    return new UserProfile(
        source.ownerId(),
        source.name(),
        source.email(),
        source.birthDate(),
        source.sex(),
        source.heightCm(),
        source.activityLevel(),
        source.mainGoal(),
        source.unitPreferences(),
        objectives,
        source.themeMode(),
        source.onboardingAnswers(),
        source.firstRunCompleted(),
        source.profileBaseline(),
        source.personalTargets());
  }

  private static UserProfile withThemeMode(UserProfile source, ThemeMode themeMode) {
    return new UserProfile(
        source.ownerId(),
        source.name(),
        source.email(),
        source.birthDate(),
        source.sex(),
        source.heightCm(),
        source.activityLevel(),
        source.mainGoal(),
        source.unitPreferences(),
        source.defaultObjectives(),
        themeMode,
        source.onboardingAnswers(),
        source.firstRunCompleted(),
        source.profileBaseline(),
        source.personalTargets());
  }
}
