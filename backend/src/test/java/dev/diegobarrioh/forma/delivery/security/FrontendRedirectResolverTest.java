package dev.diegobarrioh.forma.delivery.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FrontendRedirectResolver} (finding #3 of the fresh-review fixes on
 * ADR-014): the empty default resolves to a same-origin relative path — correct in every supported
 * flow (prod nginx, compose nginx, Vite's dev {@code /api} proxy all put the frontend and backend
 * on one browser-visible origin) — and a configured absolute origin is used only for the genuine
 * exception (hitting the backend directly, bypassing any proxy).
 */
class FrontendRedirectResolverTest {

  @Test
  void withNoConfiguredFrontendUrlResolvesToARelativePath() {
    FrontendRedirectResolver resolver = new FrontendRedirectResolver("");

    assertThat(resolver.resolve("/app")).isEqualTo("/app");
    assertThat(resolver.resolve("/login?error=google")).isEqualTo("/login?error=google");
  }

  @Test
  void aBlankConfiguredFrontendUrlIsTreatedAsUnconfigured() {
    FrontendRedirectResolver resolver = new FrontendRedirectResolver("   ");

    assertThat(resolver.resolve("/app")).isEqualTo("/app");
  }

  @Test
  void withAConfiguredFrontendUrlPrefixesThePath() {
    FrontendRedirectResolver resolver = new FrontendRedirectResolver("http://localhost:5173");

    assertThat(resolver.resolve("/app")).isEqualTo("http://localhost:5173/app");
    assertThat(resolver.resolve("/login?error=google"))
        .isEqualTo("http://localhost:5173/login?error=google");
  }
}
