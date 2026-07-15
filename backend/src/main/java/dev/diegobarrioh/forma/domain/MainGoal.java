package dev.diegobarrioh.forma.domain;

/**
 * The user's main training/health goal (FOR-107, spec FOR-58's Ajustes mockup).
 *
 * <p>Values match the frontend onboarding's {@code GoalOption} union ({@code
 * frontend/src/pages/onboarding/onboardingStorage.ts}, FOR-59) so the profile's canonical goal and
 * the onboarding draft answer speak the same vocabulary.
 */
public enum MainGoal {
  COMPOSICION,
  RENDIMIENTO,
  HABITO
}
