package dev.diegobarrioh.forma.config;

import dev.diegobarrioh.forma.delivery.ApiPaths;
import dev.diegobarrioh.forma.delivery.security.ApiAccessDeniedHandler;
import dev.diegobarrioh.forma.delivery.security.ApiAuthenticationEntryPoint;
import dev.diegobarrioh.forma.delivery.security.CsrfCookieFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
@EnableWebSecurity
public class SecurityConfig {

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
      CsrfCookieFilter csrfCookieFilter)
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
                    .requestMatchers(HttpMethod.GET, "/actuator/health")
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
    return http.build();
  }
}
