package dev.diegobarrioh.forma.application;

/**
 * Thrown by a {@link ProviderOAuthGateway} when a provider OAuth call (authorization URL is a pure
 * function and cannot fail this way, but token exchange/refresh are real network calls) fails — the
 * provider is unreachable, returns an error status, or returns a response this adapter cannot parse
 * (spec FOR-131 Edge Cases: "Token exchange failure → connection not marked CONNECTED, readable
 * outcome, no secret leak"; "Refresh failure → mark connection needing re-auth"). The delivery
 * layer maps it to a 502 {@code ApiError}.
 *
 * <p>The message is always a safe, generic summary — constructors deliberately do not accept the
 * raw provider response body, an authorization {@code code}, or a token, so a future call site
 * cannot accidentally leak one into this exception's message (which server logs and, via {@code
 * GlobalExceptionHandler}, is never returned to the client anyway — this is defense in depth, spec
 * FOR-131 api.md: "no secret in the body").
 */
public class ProviderOAuthException extends RuntimeException {
  public ProviderOAuthException(String safeMessage) {
    super(safeMessage);
  }

  public ProviderOAuthException(String safeMessage, Throwable cause) {
    super(safeMessage, cause);
  }
}
