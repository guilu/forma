package dev.diegobarrioh.forma;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the Flyway migration baseline (FOR-83) applies to a fresh database. Runs against an
 * in-memory PostgreSQL-mode H2 (see application-test.yml), so it proves "a fresh database can be
 * migrated from scratch" without Docker.
 */
@SpringBootTest
@ActiveProfiles("test")
class MigrationBaselineTest {

  @Autowired private Flyway flyway;

  @Test
  void baselineMigrationIsApplied() {
    MigrationInfo current = flyway.info().current();

    assertThat(current).isNotNull();
    assertThat(current.getVersion().getVersion()).isEqualTo("1");
    assertThat(current.getState().isApplied()).isTrue();
  }
}
