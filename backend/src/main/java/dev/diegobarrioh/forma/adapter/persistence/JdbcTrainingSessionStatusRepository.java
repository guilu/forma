package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.StoredSessionStatus;
import dev.diegobarrioh.forma.application.TrainingSessionStatusRepository;
import dev.diegobarrioh.forma.domain.SessionStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter storing training session completion status (FOR-27) in {@code
 * training_session_status}.
 *
 * <p>Plain JDBC via {@link JdbcTemplate} (no ORM, like FOR-16). {@code upsert} uses a portable
 * update-then-insert rather than a database-specific {@code ON CONFLICT}/{@code MERGE}, so it works
 * on both PostgreSQL and the H2 test database.
 */
@Repository
public class JdbcTrainingSessionStatusRepository implements TrainingSessionStatusRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcTrainingSessionStatusRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Map<String, StoredSessionStatus> findAll() {
    Map<String, StoredSessionStatus> byId = new LinkedHashMap<>();
    jdbcTemplate.query(
        "SELECT session_id, status, notes FROM training_session_status",
        rs -> {
          String sessionId = rs.getString("session_id");
          byId.put(
              sessionId,
              new StoredSessionStatus(
                  sessionId, SessionStatus.valueOf(rs.getString("status")), rs.getString("notes")));
        });
    return byId;
  }

  @Override
  public void upsert(String sessionId, SessionStatus status, String notes) {
    int updated =
        jdbcTemplate.update(
            "UPDATE training_session_status SET status = ?, notes = ? WHERE session_id = ?",
            status.name(),
            notes,
            sessionId);
    if (updated == 0) {
      jdbcTemplate.update(
          "INSERT INTO training_session_status (session_id, status, notes) VALUES (?, ?, ?)",
          sessionId,
          status.name(),
          notes);
    }
  }
}
