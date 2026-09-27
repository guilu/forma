package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.TrainingSetLogRepository;
import dev.diegobarrioh.forma.domain.LoggedSet;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter storing the per-set training log (training-progression-and-logging slice B, design
 * D6/D7) in {@code training_set_log} (migration V67).
 *
 * <p>Plain JDBC via {@link JdbcTemplate} (no ORM, like FOR-16). The upsert is a portable
 * update-then-insert rather than a database-specific {@code ON CONFLICT}/{@code MERGE}, mirroring
 * {@link JdbcTrainingSessionStatusRepository}, so it works on both PostgreSQL and the H2 test
 * database.
 */
@Repository
public class JdbcTrainingSetLogRepository implements TrainingSetLogRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcTrainingSetLogRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<LoggedSet> findByUserWeekAndSession(
      UUID userId, LocalDate weekStart, String sessionKey) {
    List<LoggedSet> sets = new ArrayList<>();
    jdbcTemplate.query(
        "SELECT exercise_id, set_number, weight_kg, reps, done"
            + " FROM training_set_log WHERE user_id = ? AND week_start = ? AND session_key = ?",
        rs -> {
          BigDecimal weight = rs.getBigDecimal("weight_kg");
          int reps = rs.getInt("reps");
          boolean repsWasNull = rs.wasNull();
          sets.add(
              new LoggedSet(
                  rs.getString("exercise_id"),
                  rs.getInt("set_number"),
                  weight == null ? null : weight.doubleValue(),
                  repsWasNull ? null : reps,
                  rs.getBoolean("done")));
        },
        userId,
        weekStart,
        sessionKey);
    return sets;
  }

  @Override
  public void upsertSet(
      UUID userId, LocalDate weekStart, String sessionKey, LoggedSet set, Instant loggedAt) {
    BigDecimal weight = set.weightKg() == null ? null : BigDecimal.valueOf(set.weightKg());
    Timestamp logged = Timestamp.from(loggedAt);
    int updated =
        jdbcTemplate.update(
            "UPDATE training_set_log SET weight_kg = ?, reps = ?, done = ?, logged_at = ?"
                + " WHERE user_id = ? AND week_start = ? AND session_key = ?"
                + " AND exercise_id = ? AND set_number = ?",
            weight,
            set.reps(),
            set.done(),
            logged,
            userId,
            weekStart,
            sessionKey,
            set.exerciseId(),
            set.setNumber());
    if (updated == 0) {
      jdbcTemplate.update(
          "INSERT INTO training_set_log"
              + " (user_id, week_start, session_key, exercise_id, set_number, weight_kg, reps,"
              + " done, logged_at)"
              + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
          userId,
          weekStart,
          sessionKey,
          set.exerciseId(),
          set.setNumber(),
          weight,
          set.reps(),
          set.done(),
          logged);
    }
  }
}
