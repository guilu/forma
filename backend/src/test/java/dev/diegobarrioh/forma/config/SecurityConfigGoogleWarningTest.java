package dev.diegobarrioh.forma.config;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for {@link SecurityConfig}'s startup warning when exactly one of
 * GOOGLE_CLIENT_ID/GOOGLE_CLIENT_SECRET is set (finding #5 of the fresh-review fixes on ADR-014) —
 * a partial configuration silently leaves {@code oauth2Login()} disabled with no other signal.
 */
class SecurityConfigGoogleWarningTest {

  private ListAppender<ILoggingEvent> appender;
  private Logger logbackLogger;

  @BeforeEach
  void attachAppender() {
    logbackLogger = (Logger) LoggerFactory.getLogger(SecurityConfig.class);
    appender = new ListAppender<>();
    appender.start();
    logbackLogger.addAppender(appender);
  }

  @AfterEach
  void detachAppender() {
    logbackLogger.detachAppender(appender);
  }

  @Test
  void warnsWhenOnlyClientIdIsSet() {
    new SecurityConfig("a-client-id", "");

    assertThat(appender.list)
        .anySatisfy(
            event -> {
              assertThat(event.getLevel().toString()).isEqualTo("WARN");
              assertThat(event.getFormattedMessage()).contains("GOOGLE_CLIENT_ID");
              // Never log the value itself.
              assertThat(event.getFormattedMessage()).doesNotContain("a-client-id");
            });
  }

  @Test
  void warnsWhenOnlyClientSecretIsSet() {
    new SecurityConfig("", "a-client-secret");

    assertThat(appender.list)
        .anySatisfy(event -> assertThat(event.getLevel().toString()).isEqualTo("WARN"));
  }

  @Test
  void doesNotWarnWhenNeitherIsSet() {
    new SecurityConfig("", "");

    assertThat(appender.list).isEmpty();
  }

  @Test
  void doesNotWarnWhenBothAreSet() {
    new SecurityConfig("a-client-id", "a-client-secret");

    assertThat(appender.list).isEmpty();
  }
}
