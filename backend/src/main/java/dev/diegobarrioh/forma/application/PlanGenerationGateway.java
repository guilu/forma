package dev.diegobarrioh.forma.application;

/**
 * Provider-neutral port for asking an external AI agent to write a nutrition plan (ADR-015 decision
 * 10), mirroring how {@link ProviderOAuthGateway}/{@link ProviderMeasuresGateway} sit in front of
 * Withings (ADR-004: "External integrations will be implemented as adapters behind provider-neutral
 * application ports"). {@code adapter/planagent}'s {@code PlanAgentAdapter} is the only
 * implementation this slice registers.
 *
 * <p>No provider type — no provider JSON, no Jackson tree, no HTTP status — appears anywhere in
 * this interface's signature, only {@link PlanGenerationCommand} and {@link
 * GeneratedNutritionPlan}: this is ADR-004's rule, stated the same way in {@link
 * ProviderOAuthGateway} and {@link ProviderMeasuresGateway}'s own javadoc. Mapping the agent's wire
 * format to and from these types happens entirely inside the adapter.
 *
 * <p><b>Not wired into anything yet, deliberately.</b> No dispatcher, no scheduled job, no call
 * from any {@code *Service} exists in this slice (ADR-015 implementation plan, slice 5 vs. slice
 * 6). This port and its adapter are built to be consumed, not to be consumed by this slice.
 *
 * <p><b>No retry.</b> There is no retry or backoff anywhere in this codebase (verified across every
 * existing adapter) and this port adds none — a failed call is a {@link PlanGenerationException}
 * the caller sees once, matching ADR-015 decision 10's explicit "no retry, still".
 */
public interface PlanGenerationGateway {

  /**
   * Asks the agent to write a plan for {@code command}, and returns it parsed into FORMA's own
   * types.
   *
   * <p>Never recomputes anything the command already claims ({@code targets.planKcal} in, the
   * agent's own claimed macros back out unchanged) — this call is a translation, not a validation;
   * checking whether the agent's numbers add up is {@link PlanToleranceAuditor}'s job, run
   * separately, after this returns (ADR-015 decision 9).
   *
   * @throws PlanGenerationException if the agent is not configured, is unreachable, times out,
   *     signals an error, or returns a response that cannot be parsed into a usable plan
   */
  GeneratedNutritionPlan generate(PlanGenerationCommand command);
}
