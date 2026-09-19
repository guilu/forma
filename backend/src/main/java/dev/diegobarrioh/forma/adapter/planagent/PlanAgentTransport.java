package dev.diegobarrioh.forma.adapter.planagent;

/**
 * Minimal HTTP transport seam for {@link PlanAgentAdapter}, in the same spirit as {@code
 * adapter/withings}'s {@code WithingsHttpTransport}: exists purely so tests substitute a
 * fixture-backed fake instead of performing a real network call (ADR-015 slice 5: "no live call to
 * anything"). {@link JdkHttpPlanAgentTransport} is the only production implementation.
 *
 * <p>Unlike Withings' form-encoded transport, the plan agent contract is JSON in both directions
 * ({@code docs/FORMA_Contrato_Agente_Plan.md}), so this seam carries a single JSON string body
 * rather than a form-parameter map.
 */
public interface PlanAgentTransport {

  /**
   * POSTs {@code jsonBody} as {@code application/json} to {@code url}, presenting {@code apiKey} as
   * bearer credentials when non-blank, and returns the raw response body.
   *
   * <p>Never logs {@code apiKey} or either body (ADR-008).
   *
   * @throws RuntimeException if the request cannot be completed (unreachable, times out, non-2xx
   *     HTTP status, ...)
   */
  String post(String url, String apiKey, String jsonBody);
}
