package dev.diegobarrioh.forma.delivery.profile;

import dev.diegobarrioh.forma.application.UserProfileService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import dev.diegobarrioh.forma.domain.ActivityLevel;
import dev.diegobarrioh.forma.domain.DefaultObjectives;
import dev.diegobarrioh.forma.domain.DistanceUnit;
import dev.diegobarrioh.forma.domain.EnergyUnit;
import dev.diegobarrioh.forma.domain.HeightUnit;
import dev.diegobarrioh.forma.domain.MainGoal;
import dev.diegobarrioh.forma.domain.Sex;
import dev.diegobarrioh.forma.domain.ThemeMode;
import dev.diegobarrioh.forma.domain.UnitPreferences;
import dev.diegobarrioh.forma.domain.WeightUnit;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User profile & preferences REST endpoints (FOR-107) under {@link ApiPaths#V1}{@code /profile}:
 * read the whole aggregate (with sane defaults before any data is saved) and scoped updates for
 * profile fields, unit preferences, default objectives, theme preference and onboarding answers.
 *
 * <p>Thin controller (ADR-001, ADR-005): validates request DTOs, converts their known-value strings
 * to domain enums, and delegates all merge/persistence behavior to {@link UserProfileService}. It
 * never accepts or returns domain/persistence types directly. Validation failures become the
 * standard {@code VALIDATION_ERROR} response via the FOR-88 {@code GlobalExceptionHandler}.
 *
 * <p>Single-user MVP (ADR-002): every endpoint operates on the one account the service resolves
 * internally; no account/owner path segment or auth header is accepted yet. Not enforcing
 * authorization here is a known, documented MVP limitation, not an oversight (AGENTS.md Forbidden
 * Shortcuts still applies once authentication lands).
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/profile")
public class UserProfileController {

  private final UserProfileService service;

  public UserProfileController(UserProfileService service) {
    this.service = service;
  }

  /**
   * Returns the current profile & preferences, or sane defaults (dark theme, metric units,
   * incomplete onboarding) when no data has been saved yet — never a 404 (spec FOR-107 Edge Cases).
   */
  @GetMapping
  public UserProfileResponse get() {
    return UserProfileResponse.from(service.get());
  }

  /** Partially updates the "Profile fields" section; omitted fields are left unchanged. */
  @PatchMapping
  public UserProfileResponse updateProfileFields(
      @Valid @RequestBody UpdateProfileFieldsRequest request) {
    return UserProfileResponse.from(
        service.updateProfileFields(
            request.name(),
            request.email(),
            request.birthDate(),
            request.sex() == null ? null : Sex.valueOf(request.sex()),
            request.heightCm(),
            request.activityLevel() == null ? null : ActivityLevel.valueOf(request.activityLevel()),
            request.mainGoal() == null ? null : MainGoal.valueOf(request.mainGoal())));
  }

  /** Partially updates unit preferences; omitted fields are left unchanged. */
  @PatchMapping("/units")
  public UserProfileResponse updateUnitPreferences(
      @Valid @RequestBody UpdateUnitPreferencesRequest request) {
    UnitPreferences requested =
        new UnitPreferences(
            request.weightUnit() == null ? null : WeightUnit.valueOf(request.weightUnit()),
            request.heightUnit() == null ? null : HeightUnit.valueOf(request.heightUnit()),
            request.distanceUnit() == null ? null : DistanceUnit.valueOf(request.distanceUnit()),
            request.energyUnit() == null ? null : EnergyUnit.valueOf(request.energyUnit()));
    return UserProfileResponse.from(service.updateUnitPreferences(requested));
  }

  /** Partially updates default objectives; omitted fields are left unchanged. */
  @PatchMapping("/objectives")
  public UserProfileResponse updateDefaultObjectives(
      @Valid @RequestBody UpdateDefaultObjectivesRequest request) {
    DefaultObjectives requested =
        new DefaultObjectives(
            request.caloricDeficitKcal(), request.proteinTargetG(), request.dailyWaterMl());
    return UserProfileResponse.from(service.updateDefaultObjectives(requested));
  }

  /** Updates the theme preference (single-valued; {@code themeMode} is required). */
  @PatchMapping("/theme")
  public UserProfileResponse updateThemeMode(@Valid @RequestBody UpdateThemeModeRequest request) {
    return UserProfileResponse.from(
        service.updateThemeMode(ThemeMode.valueOf(request.themeMode())));
  }

  /**
   * Submits the onboarding draft (full replace, not a partial merge — see {@link
   * SubmitOnboardingAnswersRequest}) and the {@code completed} flag.
   */
  @PatchMapping("/onboarding")
  public UserProfileResponse submitOnboardingAnswers(
      @Valid @RequestBody SubmitOnboardingAnswersRequest request) {
    return UserProfileResponse.from(
        service.submitOnboardingAnswers(request.toDomain(), request.completed()));
  }
}
