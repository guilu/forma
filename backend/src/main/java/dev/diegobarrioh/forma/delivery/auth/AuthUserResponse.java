package dev.diegobarrioh.forma.delivery.auth;

import dev.diegobarrioh.forma.domain.User;

/**
 * Response shape for {@code register}/{@code login}/{@code me} (FOR-145, ADR-012). Deliberately
 * never carries {@code password_hash} or any credential material (ADR-012 rule: "password_hash
 * never returned/logged").
 */
public record AuthUserResponse(String id, String email, String role) {

  public static AuthUserResponse from(User user) {
    // The role travels on the session's own user, so the SPA can decide what to offer without a
    // second request — and it is a *display* decision only: every restricted endpoint enforces the
    // authority server-side regardless of what the client believes (FOR-190).
    return new AuthUserResponse(user.id().toString(), user.email(), user.role().name());
  }
}
