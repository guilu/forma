package dev.diegobarrioh.forma;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

/** Verifies V20 can migrate a real single-user database that already has a user_profile row. */
class UserProfilePersonalTargetsExistingProfileMigrationTest {

  private static final String JDBC_URL =
      "jdbc:h2:mem:for149_existing_profile_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

  @Test
  void v20MergesDiegosPerfilValuesIntoExistingDefaultUserProfile() throws Exception {
    Flyway.configure()
        .dataSource(JDBC_URL, "sa", "")
        .locations("classpath:db/migration")
        .target("19")
        .load()
        .migrate();

    try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "");
        Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          """
          INSERT INTO user_profile (owner_id, name, height_cm, protein_target_g)
          VALUES ('default-user', 'Existing Diego', 179.0, 120.0)
          """);
    }

    // Pinned to V22: FOR-169's V23 cleanup would delete this default-user row
    // (it matches the seed and first_run_completed stays false), so validate
    // V20's merge into an existing profile at its own version. The V23 removal
    // is covered by EmptyFirstRunMigrationTest.
    Flyway.configure()
        .dataSource(JDBC_URL, "sa", "")
        .locations("classpath:db/migration")
        .target("22")
        .load()
        .migrate();

    try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "");
        Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                """
                SELECT COUNT(*) AS total,
                       MAX(name) AS name,
                       MAX(height_cm) AS height_cm,
                       MAX(protein_target_g) AS protein_target_g,
                       MAX(baseline_weight_kg) AS baseline_weight_kg,
                       MAX(body_fat_target_min_pct) AS body_fat_target_min_pct,
                       MAX(body_fat_target_max_pct) AS body_fat_target_max_pct,
                       MAX(weight_target_min_kg) AS weight_target_min_kg,
                       MAX(weight_target_max_kg) AS weight_target_max_kg,
                       MAX(fat_target_g) AS fat_target_g,
                       MAX(carbs_target_g) AS carbs_target_g
                FROM user_profile WHERE owner_id = 'default-user'
                """)) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getInt("total")).isEqualTo(1);
      assertThat(rs.getString("name")).isEqualTo("Diego");
      assertThat(rs.getBigDecimal("height_cm")).isEqualByComparingTo(new BigDecimal("180.0"));
      assertThat(rs.getBigDecimal("protein_target_g"))
          .isEqualByComparingTo(new BigDecimal("160.0"));
      assertThat(rs.getBigDecimal("baseline_weight_kg"))
          .isEqualByComparingTo(new BigDecimal("73.6"));
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
}
