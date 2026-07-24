package dev.diegobarrioh.forma.delivery.auth;

import dev.diegobarrioh.forma.domain.User;

/**
 * Response shape for {@code register}/{@code login}/{@code me} (FOR-145, ADR-012). Deliberately
 * never carries {@code password_hash} or any credential material (ADR-012 rule: "password_hash
 * never returned/logged").
 */
public record AuthUserResponse(String id, String email) {

  public static AuthUserResponse from(User user) {
    return new AuthUserResponse(user.id().toString(), user.email());
  }
}
