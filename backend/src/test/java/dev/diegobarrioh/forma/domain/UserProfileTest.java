package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link UserProfile} (FOR-107): default construction and the enum-backed
 * preference values it is built from. Plain JUnit 5 + AssertJ (ADR-007).
 */
class UserProfileTest {

  private static final String OWNER_ID = "default-user";

  @Test
  void defaultsYieldDarkThemeMetricUnitsAndIncompleteOnboarding() {
    UserProfile defaults = UserProfile.defaults(OWNER_ID);

    assertThat(defaults.ownerId()).isEqualTo(OWNER_ID);
    assertThat(defaults.themeMode()).isEqualTo(ThemeMode.DARK);
    assertThat(defaults.unitPreferences()).isEqualTo(UnitPreferences.DEFAULT);
    assertThat(defaults.unitPreferences().weightUnit()).isEqualTo(WeightUnit.KG);
    assertThat(defaults.unitPreferences().heightUnit()).isEqualTo(HeightUnit.CM);
    assertThat(defaults.unitPreferences().distanceUnit()).isEqualTo(DistanceUnit.KM);
    assertThat(defaults.unitPreferences().energyUnit()).isEqualTo(EnergyUnit.KCAL);
    assertThat(defaults.firstRunCompleted()).isFalse();
    assertThat(defaults.onboardingAnswers()).isEqualTo(OnboardingAnswers.EMPTY);
    assertThat(defaults.name()).isNull();
    assertThat(defaults.email()).isNull();
  }

  @Test
  void rejectsInvalidThemeModeValue() {
    assertThatThrownBy(() -> ThemeMode.valueOf("NEON"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsInvalidUnitValue() {
    assertThatThrownBy(() -> WeightUnit.valueOf("LB")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> EnergyUnit.valueOf("KJ")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullOwnerId() {
    assertThatThrownBy(
            () ->
                new UserProfile(
                    null,
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
                    false))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void nullUnitPreferencesDefaultsToMetric() {
    UserProfile profile =
        new UserProfile(
            OWNER_ID, null, null, null, null, null, null, null, null, null, null, null, false);

    assertThat(profile.unitPreferences()).isEqualTo(UnitPreferences.DEFAULT);
    assertThat(profile.defaultObjectives()).isEqualTo(DefaultObjectives.EMPTY);
    assertThat(profile.themeMode()).isEqualTo(ThemeMode.DARK);
    assertThat(profile.onboardingAnswers()).isEqualTo(OnboardingAnswers.EMPTY);
  }
}
