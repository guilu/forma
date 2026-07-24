package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.UserRepository;
import dev.diegobarrioh.forma.domain.User;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter that persists {@link User} accounts to the {@code users} table (FOR-145, migration
 * V26, ADR-012). Plain JDBC via {@link JdbcTemplate} — no ORM (ADR-003), following {@link
 * JdbcUserProfileRepository}/{@link JdbcGoalRepository}'s shape.
 *
 * <p>Never selects {@code password_hash} into anything other than the {@link User#passwordHash()}
 * field used exclusively for authentication comparisons (ADR-012: never log or return it).
 */
@Repository
public class JdbcUserRepository implements UserRepository {

  private static final RowMapper<User> USER_ROW_MAPPER =
      (rs, rowNum) ->
          new User(
              (UUID) rs.getObject("id"),
              rs.getString("email"),
              rs.getString("password_hash"),
              rs.getTimestamp("created_at").toInstant(),
              rs.getTimestamp("last_login_at") == null
                  ? null
                  : rs.getTimestamp("last_login_at").toInstant(),
              rs.getBoolean("is_active"));

  private final JdbcTemplate jdbcTemplate;

  public JdbcUserRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Optional<User> findByEmail(String email) {
    try {
      User user =
          jdbcTemplate.queryForObject(
              "SELECT id, email, password_hash, created_at, last_login_at, is_active"
                  + " FROM users WHERE email = ?",
              USER_ROW_MAPPER,
              email);
      return Optional.of(user);
    } catch (EmptyResultDataAccessException ex) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<User> findById(UUID id) {
    try {
      User user =
          jdbcTemplate.queryForObject(
              "SELECT id, email, password_hash, created_at, last_login_at, is_active"
                  + " FROM users WHERE id = ?",
              USER_ROW_MAPPER,
              id);
      return Optional.of(user);
    } catch (EmptyResultDataAccessException ex) {
      return Optional.empty();
    }
  }

  @Override
  public boolean existsByEmail(String email) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
    return count != null && count > 0;
  }

  @Override
  public void insert(UUID id, String email, String passwordHash) {
    jdbcTemplate.update(
        "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)", id, email, passwordHash);
  }

  @Override
  public void updateLastLoginAt(UUID id, Instant at) {
    jdbcTemplate.update("UPDATE users SET last_login_at = ? WHERE id = ?", Timestamp.from(at), id);
  }
}
