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

  /** Whether an account with this email already exists (FOR-145 spec: duplicate-email check). */
  boolean existsByEmail(String email);

  /**
   * Inserts a new account row. Callers pass an already Argon2id-hashed password (ADR-012) — this
   * port never hashes.
   */
  void insert(UUID id, String email, String passwordHash);

  /** Records a successful login (FOR-145 spec: "last_login_at MUST update"). */
  void updateLastLoginAt(UUID id, Instant at);
}
