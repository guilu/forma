package dev.diegobarrioh.forma.delivery.auth;

import dev.diegobarrioh.forma.application.CurrentUserProvider;
import dev.diegobarrioh.forma.application.UnauthorizedException;
import dev.diegobarrioh.forma.application.UserService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import dev.diegobarrioh.forma.delivery.security.FormaUserPrincipal;
import dev.diegobarrioh.forma.delivery.security.SessionAuthenticator;
import dev.diegobarrioh.forma.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registration/login/current-user endpoints (FOR-145, ADR-012) under {@link ApiPaths#V1}{@code
 * /auth}. Logout is handled entirely by the {@code SecurityFilterChain}'s {@code logout(...)}
 * configuration ({@code SecurityConfig}), not by a controller method here.
 *
 * <p>Login authenticates programmatically (JSON body, not Spring Security's default form-login
 * filter): it delegates credential verification to the {@link AuthenticationManager} the same way
 * the filter chain would, then persists the resulting {@link Authentication} into the HTTP session
 * itself via {@link SecurityContextRepository} — the documented pattern for a JSON login endpoint
 * (Spring Security 6). {@code request.changeSessionId()} rotates the session id on login
 * (session-fixation protection), matching what {@code SecurityConfig}'s {@code sessionFixation()}
 * does for the filter-driven path.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/auth")
public class AuthController {

  private final UserService userService;
  private final CurrentUserProvider currentUserProvider;
  private final AuthenticationManager authenticationManager;
  private final SessionAuthenticator sessionAuthenticator;

  public AuthController(
      UserService userService,
      CurrentUserProvider currentUserProvider,
      AuthenticationManager authenticationManager,
      SessionAuthenticator sessionAuthenticator) {
    this.userService = userService;
    this.currentUserProvider = currentUserProvider;
    this.authenticationManager = authenticationManager;
    this.sessionAuthenticator = sessionAuthenticator;
  }

  /** Public self-registration (FOR-145 spec: no invite/admin gate). Duplicate email -> 409. */
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public AuthUserResponse register(@Valid @RequestBody RegisterRequest request) {
    User user = userService.register(request.email(), request.password());
    return AuthUserResponse.from(user);
  }

  /**
   * Session-cookie login (FOR-145 spec: correct credentials set an httpOnly+Secure+SameSite=Lax
   * cookie and update {@code last_login_at}; wrong credentials -> 401, no cookie set).
   */
  @PostMapping("/login")
  public AuthUserResponse login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    Authentication authenticationResult;
    try {
      authenticationResult =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(request.email(), request.password()));
    } catch (AuthenticationException ex) {
      // Never echo the underlying reason (bad email vs bad password) — a single generic message
      // avoids leaking which part of the credential pair was wrong.
      throw new UnauthorizedException("Invalid credentials");
    }

    // Session-fixation protection + persisting the context: shared with GoogleOAuth2SuccessHandler
    // (SessionAuthenticator javadoc) — this endpoint authenticates programmatically rather than via
    // the filter chain, so it must do that same rotation/save itself.
    sessionAuthenticator.establishSession(authenticationResult, httpRequest, httpResponse);

    FormaUserPrincipal principal = (FormaUserPrincipal) authenticationResult.getPrincipal();
    userService.recordSuccessfulLogin(principal.id());
    User user = userService.findById(principal.id());
    return AuthUserResponse.from(user);
  }

  /** Returns the authenticated caller; also primes the CSRF cookie for the SPA (ADR-012). */
  @GetMapping("/me")
  public AuthUserResponse me() {
    User user = userService.findById(currentUserProvider.currentUserId());
    return AuthUserResponse.from(user);
  }
}
