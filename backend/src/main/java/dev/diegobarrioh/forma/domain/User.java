package dev.diegobarrioh.forma.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A registered FORMA account (FOR-145, ADR-012). Framework-free (ADR-001): no Spring Security or
 * JDBC types — {@code delivery/security} adapts this into a {@code UserDetails} principal, and
 * {@code adapter/persistence} translates it to/from the {@code users} table.
 *
 * <p>{@code passwordHash} is always an already-hashed value (Argon2id via the {@code
 * DelegatingPasswordEncoder}, ADR-012) — this type never carries a raw password. Callers that
 * render a response DTO must never include it (ADR-002/ADR-012: never return {@code
 * password_hash}).
 *
 * @param id stable account identifier; also the {@code user_id} FK target for every owner-scoped
 *     table (ADR-011/ADR-012)
 * @param email unique login identifier
 * @param passwordHash the Argon2id (or delegating-encoder) hash — never the raw password
 * @param createdAt when the account was created
 * @param lastLoginAt when the account last authenticated successfully; {@code null} if never
 * @param active whether the account can currently authenticate (the seeded legacy placeholder
 *     starts {@code false} until {@code LegacyUserBootstrap} activates it)
 */
public record User(
    UUID id,
    String email,
    String passwordHash,
    Instant createdAt,
    Instant lastLoginAt,
    boolean active) {}
