package dev.diegobarrioh.forma;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the V20 migration's additive schema and Diego's seed row (FOR-149, epic FOR-148 slice 1)
 * directly against a freshly migrated database, isolated from the shared {@code
 * application-test.yml} H2 instance other {@code @SpringBootTest} classes reuse and mutate (e.g.
 * {@code JdbcUserProfileRepositoryTest}'s {@code @BeforeEach} clears {@code user_profile}). Plain
 * Flyway + JDBC (no Spring context), matching the {@code flyway-core}/{@code h2} dependencies
 * already used by {@link MigrationBaselineTest}, so this test's own migrated database is unaffected
 * by test execution order elsewhere in the suite.
 */
class UserProfilePersonalTargetsSeedTest {

  private static final String JDBC_URL =
      "jdbc:h2:mem:for149_personal_targets_seed;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

  private static Connection connection;
  private static Flyway flyway;

  @BeforeAll
  static void migrate() throws Exception {
    // Pinned to V22: FOR-169's V23 removes this seed (empty first-run). The
    // post-cleanup empty state is covered by EmptyFirstRunMigrationTest.
    flyway =
        Flyway.configure()
            .dataSource(JDBC_URL, "sa", "")
            .locations("classpath:db/migration")
            .target("22")
            .load();
    flyway.migrate();
    connection = DriverManager.getConnection(JDBC_URL, "sa", "");
  }

  @AfterAll
  static void closeConnection() throws Exception {
    connection.close();
  }

  @Test
  void migrationHeadIsAtLeastV20() {
    // Same Flyway-info API as MigrationBaselineTest, rather than raw SQL against
    // flyway_schema_history (whose identifier case-folding is finicky under H2/MODE=PostgreSQL).
    MigrationInfo v20 =
        Arrays.stream(flyway.info().applied())
            .filter(info -> info.getVersion() != null)
            .filter(info -> "20".equals(info.getVersion().getVersion()))
            .findFirst()
            .orElse(null);

    assertThat(v20).isNotNull();
    assertThat(v20.getState().isApplied()).isTrue();
  }

  @Test
  void seedsExactlyOneRowForDefaultUserWithDiegosPerfilValues() throws Exception {
    try (Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT COUNT(*) AS total FROM user_profile")) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getInt("total")).isEqualTo(1);
    }

    try (Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                """
                SELECT owner_id, name, height_cm, protein_target_g,
                       baseline_weight_kg, baseline_body_fat_pct, baseline_bmi,
                       base_calories_kcal, body_fat_target_min_pct, body_fat_target_max_pct,
                       weight_target_min_kg, weight_target_max_kg, fat_target_g, carbs_target_g
                FROM user_profile WHERE owner_id = 'default-user'
                """)) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getString("name")).isEqualTo("Diego");
      assertThat(rs.getBigDecimal("height_cm")).isEqualByComparingTo(new BigDecimal("180.0"));
      assertThat(rs.getBigDecimal("protein_target_g"))
          .isEqualByComparingTo(new BigDecimal("160.0"));
      assertThat(rs.getBigDecimal("baseline_weight_kg"))
          .isEqualByComparingTo(new BigDecimal("73.6"));
      assertThat(rs.getBigDecimal("baseline_body_fat_pct"))
          .isEqualByComparingTo(new BigDecimal("14.7"));
      assertThat(rs.getBigDecimal("baseline_bmi")).isEqualByComparingTo(new BigDecimal("22.7"));
      assertThat(rs.getBigDecimal("base_calories_kcal"))
          .isEqualByComparingTo(new BigDecimal("2300.0"));
      assertThat(rs.getBigDecimal("body_fat_target_min_pct"))
          .isEqualByComparingTo(new BigDecimal("12.0"));
      assertThat(rs.getBigDecimal("body_fat_target_max_pct"))
          .isEqualByComparingTo(new BigDecimal("13.0"));
      assertThat(rs.getBigDecimal("weight_target_min_kg"))
          .isEqualByComparingTo(new BigDecimal("73.0"));
      assertThat(rs.getBigDecimal("weight_target_max_kg"))
          .isEqualByComparingTo(new BigDecimal("75.0"));
      assertThat(rs.getBigDecimal("fat_target_g")).isEqualByComparingTo(new BigDecimal("70.0"));
      assertThat(rs.getBigDecimal("carbs_target_g")).isEqualByComparingTo(new BigDecimal("260.0"));
    }
  }

  @Test
  void bodyMeasurementsTableStaysEmptySeguimientoNotSeeded() throws Exception {
    try (Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT COUNT(*) AS total FROM body_measurements")) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getInt("total")).isEqualTo(0);
    }
  }
}
