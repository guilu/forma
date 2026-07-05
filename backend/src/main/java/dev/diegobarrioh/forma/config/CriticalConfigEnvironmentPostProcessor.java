package dev.diegobarrioh.forma.config;

import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Fails fast when critical configuration is missing (FOR-90).
 *
 * <p>Runs very early, before any bean is created, so that a real deployment (the {@code prod}
 * profile) stops immediately with a clear message naming the missing variable — instead of silently
 * falling back to an unsafe local default or surfacing a confusing downstream error. The
 * local/default and {@code test} profiles keep their safe defaults and are not checked.
 *
 * <p>Registered via {@code META-INF/spring.factories}.
 */
public class CriticalConfigEnvironmentPostProcessor implements EnvironmentPostProcessor {

  static final String PROD_PROFILE = "prod";

  /** Environment variables that must be present in the prod profile. */
  static final List<String> REQUIRED =
      List.of("SPRING_DATASOURCE_URL", "SPRING_DATASOURCE_USERNAME", "SPRING_DATASOURCE_PASSWORD");

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    if (!List.of(environment.getActiveProfiles()).contains(PROD_PROFILE)) {
      return;
    }
    for (String name : REQUIRED) {
      String value = environment.getProperty(name);
      if (value == null || value.isBlank()) {
        throw new IllegalStateException(
            "Missing required configuration '"
                + name
                + "' for the '"
                + PROD_PROFILE
                + "' profile. Provide it via the environment; see docs/configuration.md.");
      }
    }
  }
}
