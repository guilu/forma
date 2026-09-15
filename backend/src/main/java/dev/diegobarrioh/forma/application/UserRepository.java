package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for persisting and reading {@link User} accounts (FOR-145, ADR-012). Owned by the
 * application/domain side; {@code adapter/persistence} implements it (ADR-001). Speaks only in
 * domain objects, never in rows or SQL types.
 */
public interface UserRepository {

  /** Finds an account by its unique login email, or empty if none exists. */
  Optional<User> findByEmail(String email);

  /** Finds an account by id, or empty if none exists. */
  Optional<User> findById(UUID id);

  /** Finds an account by its linked Google "sub" claim, or empty if none exists (migration V62). */
  Optional<User> findByGoogleSubject(String googleSubject);

  /** Whether an account with this email already exists (FOR-145 spec: duplicate-email check). */
  boolean existsByEmail(String email);

  /**
   * Inserts a new account row. Callers pass an already Argon2id-hashed password (ADR-012) — this
   * port never hashes.
   */
  void insert(UUID id, String email, String passwordHash);

  /**
   * Inserts a new Google-only account row (migration V62): no password, an active {@code USER}
   * account linked to the given Google "sub" claim from the first login.
   */
  void insertWithGoogleSubject(UUID id, String email, String googleSubject);

  /**
   * Links a Google "sub" claim to an existing account found by email (migration V62 — first Google
   * login for an account that already exists, e.g. one created by self-registration). Never touches
   * {@code password_hash} or {@code role}: linking is additive, not a takeover.
   */
  void linkGoogleSubject(UUID id, String googleSubject);

  /** Records a successful login (FOR-145 spec: "last_login_at MUST update"). */
  void updateLastLoginAt(UUID id, Instant at);
}
