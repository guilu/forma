import type { OnboardingAnswersOutput, UserProfile } from '../api/profile';

/**
 * Shared `UserProfile`/`OnboardingAnswersOutput` test fixtures (FOR-107,
 * FOR-121).
 *
 * <p>The "nothing saved yet" shape — every onboarding answer group at its
 * blank default, `firstRunCompleted: false`, `themeMode: 'DARK'` — was
 * copy-pasted as an identical literal across half a dozen test files
 * (`OnboardingGate`, `OnboardingPage`, `ThemeContext`, `ProfileSection`,
 * `UnitsSection`, `onboardingStorage`). SonarCloud's `duplicated_lines_density`
 * quality gate flagged the newest copy of it, but the duplication was already
 * spread across the suite — this is the one source of truth instead.
 *
 * <p>Kept deliberately minimal: only the fields every one of those files
 * actually shared. A test whose intent depends on a specific value (e.g. a
 * non-default `themeMode`, or `firstRunCompleted: true`) still sets it
 * explicitly at the call site via {@link baseProfile}'s `overrides` argument
 * rather than have it hidden inside this fixture.
 */
export const EMPTY_ONBOARDING_ANSWERS: OnboardingAnswersOutput = {
  profile: { name: '', birthDate: '', sex: '', heightCm: '' },
  metrics: { measurementSaved: false },
  goal: {},
  training: { days: [] },
  equipment: { items: [] },
  nutrition: { preference: '', restrictions: '' },
};

/**
 * A fresh/first-run `UserProfile` (FOR-107 Edge Cases default): no profile
 * fields saved, `unitPreferences` at the MVP's single supported value,
 * `themeMode: 'DARK'`, no onboarding progress, `firstRunCompleted: false`.
 * Pass `overrides` for anything a specific test needs to differ — spread
 * after the defaults, so an override always wins.
 */
export function baseProfile(overrides: Partial<UserProfile> = {}): UserProfile {
  return {
    unitPreferences: { weightUnit: 'KG', heightUnit: 'CM', distanceUnit: 'KM', energyUnit: 'KCAL' },
    themeMode: 'DARK',
    onboardingAnswers: EMPTY_ONBOARDING_ANSWERS,
    firstRunCompleted: false,
    ...overrides,
  };
}
