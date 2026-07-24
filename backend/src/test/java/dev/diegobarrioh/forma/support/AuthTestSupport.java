package dev.diegobarrioh.forma.support;

import dev.diegobarrioh.forma.bootstrap.LegacyUserBootstrap;
import dev.diegobarrioh.forma.delivery.security.FormaUserPrincipal;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Authenticates {@code MockMvc} requests as a real {@link FormaUserPrincipal} (FOR-145, ADR-012
 * design §8 "Test helper"), so both new auth tests and (in a later slice, 145b+) migrated
 * pre-existing single-user tests can authenticate without going through the full HTTP
 * register/login flow for every request.
 */
public final class AuthTestSupport {

  private AuthTestSupport() {}

  /** Authenticates as an arbitrary account id/email (e.g. a freshly registered test user). */
  public static RequestPostProcessor asUser(UUID id, String email) {
    return SecurityMockMvcRequestPostProcessors.user(
        new FormaUserPrincipal(id, email, "{noop}unused", true));
  }

  /**
   * Authenticates as the seeded legacy placeholder account (migration V26 / {@link
   * LegacyUserBootstrap#PLACEHOLDER_USER_ID}), letting single-user-era tests keep passing post-auth
   * since legacy data stays scoped to this same id.
   */
  public static RequestPostProcessor asPlaceholderUser() {
    return asUser(LegacyUserBootstrap.PLACEHOLDER_USER_ID, "legacy@forma.local");
  }

  /**
   * Sets {@code SecurityContextHolder} directly for tests that call application services on the
   * current thread WITHOUT going through {@code MockMvc} (FOR-145b-1: {@code @SpringBootTest}
   * classes autowiring a service directly, e.g. {@code MealLogConsumptionPersistenceTest}) — {@link
   * #asUser}/{@link #asPlaceholderUser} only affect requests dispatched through {@code MockMvc}'s
   * request post-processor chain, never a direct in-process method call. Callers MUST clear it in
   * an {@code @AfterEach} via {@link SecurityContextHolder#clearContext()} to avoid leaking the
   * authentication across tests.
   */
  public static void authenticateThreadAs(UUID id, String email) {
    FormaUserPrincipal principal = new FormaUserPrincipal(id, email, "{noop}unused", true);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                principal, principal.getPassword(), principal.getAuthorities()));
  }

  /** Same-thread equivalent of {@link #asPlaceholderUser()} — see {@link #authenticateThreadAs}. */
  public static void authenticateThreadAsPlaceholderUser() {
    authenticateThreadAs(LegacyUserBootstrap.PLACEHOLDER_USER_ID, "legacy@forma.local");
  }
}
