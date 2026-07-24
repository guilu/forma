package dev.diegobarrioh.forma.delivery.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.application.UnauthorizedException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for {@link SecurityContextCurrentUserProvider} (FOR-145, ADR-012 spec "Principal
 * resolution via CurrentUserProvider"). No Spring context needed — manipulates {@link
 * SecurityContextHolder} directly, like the port's own contract.
 */
class SecurityContextCurrentUserProviderTest {

  private final SecurityContextCurrentUserProvider provider =
      new SecurityContextCurrentUserProvider();

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void resolvesTheAuthenticatedPrincipalsAccountId() {
    UUID id = UUID.randomUUID();
    FormaUserPrincipal principal = new FormaUserPrincipal(id, "a@x.com", "{argon2}hash", true);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

    assertThat(provider.currentUserId()).isEqualTo(id);
  }

  @Test
  void throwsUnauthorizedWhenNoAuthenticationIsPresent() {
    SecurityContextHolder.clearContext();

    assertThatThrownBy(provider::currentUserId).isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void throwsUnauthorizedWhenAuthenticatedButNotAFormaUserPrincipal() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    assertThatThrownBy(provider::currentUserId).isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void throwsUnauthorizedWhenTheAuthenticationIsMarkedNotAuthenticated() {
    UUID id = UUID.randomUUID();
    FormaUserPrincipal principal = new FormaUserPrincipal(id, "a@x.com", "{argon2}hash", true);
    UsernamePasswordAuthenticationToken notAuthenticated =
        new UsernamePasswordAuthenticationToken(principal, "credentials");
    assertThat(notAuthenticated.isAuthenticated()).isFalse();
    SecurityContextHolder.getContext().setAuthentication(notAuthenticated);

    assertThatThrownBy(provider::currentUserId).isInstanceOf(UnauthorizedException.class);
  }
}
