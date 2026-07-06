package dev.diegobarrioh.forma;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
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
    // Assert the V1 baseline itself is applied, not that it is the latest migration:
    // later stories add migrations (V2+, e.g. FOR-16 body_measurements) so "current" moves on.
    MigrationInfo baseline =
        Arrays.stream(flyway.info().applied())
            .filter(info -> info.getVersion() != null)
            .filter(info -> "1".equals(info.getVersion().getVersion()))
            .findFirst()
            .orElse(null);

    assertThat(baseline).isNotNull();
    assertThat(baseline.getState().isApplied()).isTrue();
  }
}
