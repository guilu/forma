package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.StoredSessionStatus;
import dev.diegobarrioh.forma.application.TrainingSessionStatusRepository;
import dev.diegobarrioh.forma.domain.SessionStatus;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter storing training session overrides (FOR-27) in {@code training_session_status}.
 *
 * <p>Plain JDBC via {@link JdbcTemplate} (no ORM, like FOR-16). Both upserts use a portable
 * update-then-insert rather than a database-specific {@code ON CONFLICT}/{@code MERGE}, so they
 * work on both PostgreSQL and the H2 test database.
 *
 * <p>Reads and writes are scoped by {@code (user_id, week_start)} since migration V60: a row
 * belongs to one week, so last week's completions stop being visible on Monday instead of being
 * replayed for ever.
 */
@Repository
public class JdbcTrainingSessionStatusRepository implements TrainingSessionStatusRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcTrainingSessionStatusRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Map<String, StoredSessionStatus> findByUserAndWeek(UUID userId, LocalDate weekStart) {
    Map<String, StoredSessionStatus> byKey = new LinkedHashMap<>();
    jdbcTemplate.query(
        "SELECT session_key, status, scheduled_day, completed_at, notes"
            + " FROM training_session_status WHERE user_id = ? AND week_start = ?",
        rs -> {
          String sessionKey = rs.getString("session_key");
          String scheduledDay = rs.getString("scheduled_day");
          Timestamp completedAt = rs.getTimestamp("completed_at");
          byKey.put(
              sessionKey,
              new StoredSessionStatus(
                  sessionKey,
                  SessionStatus.valueOf(rs.getString("status")),
                  scheduledDay == null ? null : DayOfWeek.valueOf(scheduledDay),
                  completedAt == null ? null : completedAt.toInstant(),
                  rs.getString("notes")));
        },
        userId,
        weekStart);
    return byKey;
  }

  @Override
  public void upsertStatus(
      UUID userId,
      LocalDate weekStart,
      String sessionKey,
      SessionStatus status,
      Instant completedAt,
      String notes) {
    Timestamp completed = completedAt == null ? null : Timestamp.from(completedAt);
    int updated =
        jdbcTemplate.update(
            "UPDATE training_session_status SET status = ?, completed_at = ?, notes = ?"
                + " WHERE user_id = ? AND week_start = ? AND session_key = ?",
            status.name(),
            completed,
            notes,
            userId,
            weekStart,
            sessionKey);
    if (updated == 0) {
      jdbcTemplate.update(
          "INSERT INTO training_session_status"
              + " (user_id, week_start, session_key, status, completed_at, notes)"
              + " VALUES (?, ?, ?, ?, ?, ?)",
          userId,
          weekStart,
          sessionKey,
          status.name(),
          completed,
          notes);
    }
  }

  @Override
  public void upsertScheduledDay(
      UUID userId, LocalDate weekStart, String sessionKey, DayOfWeek scheduledDay) {
    String day = scheduledDay == null ? null : scheduledDay.name();
    int updated =
        jdbcTemplate.update(
            "UPDATE training_session_status SET scheduled_day = ?"
                + " WHERE user_id = ? AND week_start = ? AND session_key = ?",
            day,
            userId,
            weekStart,
            sessionKey);
    if (updated == 0) {
      // No status recorded for this session yet — the move alone creates the row, so the session
      // stays PLANNED while sitting on its new day.
      jdbcTemplate.update(
          "INSERT INTO training_session_status"
              + " (user_id, week_start, session_key, scheduled_day, status)"
              + " VALUES (?, ?, ?, ?, ?)",
          userId,
          weekStart,
          sessionKey,
          day,
          SessionStatus.PLANNED.name());
    }
  }
}
