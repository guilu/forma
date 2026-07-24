package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.User;
import java.time.Instant;
import java.util.UUID;
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
    return new User(id, normalizedEmail, hash, Instant.now(), null, true);
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
}
