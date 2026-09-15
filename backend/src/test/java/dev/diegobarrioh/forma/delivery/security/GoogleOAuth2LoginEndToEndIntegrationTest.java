package dev.diegobarrioh.forma.delivery.security;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.bootstrap.LegacyUserBootstrap;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * End-to-end test of the Google login success path (finding #4 of the fresh-review fixes on
 * ADR-014): drives the <em>real</em> {@link GoogleOAuth2SuccessHandler} bean — real {@code
 * UserService}, real {@code JdbcUserRepository} against the H2 test database, real {@code
 * SessionAuthenticator}, real {@code SecurityContextRepository} — with a real {@link
 * OAuth2AuthenticationToken} wrapping a real {@link DefaultOidcUser}, and checks every promise the
 * handler makes: the account lands in the database with its Google subject, the session id is
 * rotated, the persisted {@code SecurityContext}'s principal is a {@link FormaUserPrincipal}, and
 * the redirect goes to the app.
 *
 * <p><b>Why not a real HTTP round trip via {@code MockMvc}/{@code TestRestTemplate} all the way to
 * {@code GET /api/v1/auth/me}</b> (the "better still" suggestion): that would need either (a) a
 * session built by hand (as this test does) handed to a <em>different</em>, later HTTP request —
 * impossible with {@link SpringBootTest.WebEnvironment#RANDOM_PORT}'s real embedded Tomcat, whose
 * session store is entirely separate from a manually constructed {@link MockHttpServletRequest}; a
 * mock session is not a row Tomcat's session manager knows about, so there is no cookie value that
 * would ever resolve back to it — or (b) actually exercising Google's authorization/token/userinfo
 * endpoints over HTTP, which needs a local OIDC-compliant stub server (signed ID tokens, JWKS) that
 * does not exist in this project and is not "simple" to stand up correctly — explicitly out of
 * scope per the task notes ("do NOT mock the Google token endpoint unless it turns out to be
 * simple"). This test gets the closest practical equivalent instead: after calling the handler, it
 * takes the resulting session and asks the <em>real</em> {@code SecurityContextRepository} bean —
 * the exact same one a subsequent request's {@code SecurityContextHolderFilter} would ask — to load
 * the context back from it, proving a follow-up request presenting that session would in fact see
 * the {@code FormaUserPrincipal}, which is what {@code GET /api/v1/auth/me} depends on.
 */
@SpringBootTest
@ActiveProfiles("test")
class GoogleOAuth2LoginEndToEndIntegrationTest {

  @DynamicPropertySource
  static void googleOAuth2Credentials(DynamicPropertyRegistry registry) {
    registry.add("forma.oauth2.google.client-id", () -> "test-google-client-id");
    registry.add("forma.oauth2.google.client-secret", () -> "test-google-client-secret");
  }

  @Autowired private GoogleOAuth2SuccessHandler successHandler;
  @Autowired private SecurityContextRepository securityContextRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearTestUsersAndSecurityContext() {
    jdbcTemplate.update("DELETE FROM users WHERE id <> ?", LegacyUserBootstrap.PLACEHOLDER_USER_ID);
    SecurityContextHolder.clearContext();
  }

  private static OAuth2AuthenticationToken googleAuthenticationToken(
      String subject, String email, boolean emailVerified) {
    Instant issuedAt = LocalDateTime.of(2026, 1, 1, 0, 0).toInstant(ZoneOffset.UTC);
    OidcIdToken idToken =
        new OidcIdToken(
            "id-token-value",
            issuedAt,
            issuedAt.plusSeconds(3600),
            Map.of(
                IdTokenClaimNames.SUB,
                subject,
                IdTokenClaimNames.ISS,
                java.net.URI.create("https://accounts.google.com"),
                "email",
                email,
                "email_verified",
                emailVerified));
    OidcUser oidcUser = new DefaultOidcUser(List.of(), idToken);
    return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
  }

  @Test
  void establishesARealAuthenticatedSessionAndPersistsTheAccount() throws Exception {
    String subject = "e2e-google-sub-" + UUID.randomUUID();
    String email = "e2e." + UUID.randomUUID() + "@x.com";
    MockHttpServletRequest request = new MockHttpServletRequest();
    HttpSession originalSession = request.getSession(true);
    String originalSessionId = originalSession.getId();
    MockHttpServletResponse response = new MockHttpServletResponse();

    successHandler.onAuthenticationSuccess(
        request, response, googleAuthenticationToken(subject, email, true));

    // 1) The account was actually created and linked in the database.
    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "SELECT google_subject, is_active, role FROM users WHERE email = ?", email);
    assertThat(row.get("google_subject")).isEqualTo(subject);
    assertThat(row.get("is_active")).isEqualTo(true);
    assertThat(row.get("role")).isEqualTo("USER");

    // 2) Session-fixation protection rotated the session id.
    HttpSession sessionAfter = request.getSession(false);
    assertThat(sessionAfter).isNotNull();
    assertThat(sessionAfter.getId()).isNotEqualTo(originalSessionId);

    // 3) The thread-local SecurityContext right after the call carries a FormaUserPrincipal.
    Authentication threadLocalAuth = SecurityContextHolder.getContext().getAuthentication();
    assertThat(threadLocalAuth).isNotNull();
    assertThat(threadLocalAuth.getPrincipal()).isInstanceOf(FormaUserPrincipal.class);
    assertThat(((FormaUserPrincipal) threadLocalAuth.getPrincipal()).getUsername())
        .isEqualTo(email);

    // 4) The SAME session, handed to the real SecurityContextRepository bean on a fresh request —
    // the same lookup a follow-up GET /api/v1/auth/me would perform — resolves to that same
    // FormaUserPrincipal. This is the closest practical proof, without a real Tomcat session store
    // wired to this mock request, that a subsequent request would be authenticated (see class
    // javadoc for why a literal MockMvc/TestRestTemplate round trip is impractical here).
    MockHttpServletRequest followUpRequest = new MockHttpServletRequest();
    followUpRequest.setSession(sessionAfter);
    SecurityContext reloadedContext =
        securityContextRepository.loadDeferredContext(followUpRequest).get();
    assertThat(reloadedContext.getAuthentication()).isNotNull();
    assertThat(reloadedContext.getAuthentication().getPrincipal())
        .isInstanceOf(FormaUserPrincipal.class);
    assertThat(
            ((FormaUserPrincipal) reloadedContext.getAuthentication().getPrincipal()).getUsername())
        .isEqualTo(email);

    // 5) Redirected to the SPA's default authenticated destination.
    assertThat(response.getRedirectedUrl()).isEqualTo("/app");
  }

  @Test
  void aRejectedLoginNeverCreatesAnAccountAndLeavesNoAuthenticatedSecurityContext()
      throws Exception {
    String subject = "e2e-google-sub-rejected-" + UUID.randomUUID();
    String email = "e2e.rejected." + UUID.randomUUID() + "@x.com";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.getSession(true);
    MockHttpServletResponse response = new MockHttpServletResponse();

    // email_verified: false must never create or touch an account.
    successHandler.onAuthenticationSuccess(
        request, response, googleAuthenticationToken(subject, email, false));

    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
    assertThat(count).isEqualTo(0);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(response.getRedirectedUrl()).isEqualTo("/login?error=google");
  }
}
