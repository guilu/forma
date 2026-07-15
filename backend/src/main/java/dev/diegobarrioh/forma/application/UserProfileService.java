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
 * <p>Single-user MVP (ADR-002): every use case operates on the one {@link #OWNER_ID} row. The
 * aggregate is still account-scoped in shape (an owner identifier on every row) so real
 * authorization can be layered on later without a data-model rewrite — this constant is the seam
 * that gets replaced by the authenticated caller's account id.
 *
 * <p>Every update use case reads the current row (or {@link UserProfile#defaults(String)} when none
 * exists yet), replaces only the fields the caller supplied, and saves the merged aggregate — so a
 * partial update never nulls out unrelated preferences (spec FOR-107 Edge Cases, tests.md
 * Application Tests). {@link UserProfileRepository#save} always receives a complete, merged
 * aggregate; merging is this service's responsibility, not the repository's.
 */
@Service
public class UserProfileService {

  /** Fixed single-user owner id for the MVP (ADR-002). Replaced by a real account id later. */
  public static final String OWNER_ID = "default-user";

  private final UserProfileRepository repository;

  public UserProfileService(UserProfileRepository repository) {
    this.repository = repository;
  }

  /** Returns the profile, or sane defaults when no row has been saved yet. */
  public UserProfile get() {
    return current();
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
            current.firstRunCompleted());
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
            completed || current.firstRunCompleted());
    repository.save(updated);
    return updated;
  }

  private UserProfile current() {
    return repository.find(OWNER_ID).orElseGet(() -> UserProfile.defaults(OWNER_ID));
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
        source.firstRunCompleted());
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
        source.firstRunCompleted());
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
        source.firstRunCompleted());
  }
}
