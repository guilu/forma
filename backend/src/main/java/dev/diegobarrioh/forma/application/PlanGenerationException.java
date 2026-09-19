package dev.diegobarrioh.forma.application;

/**
 * Thrown by a {@link PlanGenerationGateway} when asking the plan agent for a plan fails (ADR-015
 * decision 10) — the agent is unreachable, not configured, times out, signals an error, or returns
 * a response this adapter cannot parse into a usable plan.
 *
 * <p>Mirrors {@link ProviderSyncException}/{@link ProviderOAuthException}: the message is always a
 * safe, generic summary. Constructors deliberately do not accept the raw request/response body, the
 * agent's API key, or any other secret, so a future call site cannot accidentally leak one into
 * this exception's message (ADR-008).
 */
public class PlanGenerationException extends RuntimeException {
  public PlanGenerationException(String safeMessage) {
    super(safeMessage);
  }

  public PlanGenerationException(String safeMessage, Throwable cause) {
    super(safeMessage, cause);
  }
}
