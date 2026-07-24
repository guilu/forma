package dev.diegobarrioh.forma.delivery.auth;

import dev.diegobarrioh.forma.application.UserService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for {@code POST /api/v1/auth/register} (FOR-145, ADR-012). */
public record RegisterRequest(
    @Email @NotBlank String email,
    @NotBlank @Size(min = UserService.MIN_PASSWORD_LENGTH, max = 200) String password) {}
