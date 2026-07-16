package dev.diegobarrioh.forma.application;

/**
 * Thrown when an OAuth callback presents a {@code state} that does not match a stored, unexpired,
 * unconsumed {@link OAuthChallenge} (FOR-131 Edge Cases: "mismatched/expired/replayed state →
 * reject, no connection created, no tokens stored"). The delivery layer maps it to a {@code
 * VALIDATION_ERROR} (400) {@code ApiError}, mirroring {@link ValidationException}.
 *
 * <p>The message is always a fixed, generic string — never the caller-supplied {@code state} value
 * itself (spec FOR-131 api.md: "Never log or echo code, state, or any token").
 */
public class OAuthStateException extends RuntimeException {
  public OAuthStateException() {
    super("La autorización expiró o ya fue utilizada. Vuelve a intentar la conexión.");
  }
}
