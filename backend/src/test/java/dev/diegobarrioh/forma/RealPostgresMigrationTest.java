package dev.diegobarrioh.forma;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies the Flyway migration baseline (FOR-83) applies to a fresh, REAL PostgreSQL database —
 * not H2's PostgreSQL-compatibility mode, which every other test (see {@link
 * MigrationBaselineTest}) runs against instead.
 *
 * <p>That compatibility mode is not Postgres: it has already forced real design decisions in this
 * repo (e.g. ADR-011's nullable-sentinel + UNIQUE trick in place of a partial index, because H2's
 * {@code CREATE INDEX ... WHERE} does not parse). Anything Postgres-specific that H2 silently
 * accepts, or silently rejects, has been invisible to CI until this test existed.
 *
 * <p>No {@code @ActiveProfiles} is declared on purpose: activating a profile would pull in {@code
 * application-test.yml}, which points at H2. Leaving the default profile active means only the base
 * {@code application.yml} datasource applies — the same {@code SPRING_DATASOURCE_*} variables
 * production and Docker Compose already use (see {@code compose.yaml}, {@code
 * application-prod.yml}) — which the {@code postgres-migrations} CI job points at its {@code
 * postgres:17} service container. If a migration does not apply cleanly, Spring's context fails to
 * start and this test fails with Flyway's own error — no separate "did it actually run" check is
 * needed for that failure mode, but the assertions below also make a completed run observable in
 * the CI log rather than relying on "no exception was thrown".
 *
 * <p>Tagged {@code postgres} rather than left in the default suite: it needs a real database that a
 * plain {@code ./gradlew build} does not have. {@code build.gradle} excludes this tag from the
 * {@code test} task and adds it only to {@code postgresTest}, which the dedicated CI job runs.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("postgres")
class RealPostgresMigrationTest {

  private static final Logger log = LoggerFactory.getLogger(RealPostgresMigrationTest.class);

  @Autowired private Flyway flyway;

  @Test
  void allMigrationsApplyCleanlyToRealPostgres() {
    MigrationInfo[] all = flyway.info().all();
    MigrationInfo[] applied = flyway.info().applied();

    assertThat(all).isNotEmpty();

    // Printed with the runner's default INFO logging (no extra config needed), so anyone reading
    // the `postgres-migrations` job's log sees exactly how far Flyway got — the job cannot pass
    // silently without this line naming the highest migration it actually reached.
    MigrationInfo latest = all[all.length - 1];
    log.info(
        "Real PostgreSQL: applied {} of {} migrations. Latest = V{} ({})",
        applied.length,
        all.length,
        latest.getVersion(),
        latest.getDescription());

    // Every discovered migration was applied — none pending, none skipped — and the schema
    // history table's last entry is the highest-versioned migration on disk, not merely "some"
    // migration. This is the schema-history-reaches-the-top assertion, made explicit rather than
    // inferred from the absence of a startup failure.
    assertThat(applied).hasSameSizeAs(all);
    assertThat(applied).allSatisfy(info -> assertThat(info.getState().isApplied()).isTrue());
    assertThat(applied[applied.length - 1].getVersion()).isEqualTo(latest.getVersion());
  }
}
