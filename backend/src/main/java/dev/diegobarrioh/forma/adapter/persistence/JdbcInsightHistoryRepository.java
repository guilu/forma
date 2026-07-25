package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.InsightHistoryRepository;
import dev.diegobarrioh.forma.application.WeeklyInsights;
import dev.diegobarrioh.forma.domain.Recommendation;
import dev.diegobarrioh.forma.domain.RecommendationCategory;
import dev.diegobarrioh.forma.domain.RecommendationSeverity;
import dev.diegobarrioh.forma.domain.WeeklyCheckIn;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter that persists each generated {@link WeeklyInsights} to the period-keyed {@code
 * insight_history} table plus its {@code insight_history_recommendation} child rows (FOR-110).
 *
 * <p>Plain JDBC via {@link JdbcTemplate} — the project has no JPA/ORM on purpose ({@code
 * backend/build.gradle}), same as {@link JdbcUserProfileRepository}. {@code save} is a portable
 * update-then-insert upsert on the parent row (works on both PostgreSQL and the H2 test database),
 * following {@link JdbcUserProfileRepository}'s pattern; the child recommendation rows are always
 * deleted and re-inserted, following {@link JdbcShoppingListRepository#regenerate}'s
 * replace-in-place pattern — this gives "repeated generation within the same period overwrites"
 * (spec FOR-110 Edge Cases).
 *
 * <p>{@code listAll}/{@code findMostRecentCheckInBefore} intentionally issue one query per period
 * for its recommendations (N+1) rather than a single join — acceptable at the MVP's low, personal,
 * weekly-cadence data volume (spec FOR-110 Open Questions: no retention cap for MVP), and keeps the
 * mapping code simple and easy to follow.
 *
 * <p>Real multi-user auth (FOR-145c, ADR-012, migration V34): both tables' primary keys were
 * rebuilt to include {@code user_id UUID} — {@code insight_history}'s PK is now {@code (user_id,
 * week_start_date)} and {@code insight_history_recommendation}'s is {@code (user_id,
 * week_start_date, sort_order)} — closing the "gap table" cross-user collision on the bare {@code
 * week_start_date}. Every statement is scoped by {@code user_id}.
 */
@Repository
public class JdbcInsightHistoryRepository implements InsightHistoryRepository {

  private static final String CHECKIN_COLUMNS =
      "week_start_date, latest_weight_kg, latest_body_fat_percentage, latest_lean_mass_kg,"
          + " planned_running_sessions, completed_running_sessions, planned_strength_sessions,"
          + " completed_strength_sessions, notes";

  private static final String UPDATE_PARENT_SQL =
      """
      UPDATE insight_history SET
        latest_weight_kg = ?, latest_body_fat_percentage = ?, latest_lean_mass_kg = ?,
        planned_running_sessions = ?, completed_running_sessions = ?,
        planned_strength_sessions = ?, completed_strength_sessions = ?, notes = ?, generated_at = ?
      WHERE user_id = ? AND week_start_date = ?
      """;

  private static final String INSERT_PARENT_SQL =
      """
      INSERT INTO insight_history
        (user_id, week_start_date, latest_weight_kg, latest_body_fat_percentage,
         latest_lean_mass_kg, planned_running_sessions, completed_running_sessions,
         planned_strength_sessions, completed_strength_sessions, notes, generated_at)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private static final String DELETE_RECOMMENDATIONS_SQL =
      "DELETE FROM insight_history_recommendation WHERE user_id = ? AND week_start_date = ?";

  private static final String INSERT_RECOMMENDATION_SQL =
      """
      INSERT INTO insight_history_recommendation
        (user_id, week_start_date, sort_order, is_main, category, severity, message, reason,
         related_metric, created_at)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private static final String LIST_PERIODS_SQL =
      "SELECT "
          + CHECKIN_COLUMNS
          + ", generated_at FROM insight_history"
          + " WHERE user_id = ? ORDER BY week_start_date DESC";

  private static final String FIND_RECOMMENDATIONS_SQL =
      "SELECT sort_order, is_main, category, severity, message, reason, related_metric, created_at"
          + " FROM insight_history_recommendation WHERE user_id = ? AND week_start_date = ?"
          + " ORDER BY sort_order";

  private static final String FIND_PRIOR_CHECKIN_SQL =
      "SELECT "
          + CHECKIN_COLUMNS
          + " FROM insight_history"
          + " WHERE user_id = ? AND week_start_date < ? ORDER BY week_start_date DESC";

  private static final RowMapper<WeeklyCheckIn> CHECKIN_MAPPER =
      (rs, rowNum) ->
          new WeeklyCheckIn(
              rs.getObject("week_start_date", LocalDate.class),
              toNullableDouble(rs.getBigDecimal("latest_weight_kg")),
              toNullableDouble(rs.getBigDecimal("latest_body_fat_percentage")),
              toNullableDouble(rs.getBigDecimal("latest_lean_mass_kg")),
              rs.getInt("planned_running_sessions"),
              rs.getInt("completed_running_sessions"),
              rs.getInt("planned_strength_sessions"),
              rs.getInt("completed_strength_sessions"),
              rs.getString("notes"));

  private static final RowMapper<StoredPeriod> PERIOD_MAPPER =
      (rs, rowNum) ->
          new StoredPeriod(
              CHECKIN_MAPPER.mapRow(rs, rowNum),
              rs.getObject("generated_at", OffsetDateTime.class).toInstant());

  private static final RowMapper<StoredRecommendation> RECOMMENDATION_MAPPER =
      (rs, rowNum) ->
          new StoredRecommendation(
              rs.getBoolean("is_main"),
              new Recommendation(
                  rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                  RecommendationCategory.valueOf(rs.getString("category")),
                  RecommendationSeverity.valueOf(rs.getString("severity")),
                  rs.getString("message"),
                  rs.getString("reason"),
                  rs.getString("related_metric")));

  private final JdbcTemplate jdbcTemplate;

  public JdbcInsightHistoryRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void save(UUID userId, WeeklyInsights insights) {
    WeeklyCheckIn checkIn = insights.checkIn();
    LocalDate period = checkIn.weekStartDate();
    OffsetDateTime generatedAt = toOffsetDateTime(insights.generatedAt());

    int updated =
        jdbcTemplate.update(
            UPDATE_PARENT_SQL,
            toNullableBigDecimal(checkIn.latestWeightKg()),
            toNullableBigDecimal(checkIn.latestBodyFatPercentage()),
            toNullableBigDecimal(checkIn.latestLeanMassKg()),
            checkIn.plannedRunningSessions(),
            checkIn.completedRunningSessions(),
            checkIn.plannedStrengthSessions(),
            checkIn.completedStrengthSessions(),
            checkIn.notes(),
            generatedAt,
            userId,
            period);
    if (updated == 0) {
      jdbcTemplate.update(
          INSERT_PARENT_SQL,
          userId,
          period,
          toNullableBigDecimal(checkIn.latestWeightKg()),
          toNullableBigDecimal(checkIn.latestBodyFatPercentage()),
          toNullableBigDecimal(checkIn.latestLeanMassKg()),
          checkIn.plannedRunningSessions(),
          checkIn.completedRunningSessions(),
          checkIn.plannedStrengthSessions(),
          checkIn.completedStrengthSessions(),
          checkIn.notes(),
          generatedAt);
    }

    jdbcTemplate.update(DELETE_RECOMMENDATIONS_SQL, userId, period);
    insertRecommendation(userId, period, 0, true, insights.main());
    List<Recommendation> secondary = insights.secondary();
    for (int i = 0; i < secondary.size(); i++) {
      insertRecommendation(userId, period, i + 1, false, secondary.get(i));
    }
  }

  @Override
  public List<WeeklyInsights> listAll(UUID userId) {
    return jdbcTemplate.query(LIST_PERIODS_SQL, PERIOD_MAPPER, userId).stream()
        .map(period -> toWeeklyInsights(userId, period))
        .toList();
  }

  @Override
  public Optional<WeeklyCheckIn> findMostRecentCheckInBefore(UUID userId, LocalDate period) {
    List<WeeklyCheckIn> found =
        jdbcTemplate.query(FIND_PRIOR_CHECKIN_SQL, CHECKIN_MAPPER, userId, period);
    return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
  }

  private void insertRecommendation(
      UUID userId, LocalDate period, int sortOrder, boolean isMain, Recommendation recommendation) {
    jdbcTemplate.update(
        INSERT_RECOMMENDATION_SQL,
        userId,
        period,
        sortOrder,
        isMain,
        recommendation.category().name(),
        recommendation.severity().name(),
        recommendation.message(),
        recommendation.reason(),
        recommendation.relatedMetric(),
        toOffsetDateTime(recommendation.createdAt()));
  }

  private WeeklyInsights toWeeklyInsights(UUID userId, StoredPeriod period) {
    List<StoredRecommendation> rows =
        jdbcTemplate.query(
            FIND_RECOMMENDATIONS_SQL,
            RECOMMENDATION_MAPPER,
            userId,
            period.checkIn().weekStartDate());
    Recommendation main =
        rows.stream()
            .filter(StoredRecommendation::isMain)
            .map(StoredRecommendation::recommendation)
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "insight_history_recommendation has no main row for period "
                            + period.checkIn().weekStartDate()));
    List<Recommendation> secondary =
        rows.stream()
            .filter(row -> !row.isMain())
            .map(StoredRecommendation::recommendation)
            .toList();
    return new WeeklyInsights(period.checkIn(), main, secondary, period.generatedAt());
  }

  private static OffsetDateTime toOffsetDateTime(Instant instant) {
    return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  private static Double toNullableDouble(BigDecimal value) {
    return value == null ? null : value.doubleValue();
  }

  private static BigDecimal toNullableBigDecimal(Double value) {
    return value == null ? null : BigDecimal.valueOf(value);
  }

  /** Row of {@code insight_history} without its recommendations. */
  private record StoredPeriod(WeeklyCheckIn checkIn, Instant generatedAt) {}

  /** Row of {@code insight_history_recommendation}, tagged with whether it is the main one. */
  private record StoredRecommendation(boolean isMain, Recommendation recommendation) {}
}
