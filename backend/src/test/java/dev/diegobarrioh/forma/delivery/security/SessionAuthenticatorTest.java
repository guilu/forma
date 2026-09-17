package dev.diegobarrioh.forma.delivery.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.diegobarrioh.forma.domain.User;
import dev.diegobarrioh.forma.domain.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Unit tests for {@link SessionAuthenticator} (ADR-012), the component both {@code
 * AuthController#login} and {@code GoogleOAuth2SuccessHandler} share to establish a session.
 */
class SessionAuthenticatorTest {

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void authenticationForBuildsAFormaUserPrincipalWithNoCredentials() {
    SecurityContextRepository repository = mock(SecurityContextRepository.class);
    SessionAuthenticator authenticator = new SessionAuthenticator(repository);
    User user =
        new User(
            UUID.randomUUID(),
            "a@x.com",
            null,
            Instant.now(),
            null,
            true,
            UserRole.USER,
            "google-sub");

    Authentication authentication = authenticator.authenticationFor(user);

    assertThat(authentication.getPrincipal()).isInstanceOf(FormaUserPrincipal.class);
    assertThat(authentication.getCredentials()).isNull();
    assertThat(((FormaUserPrincipal) authentication.getPrincipal()).id()).isEqualTo(user.id());
  }

  @Test
  void establishSessionSavesTheContextWithoutRotatingWhenNoSessionExists() {
    SecurityContextRepository repository = mock(SecurityContextRepository.class);
    SessionAuthenticator authenticator = new SessionAuthenticator(repository);
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(request.getSession(false)).thenReturn(null);
    Authentication authentication =
        authenticator.authenticationFor(
            new User(
                UUID.randomUUID(),
                "a@x.com",
                null,
                Instant.now(),
                null,
                true,
                UserRole.USER,
                null));

    authenticator.establishSession(authentication, request, response);

    verify(request, never()).changeSessionId();
    verify(repository, times(1)).saveContext(any(), eq(request), eq(response));
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
  }

  @Test
  void establishSessionRotatesTheSessionIdWhenOneAlreadyExists() {
    SecurityContextRepository repository = mock(SecurityContextRepository.class);
    SessionAuthenticator authenticator = new SessionAuthenticator(repository);
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(request.getSession(false)).thenReturn(mock(HttpSession.class));
    Authentication authentication =
        authenticator.authenticationFor(
            new User(
                UUID.randomUUID(),
                "a@x.com",
                null,
                Instant.now(),
                null,
                true,
                UserRole.USER,
                null));

    authenticator.establishSession(authentication, request, response);

    verify(request, times(1)).changeSessionId();
  }

  /**
   * Regression test (finding #2 of the fresh-review fixes on ADR-014): {@code
   * GoogleOAuth2SuccessHandler} must be able to tear down a half-built session — the raw
   * OAuth2AuthenticationToken/OidcUser Spring Security's filter already saved before our handler
   * ran — when {@code UserService#loginWithGoogle} rejects the login. No trace of that
   * not-a-FormaUserPrincipal Authentication may survive: neither in the thread-local context nor in
   * the session.
   */
  @Test
  void clearSessionClearsTheContextAndInvalidatesAnExistingSession() {
    SecurityContextRepository repository = mock(SecurityContextRepository.class);
    SessionAuthenticator authenticator = new SessionAuthenticator(repository);
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);
    when(request.getSession(false)).thenReturn(session);
    SecurityContextHolder.getContext()
        .setAuthentication(
            authenticator.authenticationFor(
                new User(
                    UUID.randomUUID(),
                    "a@x.com",
                    null,
                    Instant.now(),
                    null,
                    true,
                    UserRole.USER,
                    null)));

    authenticator.clearSession(request);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(session, times(1)).invalidate();
  }

  @Test
  void clearSessionIsANoOpWhenNoSessionExists() {
    SecurityContextRepository repository = mock(SecurityContextRepository.class);
    SessionAuthenticator authenticator = new SessionAuthenticator(repository);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getSession(false)).thenReturn(null);

    authenticator.clearSession(request);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
