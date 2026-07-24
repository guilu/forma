package dev.diegobarrioh.forma.delivery.auth;

import jakarta.validation.constraints.NotBlank;

/** Request body for {@code POST /api/v1/auth/login} (FOR-145, ADR-012). */
public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
