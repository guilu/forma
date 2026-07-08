package dev.diegobarrioh.forma;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Application entry point for the FORMA backend.
 *
 * <p>This is bootstrap-only. It wires the Spring Boot runtime and does not contain any
 * product/domain behavior. Domain rules live under {@code dev.diegobarrioh.forma.domain}, use cases
 * under {@code application}, infrastructure under {@code adapter} and delivery under {@code
 * delivery}.
 */
@SpringBootApplication
public class FormaApplication {

  public static void main(String[] args) {
    SpringApplication.run(FormaApplication.class, args);
  }

  /**
   * The system clock, injected into time-dependent use cases so they stay deterministic under test
   * (a fixed {@link Clock} can replace it). See FOR-40's {@code WeeklyCheckInService}.
   */
  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
}
