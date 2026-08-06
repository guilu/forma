package dev.diegobarrioh.forma.delivery.generator;

import dev.diegobarrioh.forma.domain.EnergyRequirement;

/**
 * The three figures the funnel shows, each with the step that produced it.
 *
 * <p>Sent whole rather than as one number because the screen explains the arithmetic — basal, then
 * movement, then the objective — and an explanation the client has to reassemble is one the client
 * can get wrong.
 */
public record EnergyRequirementResponse(
    int basalKcal, double activityFactor, int dailyKcal, double objectiveFactor, int planKcal) {

  static EnergyRequirementResponse from(EnergyRequirement requirement) {
    return new EnergyRequirementResponse(
        requirement.basalKcal(),
        requirement.activityFactor(),
        requirement.dailyKcal(),
        requirement.objectiveFactor(),
        requirement.planKcal());
  }
}
