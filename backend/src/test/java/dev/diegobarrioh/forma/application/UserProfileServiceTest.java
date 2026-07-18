package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.domain.ActivityLevel;
import dev.diegobarrioh.forma.domain.DefaultObjectives;
import dev.diegobarrioh.forma.domain.MainGoal;
import dev.diegobarrioh.forma.domain.OnboardingAnswers;
import dev.diegobarrioh.forma.domain.PersonalTargets;
import dev.diegobarrioh.forma.domain.ProfileBaseline;
import dev.diegobarrioh.forma.domain.Sex;
import dev.diegobarrioh.forma.domain.ThemeMode;
import dev.diegobarrioh.forma.domain.UnitPreferences;
import dev.diegobarrioh.forma.domain.UserProfile;
import dev.diegobarrioh.forma.domain.WeightUnit;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Application use case tests for {@link UserProfileService} (FOR-107): the read-with-defaults
 * behavior and the scoped, non-clobbering update use cases. Uses a hand-rolled in-memory fake
 * repository (no Spring, no Mockito), matching {@code BodyMeasurementServiceTest} (FOR-17,
 * ADR-007).
 */
class UserProfileServiceTest {

  private final RecordingRepository repository = new RecordingRepository();
  private final UserProfileService service = new UserProfileService(repository);

  @Test
  void getReturnsDefaultsWhenNoRowExistsYet() {
    UserProfile profile = service.get();

    assertThat(profile.themeMode()).isEqualTo(ThemeMode.DARK);
    assertThat(profile.unitPreferences()).isEqualTo(UnitPreferences.DEFAULT);
    assertThat(profile.firstRunCompleted()).isFalse();
  }

  @Test
  void getReturnsStoredProfileWhenARowExists() {
    repository.rows.put(
        UserProfileService.OWNER_ID,
        new UserProfile(
            UserProfileService.OWNER_ID,
            "Ada",
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
            false,
            ProfileBaseline.EMPTY,
            PersonalTargets.EMPTY));

    assertThat(service.get().name()).isEqualTo("Ada");
  }

  @Test
  void updateProfileFieldsPersistsWithoutClobberingPreferences() {
    repository.rows.put(
        UserProfileService.OWNER_ID,
        new UserProfile(
            UserProfileService.OWNER_ID,
            "Old Name",
            "old@example.com",
            LocalDate.of(1990, 1, 1),
            Sex.FEMALE,
            170.0,
            ActivityLevel.MODERATE,
            MainGoal.HABITO,
            UnitPreferences.DEFAULT,
            new DefaultObjectives(500.0, 140.0, 2500.0),
            ThemeMode.LIGHT,
            OnboardingAnswers.EMPTY,
            true,
            ProfileBaseline.EMPTY,
            PersonalTargets.EMPTY));

    UserProfile updated =
        service.updateProfileFields("New Name", null, null, null, null, null, null);

    assertThat(updated.name()).isEqualTo("New Name");
    // Unrelated fields (including preferences) survive the partial update untouched.
    assertThat(updated.email()).isEqualTo("old@example.com");
    assertThat(updated.sex()).isEqualTo(Sex.FEMALE);
    assertThat(updated.themeMode()).isEqualTo(ThemeMode.LIGHT);
    assertThat(updated.defaultObjectives().caloricDeficitKcal()).isEqualTo(500.0);
    assertThat(repository.rows.get(UserProfileService.OWNER_ID)).isEqualTo(updated);
  }

  @Test
  void updateProfileFieldsPreservesBaselineAndPersonalTargets() {
    ProfileBaseline baseline = new ProfileBaseline(73.6, 14.7, 22.7);
    PersonalTargets targets = new PersonalTargets(2300.0, 12.0, 13.0, 73.0, 75.0, 70.0, 260.0);
    repository.rows.put(
        UserProfileService.OWNER_ID,
        new UserProfile(
            UserProfileService.OWNER_ID,
            "Diego",
            null,
            null,
            null,
            null,
            null,
            null,
            UnitPreferences.DEFAULT,
            new DefaultObjectives(null, 160.0, null),
            ThemeMode.DARK,
            OnboardingAnswers.EMPTY,
            false,
            baseline,
            targets));

    UserProfile updated =
        service.updateProfileFields("Diego B.", null, null, null, null, null, null);

    // A profile-fields-only update must not clobber the seeded baseline/targets (same
    // non-clobbering contract as preferences/objectives, spec FOR-149 Edge Cases).
    assertThat(updated.profileBaseline()).isEqualTo(baseline);
    assertThat(updated.personalTargets()).isEqualTo(targets);
    assertThat(updated.defaultObjectives().proteinTargetG()).isEqualTo(160.0);
  }

  @Test
  void updateThemeModePersistsWithoutClobberingProfileFields() {
    repository.rows.put(
        UserProfileService.OWNER_ID,
        new UserProfile(
            UserProfileService.OWNER_ID,
            "Ada",
            "ada@example.com",
            null,
            null,
            null,
            null,
            null,
            UnitPreferences.DEFAULT,
            DefaultObjectives.EMPTY,
            ThemeMode.DARK,
            OnboardingAnswers.EMPTY,
            false,
            ProfileBaseline.EMPTY,
            PersonalTargets.EMPTY));

    UserProfile updated = service.updateThemeMode(ThemeMode.LIGHT);

    assertThat(updated.themeMode()).isEqualTo(ThemeMode.LIGHT);
    assertThat(updated.name()).isEqualTo("Ada");
    assertThat(repository.rows.get(UserProfileService.OWNER_ID).themeMode())
        .isEqualTo(ThemeMode.LIGHT);
  }

  @Test
  void updateUnitPreferencesMergesOnlySuppliedFields() {
    UserProfile updated =
        service.updateUnitPreferences(new UnitPreferences(WeightUnit.KG, null, null, null));

    // Every unit preference dimension currently only supports its metric value, so an update
    // that only supplies weightUnit still reads back the full metric set.
    assertThat(updated.unitPreferences()).isEqualTo(UnitPreferences.DEFAULT);
  }

  @Test
  void updateDefaultObjectivesPersistsWithoutClobberingThemeMode() {
    repository.rows.put(
        UserProfileService.OWNER_ID,
        new UserProfile(
            UserProfileService.OWNER_ID,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            UnitPreferences.DEFAULT,
            DefaultObjectives.EMPTY,
            ThemeMode.LIGHT,
            OnboardingAnswers.EMPTY,
            false,
            ProfileBaseline.EMPTY,
            PersonalTargets.EMPTY));

    UserProfile updated = service.updateDefaultObjectives(new DefaultObjectives(400.0, null, null));

    assertThat(updated.defaultObjectives().caloricDeficitKcal()).isEqualTo(400.0);
    assertThat(updated.themeMode()).isEqualTo(ThemeMode.LIGHT);
  }

  @Test
  void submitOnboardingAnswersUpsertsAcrossRepeatedCalls() {
    OnboardingAnswers inProgress =
        new OnboardingAnswers(
            new OnboardingAnswers.ProfileDraft("Ada", "", "", ""), null, null, null, null, null);
    UserProfile afterFirstCall = service.submitOnboardingAnswers(inProgress, false);

    assertThat(afterFirstCall.onboardingAnswers().profile().name()).isEqualTo("Ada");
    assertThat(afterFirstCall.firstRunCompleted()).isFalse();

    OnboardingAnswers completed =
        new OnboardingAnswers(
            new OnboardingAnswers.ProfileDraft("Ada", "1990-01-01", "FEMALE", "170"),
            null,
            null,
            null,
            null,
            null);
    UserProfile afterSecondCall = service.submitOnboardingAnswers(completed, true);

    assertThat(afterSecondCall.onboardingAnswers().profile().birthDate()).isEqualTo("1990-01-01");
    assertThat(afterSecondCall.firstRunCompleted()).isTrue();
    // Still a single stored row, updated in place (upsert, not append).
    assertThat(repository.rows).hasSize(1);
  }

  @Test
  void submitOnboardingAnswersAfterCompletionIsStillAllowed() {
    repository.rows.put(
        UserProfileService.OWNER_ID,
        new UserProfile(
            UserProfileService.OWNER_ID,
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
            true,
            ProfileBaseline.EMPTY,
            PersonalTargets.EMPTY));

    UserProfile updated =
        service.submitOnboardingAnswers(
            new OnboardingAnswers(
                new OnboardingAnswers.ProfileDraft("Edited", "", "", ""),
                null,
                null,
                null,
                null,
                null),
            true);

    assertThat(updated.firstRunCompleted()).isTrue();
    assertThat(updated.onboardingAnswers().profile().name()).isEqualTo("Edited");
  }

  /** In-memory {@link UserProfileRepository} that records saves, keyed by owner id. */
  private static final class RecordingRepository implements UserProfileRepository {
    private final Map<String, UserProfile> rows = new HashMap<>();

    @Override
    public Optional<UserProfile> find(String ownerId) {
      return Optional.ofNullable(rows.get(ownerId));
    }

    @Override
    public void save(UserProfile profile) {
      rows.put(profile.ownerId(), profile);
    }
  }
}
