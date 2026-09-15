package dev.diegobarrioh.forma.config;

import dev.diegobarrioh.forma.delivery.ApiPaths;
import dev.diegobarrioh.forma.delivery.security.ApiAccessDeniedHandler;
import dev.diegobarrioh.forma.delivery.security.ApiAuthenticationEntryPoint;
import dev.diegobarrioh.forma.delivery.security.CsrfCookieFilter;
import dev.diegobarrioh.forma.delivery.security.FrontendRedirectResolver;
import dev.diegobarrioh.forma.delivery.security.GoogleOAuth2SuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security wiring for session-cookie authentication (FOR-145, ADR-012). See the ADR for the
 * full rationale; this class implements its "Spring Security filter chain" section.
 */
@Configuration
// Turns on @PreAuthorize (FOR-190): the catalog maintenance endpoints are the first rules that
// depend on an authority rather than merely on being authenticated.
@EnableMethodSecurity
@EnableWebSecurity
public class SecurityConfig {

  private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

  private final String googleClientId;
  private final String googleClientSecret;

  /**
   * Google login credentials (ADR-012 addendum), for {@link #googleConfigured()}. Read here — not
   * via Boot's {@code spring.security.oauth2.client.registration.*} autoconfiguration — because
   * that autoconfiguration fails application startup when {@code client-id} is an empty string (a
   * real environment that has not configured Google yet would otherwise never boot); {@link
   * #googleClientRegistration()} builds the {@link ClientRegistration} by hand instead, only when
   * both are non-blank ({@link #googleConfigured()}).
   *
   * <p>Logs (never the values themselves) when exactly one of the two is set — that is always a
   * misconfiguration (a copy-paste of one var without the other), and oauth2Login() silently
   * staying disabled with no signal anywhere would otherwise be a confusing way to discover it.
   */
  public SecurityConfig(
      @Value("${forma.oauth2.google.client-id:}") String googleClientId,
      @Value("${forma.oauth2.google.client-secret:}") String googleClientSecret) {
    this.googleClientId = googleClientId;
    this.googleClientSecret = googleClientSecret;
    if (googlePartiallyConfigured()) {
      log.warn(
          "Google login misconfigured: exactly one of GOOGLE_CLIENT_ID/GOOGLE_CLIENT_SECRET is"
              + " set. oauth2Login() stays disabled until both are provided.");
    }
  }

  private boolean googleConfigured() {
    return hasGoogleClientId() && hasGoogleClientSecret();
  }

  /**
   * @return whether exactly one of client id/secret is set — always a misconfiguration.
   */
  private boolean googlePartiallyConfigured() {
    return hasGoogleClientId() != hasGoogleClientSecret();
  }

  private boolean hasGoogleClientId() {
    return googleClientId != null && !googleClientId.isBlank();
  }

  private boolean hasGoogleClientSecret() {
    return googleClientSecret != null && !googleClientSecret.isBlank();
  }

  /**
   * Built by hand from {@link CommonOAuth2Provider#GOOGLE}'s defaults (authorization/token/userinfo
   * endpoints, {@code openid,profile,email} scope) plus this environment's client id/secret. The
   * {@code redirect-uri} template mirrors akademia: mounted under {@code /api} so the single nginx
   * proxy rule for {@code /api/} already covers it and the SPA's client-side router never sees it
   * (README of this decision: ADR-012 addendum). {@code {baseUrl}} expands against the *inbound*
   * request at authorization time — {@code server.forward-headers-strategy=framework}
   * (application.yml) is what makes that resolve to the public https host behind nginx rather than
   * the container's own plain-http view of itself.
   */
  private ClientRegistration googleClientRegistration() {
    return CommonOAuth2Provider.GOOGLE
        .getBuilder("google")
        .clientId(googleClientId)
        .clientSecret(googleClientSecret)
        .redirectUri("{baseUrl}/api/login/oauth2/code/{registrationId}")
        .build();
  }

  /**
   * Argon2id password hashing wrapped in a {@link DelegatingPasswordEncoder} for algorithm agility
   * (ADR-012 Decision 2). Parameters follow the OWASP Argon2id minimum: saltLength=16,
   * hashLength=32, parallelism=1, memory=19456 KiB (~19 MiB), iterations=2. BCrypt is registered as
   * an acceptable fallback encoder only (constrained self-hosted hardware) — Argon2id is the
   * default for every new hash. Never use {@code AesGcmTokenCipher} here (ADR-012 explicit
   * prohibition: that cipher is reversible and exists only for OAuth provider tokens).
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    String encodingId = "argon2";
    Map<String, PasswordEncoder> encoders = new HashMap<>();
    encoders.put(encodingId, new Argon2PasswordEncoder(16, 32, 1, 19456, 2));
    encoders.put("bcrypt", new BCryptPasswordEncoder());
    DelegatingPasswordEncoder delegatingPasswordEncoder =
        new DelegatingPasswordEncoder(encodingId, encoders);
    delegatingPasswordEncoder.setDefaultPasswordEncoderForMatches(encoders.get(encodingId));
    return delegatingPasswordEncoder;
  }

  /**
   * Exposed for {@code AuthController}'s JSON login endpoint, which authenticates programmatically
   * (not via Spring Security's default form-login filter) and must save the resulting {@code
   * Authentication} into the session itself.
   */
  @Bean
  public AuthenticationManager authenticationManager(
      HttpSecurity http, UserDetailsService userDetailsService, PasswordEncoder passwordEncoder)
      throws Exception {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
    builder.authenticationProvider(provider);
    return builder.build();
  }

  /**
   * Exposed for {@code AuthController}'s login endpoint to persist the {@code Authentication} it
   * builds programmatically into the HTTP session, matching what the servlet filter chain reads
   * back on every subsequent request.
   */
  @Bean
  public SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      CorsConfigurationSource corsConfigurationSource,
      ApiAuthenticationEntryPoint authenticationEntryPoint,
      ApiAccessDeniedHandler accessDeniedHandler,
      CsrfCookieFilter csrfCookieFilter,
      GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler,
      FrontendRedirectResolver frontendRedirectResolver)
      throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource))
        // Cookie-based CSRF (ADR-012): the SPA reads the JS-readable XSRF-TOKEN cookie and echoes
        // it as the X-XSRF-TOKEN header on every non-GET request. No CSRF exemptions — register
        // and login are protected too; the SPA primes the cookie with an unauthenticated GET
        // (e.g. /actuator/health) before its first POST. addFilterAfter(CsrfCookieFilter, ...) is
        // required for the repository to actually write the cookie (see its javadoc).
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
        .addFilterAfter(csrfCookieFilter, BasicAuthenticationFilter.class)
        .sessionManagement(
            session ->
                session
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .sessionFixation(fixation -> fixation.changeSessionId()))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        HttpMethod.POST,
                        ApiPaths.V1 + "/auth/register",
                        ApiPaths.V1 + "/auth/login")
                    .permitAll()
                    // El generador de planes es público a propósito: es el embudo de la portada, y
                    // pedir cuenta antes de enseñar nada lo vaciaría de sentido. Todo lo que hay
                    // detrás son cálculos sin efecto y una validación; nada escribe en la base de
                    // datos. Cuando eso cambie —cuentas, correos— hará falta limitar el ritmo
                    // antes.
                    .requestMatchers(HttpMethod.POST, ApiPaths.V1 + "/public/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/actuator/health")
                    .permitAll()
                    // Google login (ADR-012 addendum): both the authorization-start and the
                    // provider-callback paths are unauthenticated by nature — permitAll here is
                    // what
                    // lets an anonymous visitor reach them at all. Mounted under /api for the same
                    // reason akademia mounts them there: one nginx proxy rule covers /api/, so the
                    // SPA's client-side router never intercepts these.
                    .requestMatchers("/api/oauth2/**", "/api/login/oauth2/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .logout(
            logout ->
                logout
                    .logoutUrl(ApiPaths.V1 + "/auth/logout")
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler(
                        (request, response, authentication) ->
                            response.setStatus(HttpServletResponse.SC_NO_CONTENT)));

    // oauth2Login() is only registered when both credentials are configured (constructor javadoc):
    // an InMemoryClientRegistrationRepository refuses to be built with zero registrations, so there
    // is nothing valid to pass it otherwise. When unconfigured, GoogleOAuth2FallbackController
    // answers /api/oauth2/authorization/google instead (its own javadoc explains why that never
    // conflicts with this branch).
    if (googleConfigured()) {
      http.oauth2Login(
          oauth2 ->
              oauth2
                  .clientRegistrationRepository(
                      new InMemoryClientRegistrationRepository(googleClientRegistration()))
                  .authorizationEndpoint(
                      authorization -> authorization.baseUri("/api/oauth2/authorization"))
                  .redirectionEndpoint(
                      redirection -> redirection.baseUri("/api/login/oauth2/code/*"))
                  .successHandler(googleOAuth2SuccessHandler)
                  .failureHandler(
                      (request, response, exception) ->
                          response.sendRedirect(
                              frontendRedirectResolver.resolve("/login?error=google"))));
    }

    return http.build();
  }
}
