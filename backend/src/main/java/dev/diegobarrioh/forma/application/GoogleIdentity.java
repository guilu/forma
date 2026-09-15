package dev.diegobarrioh.forma.application;

/**
 * The claims {@link UserService#loginWithGoogle(GoogleIdentity)} needs out of a Google OIDC login,
 * decoupled from any Spring Security/OAuth2 type (ADR-001: the application layer stays
 * framework-free). The delivery layer ({@code GoogleOAuth2SuccessHandler}) maps the authenticated
 * {@code OidcUser}'s claims into this before calling the use case.
 *
 * @param subject the Google "sub" claim — a stable, never-reused identifier for the Google account;
 *     the join key back to a FORMA account, deliberately never the email (which a Google account
 *     holder could change at the provider)
 * @param email the Google account's email address
 * @param emailVerified Google's own "email_verified" claim; {@code false} rejects the login
 *     outright (a FORMA account is never created or linked from an unverified address)
 */
public record GoogleIdentity(String subject, String email, boolean emailVerified) {}
