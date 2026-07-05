package dev.diegobarrioh.forma.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.FormaApplication;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Verifies the environment configuration baseline (FOR-90).
 *
 * <p>Under the {@code prod} profile, critical datasource configuration is required from the
 * environment. A missing secret fails startup fast with a clear message ({@link
 * CriticalConfigEnvironmentPostProcessor}); when the values are supplied, configuration binds and
 * the context starts.
 */
class EnvironmentConfigTest {

  private SpringApplication prodApplication(Map<String, Object> env) {
    SpringApplication app = new SpringApplication(FormaApplication.class);
    app.setWebApplicationType(WebApplicationType.NONE);
    app.setAdditionalProfiles("prod");
    app.setDefaultProperties(env);
    return app;
  }

  @Test
  void prodProfileFailsFastWhenDatabasePasswordIsMissing() {
    SpringApplication app =
        prodApplication(
            Map.of(
                "SPRING_DATASOURCE_URL", "jdbc:h2:mem:forma_env_missing",
                "SPRING_DATASOURCE_USERNAME", "forma"));

    assertThatThrownBy(app::run)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SPRING_DATASOURCE_PASSWORD");
  }

  @Test
  void prodProfileLoadsDatasourceConfigurationFromEnvironment() {
    SpringApplication app =
        prodApplication(
            Map.of(
                "SPRING_DATASOURCE_URL", "jdbc:h2:mem:forma_env_ok;DB_CLOSE_DELAY=-1",
                "SPRING_DATASOURCE_USERNAME", "forma",
                "SPRING_DATASOURCE_PASSWORD", "supplied-by-environment"));

    try (ConfigurableApplicationContext context = app.run()) {
      assertThat(context.getEnvironment().getProperty("spring.datasource.username"))
          .isEqualTo("forma");
    }
  }
}
