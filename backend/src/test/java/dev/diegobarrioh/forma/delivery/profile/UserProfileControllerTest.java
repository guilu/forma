package dev.diegobarrioh.forma.delivery.profile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.UserProfileService;
import dev.diegobarrioh.forma.domain.DefaultObjectives;
import dev.diegobarrioh.forma.domain.MainGoal;
import dev.diegobarrioh.forma.domain.OnboardingAnswers;
import dev.diegobarrioh.forma.domain.PersonalTargets;
import dev.diegobarrioh.forma.domain.ProfileBaseline;
import dev.diegobarrioh.forma.domain.ThemeMode;
import dev.diegobarrioh.forma.domain.UnitPreferences;
import dev.diegobarrioh.forma.domain.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link UserProfileController} (FOR-107): defaults on first read, scoped
 * updates and enum validation. The {@link UserProfileService} is mocked (like {@code
 * ShoppingProductControllerTest}, FOR-36) so only controller mapping/validation is exercised here;
 * the merge-without-clobbering behavior itself is covered by {@code UserProfileServiceTest}.
 */
@WebMvcTest(UserProfileController.class)
class UserProfileControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private UserProfileService service;

  private static UserProfile profile(String name, ThemeMode themeMode, boolean firstRunCompleted) {
    return new UserProfile(
        "default-user",
        name,
        null,
        null,
        null,
        null,
        null,
        null,
        UnitPreferences.DEFAULT,
        DefaultObjectives.EMPTY,
        themeMode,
        OnboardingAnswers.EMPTY,
        firstRunCompleted,
        ProfileBaseline.EMPTY,
        PersonalTargets.EMPTY);
  }

  @Test
  void getBeforeAnyWriteReturnsDefaultPayload() throws Exception {
    when(service.get()).thenReturn(UserProfile.defaults("default-user"));

    mockMvc
        .perform(get("/api/v1/profile"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.themeMode").value("DARK"))
        .andExpect(jsonPath("$.unitPreferences.weightUnit").value("KG"))
        .andExpect(jsonPath("$.unitPreferences.heightUnit").value("CM"))
        .andExpect(jsonPath("$.unitPreferences.distanceUnit").value("KM"))
        .andExpect(jsonPath("$.unitPreferences.energyUnit").value("KCAL"))
        .andExpect(jsonPath("$.firstRunCompleted").value(false))
        .andExpect(jsonPath("$.name").doesNotExist())
        .andExpect(jsonPath("$.personalTargets.baseCaloriesKcal").doesNotExist());
  }

  @Test
  void getReturnsSeededPersonalTargetsMatchingThePerfilSheet() throws Exception {
    UserProfile withTargets =
        new UserProfile(
            "default-user",
            "Diego",
            null,
            null,
            null,
            180.0,
            null,
            MainGoal.COMPOSICION,
            UnitPreferences.DEFAULT,
            new DefaultObjectives(null, 160.0, null),
            ThemeMode.DARK,
            OnboardingAnswers.EMPTY,
            false,
            new ProfileBaseline(73.6, 14.7, 22.7),
            new PersonalTargets(2300.0, 12.0, 13.0, 73.0, 75.0, 70.0, 260.0));
    when(service.get()).thenReturn(withTargets);

    mockMvc
        .perform(get("/api/v1/profile"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Diego"))
        .andExpect(jsonPath("$.heightCm").value(180.0))
        .andExpect(jsonPath("$.personalTargets.baseCaloriesKcal").value(2300.0))
        .andExpect(jsonPath("$.personalTargets.bodyFatTargetMinPct").value(12.0))
        .andExpect(jsonPath("$.personalTargets.bodyFatTargetMaxPct").value(13.0))
        .andExpect(jsonPath("$.personalTargets.weightTargetMinKg").value(73.0))
        .andExpect(jsonPath("$.personalTargets.weightTargetMaxKg").value(75.0))
        .andExpect(jsonPath("$.personalTargets.proteinTargetG").value(160.0))
        .andExpect(jsonPath("$.personalTargets.fatTargetG").value(70.0))
        .andExpect(jsonPath("$.personalTargets.carbsTargetG").value(260.0));
  }

  @Test
  void getForAnUnseededProfileReturnsNullTargetsNot404() throws Exception {
    when(service.get()).thenReturn(UserProfile.defaults("default-user"));

    mockMvc
        .perform(get("/api/v1/profile"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.personalTargets.baseCaloriesKcal").doesNotExist())
        .andExpect(jsonPath("$.personalTargets.proteinTargetG").doesNotExist());
  }

  @Test
  void patchProfileFieldsReturnsUpdatedPayload() throws Exception {
    when(service.updateProfileFields(
            eq("Ada"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(profile("Ada", ThemeMode.DARK, false));

    mockMvc
        .perform(
            patch("/api/v1/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ada\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Ada"));
  }

  @Test
  void patchProfileFieldsWithInvalidSexReturnsValidationError() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sex\":\"NOT_A_SEX\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("sex"));
  }

  @Test
  void patchThemeReturnsUpdatedPayload() throws Exception {
    when(service.updateThemeMode(ThemeMode.LIGHT))
        .thenReturn(profile(null, ThemeMode.LIGHT, false));

    mockMvc
        .perform(
            patch("/api/v1/profile/theme")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"themeMode\":\"LIGHT\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.themeMode").value("LIGHT"));
  }

  @Test
  void patchThemeWithInvalidValueReturnsValidationError() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/profile/theme")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"themeMode\":\"NEON\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("themeMode"));
  }

  @Test
  void patchThemeWithMissingValueReturnsValidationError() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/profile/theme").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void patchUnitsWithInvalidValueReturnsValidationError() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/profile/units")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"weightUnit\":\"LB\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("weightUnit"));
  }

  @Test
  void patchObjectivesReturnsUpdatedPayload() throws Exception {
    UserProfile updated =
        new UserProfile(
            "default-user",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            UnitPreferences.DEFAULT,
            new DefaultObjectives(500.0, null, null),
            ThemeMode.DARK,
            OnboardingAnswers.EMPTY,
            false,
            ProfileBaseline.EMPTY,
            PersonalTargets.EMPTY);
    when(service.updateDefaultObjectives(any())).thenReturn(updated);

    mockMvc
        .perform(
            patch("/api/v1/profile/objectives")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"caloricDeficitKcal\":500}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.defaultObjectives.caloricDeficitKcal").value(500.0));
  }

  @Test
  void patchOnboardingReflectsFirstRunCompletedAndAnswers() throws Exception {
    UserProfile completed =
        new UserProfile(
            "default-user",
            null,
            null,
            null,
            null,
            null,
            null,
            MainGoal.COMPOSICION,
            UnitPreferences.DEFAULT,
            DefaultObjectives.EMPTY,
            ThemeMode.DARK,
            new OnboardingAnswers(
                new OnboardingAnswers.ProfileDraft("Ada", "1990-01-01", "FEMALE", "170"),
                null,
                new OnboardingAnswers.GoalDraft("COMPOSICION"),
                null,
                null,
                null),
            true,
            ProfileBaseline.EMPTY,
            PersonalTargets.EMPTY);
    when(service.submitOnboardingAnswers(any(), eq(true))).thenReturn(completed);

    mockMvc
        .perform(
            patch("/api/v1/profile/onboarding")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"profile\":{\"name\":\"Ada\",\"birthDate\":\"1990-01-01\","
                        + "\"sex\":\"FEMALE\",\"heightCm\":\"170\"},"
                        + "\"goal\":{\"selected\":\"COMPOSICION\"},\"completed\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstRunCompleted").value(true))
        .andExpect(jsonPath("$.onboardingAnswers.profile.name").value("Ada"))
        .andExpect(jsonPath("$.onboardingAnswers.goal.selected").value("COMPOSICION"));
  }

  @Test
  void patchOnboardingReSubmissionAfterCompletionIsAllowed() throws Exception {
    when(service.submitOnboardingAnswers(any(), eq(true)))
        .thenReturn(profile("Edited", ThemeMode.DARK, true));

    mockMvc
        .perform(
            patch("/api/v1/profile/onboarding")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"completed\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstRunCompleted").value(true));
  }
}
