package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.User;
import dev.diegobarrioh.forma.domain.UserRole;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Application use cases for account registration and login bookkeeping (FOR-145, ADR-012).
 *
 * <p>Public self-registration: any client can create a new account with no invite/admin gate (spec
 * FOR-145 "Public self-registration"). Password hashing always goes through the injected {@link
 * PasswordEncoder} (wired to the Argon2id {@code DelegatingPasswordEncoder} in {@code
 * SecurityConfig}) — this service never hashes with anything else, and in particular never with
 * {@code AesGcmTokenCipher} (ADR-012 explicit prohibition: that cipher is reversible and exists
 * only for OAuth provider tokens).
 *
 * <p>Actual credential verification during login is delegated to Spring Security's {@code
 * AuthenticationManager}/{@code DaoAuthenticationProvider} (wired in {@code SecurityConfig},
 * invoked from {@code AuthController}) — this service only records the successful-login side effect
 * ({@link #recordSuccessfulLogin}), keeping servlet/session concerns out of the application layer
 * (ADR-001).
 */
@Service
public class UserService {

  /**
   * Minimum password length (spec FOR-145 "Weak/invalid input rejected"). Enforced both here
   * (defense-in-depth for any caller that bypasses bean validation) and by {@code
   * RegisterRequest}'s {@code @Size(min = MIN_PASSWORD_LENGTH)}.
   */
  public static final int MIN_PASSWORD_LENGTH = 12;

  private final UserRepository repository;
  private final PasswordEncoder passwordEncoder;

  public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Registers a new account.
   *
   * @throws ConflictException if an account with this email already exists (spec: "Duplicate email
   *     rejected")
   * @throws ValidationException if the password fails the minimum length policy (spec:
   *     "Weak/invalid input rejected")
   */
  public User register(String email, String rawPassword) {
    String normalizedEmail = email == null ? null : email.trim().toLowerCase();
    if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
      throw new ValidationException(
          "La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
    }
    if (repository.existsByEmail(normalizedEmail)) {
      throw new ConflictException("Ya existe una cuenta con ese email");
    }
    UUID id = UUID.randomUUID();
    String hash = passwordEncoder.encode(rawPassword);
    repository.insert(id, normalizedEmail, hash);
    // Registration always creates an ordinary account: an admin is granted deliberately
    // afterwards (FOR-190), never by being the first to sign up.
    return new User(id, normalizedEmail, hash, Instant.now(), null, true, UserRole.USER, null);
  }

  /** Looks up an account by id (e.g. to build {@code AuthUserResponse} for the current caller). */
  public User findById(UUID id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("No existe la cuenta: " + id));
  }

  /** Records a successful login's timestamp (spec: "last_login_at MUST update"). */
  public void recordSuccessfulLogin(UUID id) {
    repository.updateLastLoginAt(id, Instant.now());
  }

  /**
   * Resolves a Google OIDC login into a FORMA account (ADR-012 addendum, migration V62): finds the
   * account already linked to the Google subject; failing that, links the subject to an existing
   * account found by email (self-registered previously, now also signing in with Google); failing
   * that, creates a new Google-only account. Never called with an unverified email — Google's own
   * account-recovery/verification is the only thing this trusts to prove ownership of the address.
   *
   * @throws UnauthorizedException if {@code identity.emailVerified()} is false, the resolved
   *     account is not active, or the account found by email is already linked to a
   *     <em>different</em> Google subject (re-link takeover guard — an existing link is never
   *     silently reassigned)
   */
  public User loginWithGoogle(GoogleIdentity identity) {
    if (!identity.emailVerified()) {
      throw new UnauthorizedException("El email de Google no está verificado");
    }
    String normalizedEmail =
        identity.email() == null ? null : identity.email().trim().toLowerCase();
    return resolveGoogleUser(identity.subject(), normalizedEmail, true);
  }

  /**
   * @param allowRetry whether a unique-constraint race (two concurrent first Google logins for the
   *     same account) may retry once by re-resolving instead of propagating — akademia's documented
   *     pattern for this exact race (see task notes), since the loser's insert/link failing means
   *     the winner's row now exists to find.
   */
  private User resolveGoogleUser(String subject, String normalizedEmail, boolean allowRetry) {
    Optional<User> bySubject = repository.findByGoogleSubject(subject);
    if (bySubject.isPresent()) {
      return requireActive(bySubject.get());
    }

    Optional<User> byEmail = repository.findByEmail(normalizedEmail);
    if (byEmail.isPresent()) {
      User existing = requireActive(byEmail.get());
      String existingSubject = existing.googleSubject();

      // Re-link takeover guard: an account already linked to a DIFFERENT Google subject must
      // never be silently reassigned to a new one — that would let a second Google identity take
      // over an existing account's email-matched row. Rejected before any write is attempted
      // (JdbcUserRepository#linkGoogleSubject also refuses this at the SQL level, defense-in-depth,
      // but the common, expected path never reaches it).
      if (existingSubject != null && !existingSubject.equals(subject)) {
        throw new UnauthorizedException("Esa cuenta ya está enlazada a otra identidad de Google");
      }

      // Already linked to this exact subject (only reachable via a stale/inconsistent read, since
      // a matching subject would normally have been found by findByGoogleSubject above already) —
      // a safe no-op, not a re-link.
      if (existingSubject == null) {
        boolean linked;
        try {
          linked = repository.linkGoogleSubject(existing.id(), subject);
        } catch (DataIntegrityViolationException ex) {
          if (!allowRetry) {
            throw ex;
          }
          return resolveGoogleUser(subject, normalizedEmail, false);
        }
        if (!linked) {
          // 0 rows affected: a concurrent login already linked this row (to some subject) between
          // our read and this write. Re-resolving finds the current truth — either that subject is
          // ours (no-op) or a different one (rejected by the guard above).
          if (!allowRetry) {
            throw new UnauthorizedException(
                "Esa cuenta ya está enlazada a otra identidad de Google");
          }
          return resolveGoogleUser(subject, normalizedEmail, false);
        }
      }

      return new User(
          existing.id(),
          existing.email(),
          existing.passwordHash(),
          existing.createdAt(),
          existing.lastLoginAt(),
          existing.active(),
          existing.role(),
          subject);
    }

    UUID id = UUID.randomUUID();
    try {
      repository.insertWithGoogleSubject(id, normalizedEmail, subject);
    } catch (DataIntegrityViolationException ex) {
      if (!allowRetry) {
        throw ex;
      }
      return resolveGoogleUser(subject, normalizedEmail, false);
    }
    return new User(id, normalizedEmail, null, Instant.now(), null, true, UserRole.USER, subject);
  }

  private User requireActive(User user) {
    if (!user.active()) {
      throw new UnauthorizedException("Invalid credentials");
    }
    return user;
  }
}
