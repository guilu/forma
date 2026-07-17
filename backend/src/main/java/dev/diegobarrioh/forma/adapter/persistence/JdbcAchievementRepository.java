package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.AchievementRepository;
import dev.diegobarrioh.forma.application.EarnedAchievement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter that persists earned achievements to the {@code earned_achievement} table (FOR-135,
 * migration V18).
 *
 * <p>Plain JDBC via {@link JdbcTemplate} — no ORM (ADR-003). {@code awardIfNotEarned} is a plain
 * {@code INSERT} whose idempotency is guaranteed by the table's {@code (owner_id, achievement_id)}
 * primary key, not a portable update-then-insert upsert like {@link JdbcIntegrationRepository} —
 * unlike an upsert, an already-earned achievement must never be overwritten (its {@code earned_at}
 * is immutable, spec FOR-135 Edge Cases), so catching the primary-key violation and treating it as
 * "already earned, no-op" is the correct semantics here, not just an implementation detail. Spring
 * translates the underlying constraint violation to {@link DuplicateKeyException} identically on
 * both the H2 test database and real PostgreSQL (ADR-003), so this stays portable without a
 * database-specific {@code ON CONFLICT} clause.
 */
@Repository
public class JdbcAchievementRepository implements AchievementRepository {

  private static final RowMapper<EarnedAchievement> ROW_MAPPER =
      (rs, rowNum) ->
          new EarnedAchievement(
              rs.getString("achievement_id"),
              rs.getObject("earned_at", OffsetDateTime.class).toInstant());

  private final JdbcTemplate jdbcTemplate;

  public JdbcAchievementRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<EarnedAchievement> findAllByOwner(String ownerId) {
    return jdbcTemplate.query(
        "SELECT achievement_id, earned_at FROM earned_achievement"
            + " WHERE owner_id = ? ORDER BY earned_at, achievement_id",
        ROW_MAPPER,
        ownerId);
  }

  @Override
  public boolean awardIfNotEarned(String ownerId, String achievementId, Instant earnedAt) {
    try {
      jdbcTemplate.update(
          "INSERT INTO earned_achievement (owner_id, achievement_id, earned_at) VALUES (?, ?, ?)",
          ownerId,
          achievementId,
          OffsetDateTime.ofInstant(earnedAt, ZoneOffset.UTC));
      return true;
    } catch (DuplicateKeyException alreadyEarned) {
      return false;
    }
  }
}
