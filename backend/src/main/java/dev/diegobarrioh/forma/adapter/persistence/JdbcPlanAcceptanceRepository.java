package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.PlanAcceptanceRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter for {@code plan_acceptance} (migration V58). Plain {@link JdbcTemplate}, no ORM
 * (ADR-003).
 *
 * <p>A row exists only for accounts that accepted; absence IS the "not yet" answer, which is why
 * there is no boolean column to keep in step with it.
 */
@Repository
public class JdbcPlanAcceptanceRepository implements PlanAcceptanceRepository {

  private static final String EXISTS_SQL = "SELECT COUNT(*) FROM plan_acceptance WHERE user_id = ?";

  /**
   * The current cycle's anchor (design D5, migration V66): a restarted cycle answers ahead of the
   * original acceptance, but only while it is set — {@code cycle_started_at} starts out {@code
   * NULL} for every account, including ones accepted before V66.
   */
  private static final String STARTED_AT_SQL =
      "SELECT COALESCE(cycle_started_at, accepted_at) AS started_at FROM plan_acceptance"
          + " WHERE user_id = ?";

  /**
   * Same {@code getObject(..., OffsetDateTime.class).toInstant()} pattern as {@link
   * JdbcAchievementRepository} — {@code queryForObject(..., Instant.class, ...)} is the only spot
   * in this repository that asked the driver for an {@link Instant} directly, a request the H2 test
   * database tolerates but real PostgreSQL's driver does not convert a {@code timestamptz} to
   * (pattern-alignment only, no contract change).
   */
  private static final RowMapper<Instant> STARTED_AT_ROW_MAPPER =
      (rs, rowNum) -> rs.getObject("started_at", OffsetDateTime.class).toInstant();

  private static final String RESTART_CYCLE_SQL =
      "UPDATE plan_acceptance SET cycle_started_at = ? WHERE user_id = ?";

  /**
   * Insert-if-absent. Accepting twice keeps the first instant rather than moving it: the question
   * is when this account started, and it started once.
   */
  private static final String INSERT_SQL =
      """
      INSERT INTO plan_acceptance (user_id, accepted_at)
      SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM plan_acceptance WHERE user_id = ?)
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcPlanAcceptanceRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public boolean accepted(UUID userId) {
    Integer count = jdbcTemplate.queryForObject(EXISTS_SQL, Integer.class, userId);
    return count != null && count > 0;
  }

  @Override
  public void markAccepted(UUID userId, Instant at) {
    jdbcTemplate.update(INSERT_SQL, userId, OffsetDateTime.ofInstant(at, ZoneOffset.UTC), userId);
  }

  @Override
  public Optional<Instant> planStartedAt(UUID userId) {
    try {
      return Optional.ofNullable(
          jdbcTemplate.queryForObject(STARTED_AT_SQL, STARTED_AT_ROW_MAPPER, userId));
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  /**
   * No-op for an account with no {@code plan_acceptance} row (never accepted a plan): there is no
   * row to reanchor. This adapter enforces no precondition of its own about when a caller may reach
   * this method — that guard ({@link
   * dev.diegobarrioh.forma.application.PlanRestartService#restart()} requiring the plan to have
   * already reached {@link dev.diegobarrioh.forma.domain.TrainingPlanProgress.Completed}) lives in
   * the application layer, one level up.
   */
  @Override
  public void restartCycle(UUID userId, Instant at) {
    jdbcTemplate.update(RESTART_CYCLE_SQL, OffsetDateTime.ofInstant(at, ZoneOffset.UTC), userId);
  }
}
