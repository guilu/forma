package dev.diegobarrioh.forma.delivery.integrations;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/v1/integrations/{provider}/callback} (FOR-131 api.md): {@code
 * code}/{@code state} relayed by the SPA after the browser lands on its {@code /auth} route
 * (Withings redirects the browser there, not to the backend). Bean-validated at the API boundary
 * (docs/coding-standards.md) so a missing field is a 400 {@code VALIDATION_ERROR} before this ever
 * reaches {@link dev.diegobarrioh.forma.application.IntegrationService#callback}.
 *
 * <p>Never logged, never echoed back in a response (spec FOR-131 api.md: "Never log or echo code,
 * state, or any token") — this record has no {@code toString()} override because Spring never logs
 * request bodies by default in this codebase (ADR-008), but callers must still not pass this object
 * to a logger.
 *
 * @param code the authorization code Withings issued
 * @param state the CSRF state Withings round-tripped through the redirect
 */
public record CallbackRequest(@NotBlank String code, @NotBlank String state) {}
