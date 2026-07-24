package dev.diegobarrioh.forma.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Authenticates every request performed by the auto-configured {@code MockMvc} bean as the seeded
 * legacy placeholder user, and attaches a valid CSRF token to it (FOR-145 RUN 2).
 *
 * <p>Spring Security's filter chain (enabled RUN 1, ADR-012) now rejects every unauthenticated
 * request with 401, and every state-changing request without a matching CSRF token with 403. This
 * config uses {@code ConfigurableMockMvcBuilder#defaultRequest(RequestBuilder)} — a request
 * post-processor set on a "default" request template is merged into every request subsequently
 * performed via that {@code MockMvc} instance — so pre-existing
 * {@code @WebMvcTest}/{@code @SpringBootTest} classes stay authenticated for free, without editing
 * every individual {@code mockMvc.perform(...)} call. {@code csrf()} on a GET is a harmless no-op
 * (only state-changing methods are CSRF-checked), so a single default request works for every HTTP
 * method.
 *
 * <p>Import into a test class via {@code @Import(WebMvcAuthTestConfig.class)}. {@link
 * MockMvcBuilderCustomizer} beans are auto-discovered by Boot's MockMvc test auto-configuration and
 * applied to the shared {@code @Autowired MockMvc} bean, whether the test is a {@code @WebMvcTest}
 * slice or a full {@code @SpringBootTest}.
 */
@TestConfiguration
public class WebMvcAuthTestConfig {

  @Bean
  MockMvcBuilderCustomizer authenticatedDefaultRequestCustomizer() {
    return builder ->
        builder.defaultRequest(get("/").with(AuthTestSupport.asPlaceholderUser()).with(csrf()));
  }
}
