package dev.diegobarrioh.forma.delivery.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.diegobarrioh.forma.application.GoogleIdentity;
import dev.diegobarrioh.forma.application.UnauthorizedException;
import dev.diegobarrioh.forma.application.UserService;
import dev.diegobarrioh.forma.domain.User;
import dev.diegobarrioh.forma.domain.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Unit tests for {@link GoogleOAuth2SuccessHandler} (ADR-012 addendum): maps the authenticated
 * {@link OidcUser}'s claims into a {@link GoogleIdentity}, resolves the account, establishes the
 * session via {@link SessionAuthenticator}, and redirects to the SPA.
 */
class GoogleOAuth2SuccessHandlerTest {

  private static OidcUser oidcUser(String sub, String email, boolean emailVerified) {
    Instant issuedAt = LocalDateTime.of(2026, 1, 1, 0, 0).toInstant(ZoneOffset.UTC);
    Instant expiresAt = issuedAt.plusSeconds(3600);
    OidcIdToken idToken =
        new OidcIdToken(
            "id-token-value",
            issuedAt,
            expiresAt,
            Map.of(
                IdTokenClaimNames.SUB,
                sub,
                IdTokenClaimNames.ISS,
                java.net.URI.create("https://accounts.google.com"),
                "email",
                email,
                "email_verified",
                emailVerified));
    return new DefaultOidcUser(java.util.List.of(), idToken);
  }

  @Test
  void mapsTheOidcUserToAGoogleIdentityEstablishesTheSessionAndRedirectsToTheSpa()
      throws Exception {
    UserService userService = mock(UserService.class);
    SessionAuthenticator sessionAuthenticator = mock(SessionAuthenticator.class);
    GoogleOAuth2SuccessHandler handler =
        new GoogleOAuth2SuccessHandler(
            userService,
            sessionAuthenticator,
            new FrontendRedirectResolver("http://localhost:5173"));
    UUID userId = UUID.randomUUID();
    User user =
        new User(userId, "a@x.com", null, Instant.now(), null, true, UserRole.USER, "google-sub-1");
    when(userService.loginWithGoogle(any())).thenReturn(user);
    Authentication builtAuthentication = mock(Authentication.class);
    when(sessionAuthenticator.authenticationFor(user)).thenReturn(builtAuthentication);

    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(oidcUser("google-sub-1", "a@x.com", true));
    HttpServletRequest request = mock(HttpServletRequest.class);
    RecordingResponse response = new RecordingResponse();

    handler.onAuthenticationSuccess(request, response, authentication);

    org.mockito.ArgumentCaptor<GoogleIdentity> captor =
        org.mockito.ArgumentCaptor.forClass(GoogleIdentity.class);
    verify(userService, times(1)).loginWithGoogle(captor.capture());
    assertThat(captor.getValue().subject()).isEqualTo("google-sub-1");
    assertThat(captor.getValue().email()).isEqualTo("a@x.com");
    assertThat(captor.getValue().emailVerified()).isTrue();

    verify(sessionAuthenticator, times(1)).establishSession(builtAuthentication, request, response);
    verify(userService, times(1)).recordSuccessfulLogin(userId);
    assertThat(response.redirectedTo).isEqualTo("http://localhost:5173/app");
  }

  @Test
  void withNoConfiguredFrontendUrlRedirectsToARelativePath() throws Exception {
    UserService userService = mock(UserService.class);
    SessionAuthenticator sessionAuthenticator = mock(SessionAuthenticator.class);
    GoogleOAuth2SuccessHandler handler =
        new GoogleOAuth2SuccessHandler(
            userService, sessionAuthenticator, new FrontendRedirectResolver(""));
    User user =
        new User(
            UUID.randomUUID(),
            "a@x.com",
            null,
            Instant.now(),
            null,
            true,
            UserRole.USER,
            "google-sub-1");
    when(userService.loginWithGoogle(any())).thenReturn(user);
    when(sessionAuthenticator.authenticationFor(user)).thenReturn(mock(Authentication.class));
    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(oidcUser("google-sub-1", "a@x.com", true));
    HttpServletRequest request = mock(HttpServletRequest.class);
    // DefaultRedirectStrategy prefixes a relative target with the request's context path (empty in
    // a real request against this app's root-mounted servlet) before handing it to sendRedirect.
    when(request.getContextPath()).thenReturn("");
    RecordingResponse response = new RecordingResponse();

    handler.onAuthenticationSuccess(request, response, authentication);

    assertThat(response.redirectedTo).isEqualTo("/app");
  }

  /**
   * Regression test (finding #2 of the fresh-review fixes on ADR-014): {@code
   * AbstractAuthenticationProcessingFilter} only catches {@code AuthenticationException} around
   * {@code successfulAuthentication} — an {@link UnauthorizedException} from {@code
   * UserService#loginWithGoogle} (unverified email, inactive account, re-link conflict) would
   * otherwise propagate out of the success handler and surface to the browser as a bare 500. The
   * handler must catch it, tear down the half-built session Spring Security's filter already saved
   * (the raw OidcUser-principal {@code Authentication}, from before our handler ever ran), and
   * redirect to the same {@code ?error=google} page the failure handler uses — with nothing from
   * the exception itself in the URL.
   */
  @Test
  void aRejectionFromLoginWithGoogleClearsTheSessionAndRedirectsToTheErrorPageInsteadOfThrowing()
      throws Exception {
    UserService userService = mock(UserService.class);
    SessionAuthenticator sessionAuthenticator = mock(SessionAuthenticator.class);
    GoogleOAuth2SuccessHandler handler =
        new GoogleOAuth2SuccessHandler(
            userService,
            sessionAuthenticator,
            new FrontendRedirectResolver("http://localhost:5173"));
    when(userService.loginWithGoogle(any()))
        .thenThrow(new UnauthorizedException("El email de Google no está verificado"));
    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(oidcUser("google-sub-1", "a@x.com", false));
    HttpServletRequest request = mock(HttpServletRequest.class);
    RecordingResponse response = new RecordingResponse();

    handler.onAuthenticationSuccess(request, response, authentication);

    assertThat(response.redirectedTo).isEqualTo("http://localhost:5173/login?error=google");
    verify(sessionAuthenticator, times(1)).clearSession(request);
    verify(sessionAuthenticator, never()).establishSession(any(), any(), any());
    verify(userService, never()).recordSuccessfulLogin(any());
  }

  /**
   * A minimal recording fake — the real {@code sendRedirect} contract is just "where to". Wraps a
   * mock so {@code SimpleUrlAuthenticationSuccessHandler}'s {@code DefaultRedirectStrategy} (which
   * calls {@code encodeRedirectURL} before {@code sendRedirect}) has something that echoes the URL
   * back instead of Mockito's default {@code null}.
   */
  private static final class RecordingResponse
      extends jakarta.servlet.http.HttpServletResponseWrapper {
    String redirectedTo;

    RecordingResponse() {
      super(passthroughEncodingMock());
    }

    private static HttpServletResponse passthroughEncodingMock() {
      HttpServletResponse wrapped = mock(HttpServletResponse.class);
      when(wrapped.encodeRedirectURL(org.mockito.ArgumentMatchers.anyString()))
          .thenAnswer(invocation -> invocation.getArgument(0));
      return wrapped;
    }

    @Override
    public void sendRedirect(String location) {
      this.redirectedTo = location;
    }
  }
}
