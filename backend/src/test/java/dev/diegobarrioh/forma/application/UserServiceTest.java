package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.diegobarrioh.forma.config.SecurityConfig;
import dev.diegobarrioh.forma.domain.User;
import dev.diegobarrioh.forma.domain.UserRole;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for {@link UserService} (FOR-145, ADR-012 spec "Public self-registration"). Uses the
 * real Argon2id {@link PasswordEncoder} wired by {@link SecurityConfig} (not a fake) so the
 * "Argon2id-hashed" assertion is meaningful, and a mocked {@link UserRepository}.
 */
class UserServiceTest {

  private static final String VALID_PASSWORD = "Str0ngP@ssw0rd!";

  private UserRepository repository;
  private PasswordEncoder passwordEncoder;
  private UserService service;

  @BeforeEach
  void setUp() {
    repository = Mockito.mock(UserRepository.class);
    passwordEncoder = new SecurityConfig("", "", "http://localhost:5173").passwordEncoder();
    service = new UserService(repository, passwordEncoder);
  }

  @Test
  void registerHashesThePasswordWithArgon2idAndInsertsANewAccount() {
    when(repository.existsByEmail("a@x.com")).thenReturn(false);

    User created = service.register("a@x.com", VALID_PASSWORD);

    assertThat(created.email()).isEqualTo("a@x.com");
    assertThat(created.id()).isNotNull();
    verify(repository, times(1)).insert(eq(created.id()), eq("a@x.com"), anyString());
    // Argon2id via the DelegatingPasswordEncoder is stored with the "{argon2}" id prefix — proof
    // this went through Argon2id, never a reversible cipher (ADR-012 explicit prohibition).
    assertThat(created.passwordHash()).startsWith("{argon2}");
    assertThat(created.passwordHash()).isNotEqualTo(VALID_PASSWORD);
    assertThat(passwordEncoder.matches(VALID_PASSWORD, created.passwordHash())).isTrue();
  }

  @Test
  void registerNormalizesEmailToLowercaseAndTrimmed() {
    when(repository.existsByEmail("a@x.com")).thenReturn(false);

    User created = service.register("  A@X.com  ", VALID_PASSWORD);

    assertThat(created.email()).isEqualTo("a@x.com");
  }

  @Test
  void registerRejectsADuplicateEmailWithConflictAndNeverInserts() {
    when(repository.existsByEmail("a@x.com")).thenReturn(true);

    assertThatThrownBy(() -> service.register("a@x.com", VALID_PASSWORD))
        .isInstanceOf(ConflictException.class);

    verify(repository, never()).insert(any(), anyString(), anyString());
  }

  @Test
  void registerRejectsAPasswordShorterThanTheMinimumWithoutTouchingTheRepository() {
    assertThatThrownBy(() -> service.register("a@x.com", "short"))
        .isInstanceOf(ValidationException.class);

    verify(repository, never()).existsByEmail(anyString());
    verify(repository, never()).insert(any(), anyString(), anyString());
  }

  @Test
  void findByIdReturnsTheStoredAccount() {
    UUID id = UUID.randomUUID();
    User stored = new User(id, "a@x.com", "{argon2}hash", null, null, true, UserRole.USER, null);
    when(repository.findById(id)).thenReturn(Optional.of(stored));

    assertThat(service.findById(id)).isEqualTo(stored);
  }

  @Test
  void findByIdOfAnUnknownAccountThrowsNotFound() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findById(id)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void recordSuccessfulLoginUpdatesLastLoginAt() {
    UUID id = UUID.randomUUID();

    service.recordSuccessfulLogin(id);

    verify(repository, times(1)).updateLastLoginAt(eq(id), any());
  }

  @Test
  void loginWithGoogleRejectsAnUnverifiedEmailWithoutTouchingTheRepository() {
    GoogleIdentity identity = new GoogleIdentity("google-sub-1", "a@x.com", false);

    assertThatThrownBy(() -> service.loginWithGoogle(identity))
        .isInstanceOf(UnauthorizedException.class);

    verify(repository, never()).findByGoogleSubject(anyString());
    verify(repository, never()).findByEmail(anyString());
  }

  @Test
  void loginWithGoogleReturnsTheAccountAlreadyLinkedToThatSubject() {
    GoogleIdentity identity = new GoogleIdentity("google-sub-2", "a@x.com", true);
    UUID id = UUID.randomUUID();
    User linked = new User(id, "a@x.com", null, null, null, true, UserRole.USER, "google-sub-2");
    when(repository.findByGoogleSubject("google-sub-2")).thenReturn(Optional.of(linked));

    User result = service.loginWithGoogle(identity);

    assertThat(result).isEqualTo(linked);
    verify(repository, never()).findByEmail(anyString());
    verify(repository, never()).insertWithGoogleSubject(any(), anyString(), anyString());
    verify(repository, never()).linkGoogleSubject(any(), anyString());
  }

  @Test
  void loginWithGoogleLinksTheSubjectToAnExistingAccountFoundByEmail() {
    GoogleIdentity identity = new GoogleIdentity("google-sub-3", "existing@x.com", true);
    UUID id = UUID.randomUUID();
    User existing =
        new User(id, "existing@x.com", "{argon2}hash", null, null, true, UserRole.USER, null);
    when(repository.findByGoogleSubject("google-sub-3")).thenReturn(Optional.empty());
    when(repository.findByEmail("existing@x.com")).thenReturn(Optional.of(existing));
    when(repository.linkGoogleSubject(id, "google-sub-3")).thenReturn(true);

    User result = service.loginWithGoogle(identity);

    verify(repository, times(1)).linkGoogleSubject(id, "google-sub-3");
    assertThat(result.id()).isEqualTo(id);
    assertThat(result.googleSubject()).isEqualTo("google-sub-3");
    // Linking never touches the password an account already had.
    assertThat(result.passwordHash()).isEqualTo("{argon2}hash");
  }

  /**
   * Regression test (re-link takeover finding, CRITICAL): an account already linked to a
   * *different* Google subject must never be silently re-linked to a new one — that would let
   * someone reassign an existing account's identity out from under it. Rejected before any
   * repository write is attempted.
   */
  @Test
  void loginWithGoogleRejectsAnAccountFoundByEmailAlreadyLinkedToADifferentSubject() {
    GoogleIdentity identity = new GoogleIdentity("attacker-sub", "existing@x.com", true);
    UUID id = UUID.randomUUID();
    User alreadyLinked =
        new User(
            id, "existing@x.com", "{argon2}hash", null, null, true, UserRole.USER, "original-sub");
    when(repository.findByGoogleSubject("attacker-sub")).thenReturn(Optional.empty());
    when(repository.findByEmail("existing@x.com")).thenReturn(Optional.of(alreadyLinked));

    assertThatThrownBy(() -> service.loginWithGoogle(identity))
        .isInstanceOf(UnauthorizedException.class);

    verify(repository, never()).linkGoogleSubject(any(), anyString());
    verify(repository, never()).insertWithGoogleSubject(any(), anyString(), anyString());
  }

  /**
   * If the account found by email already carries the *same* subject we are trying to link
   * (reachable only through an inconsistent read, since a matching subject would normally have been
   * found by {@code findByGoogleSubject} first), linking is a safe no-op — never rejected as a
   * takeover, and never re-issues a redundant write.
   */
  @Test
  void loginWithGoogleTreatsRelinkingTheSameSubjectAsANoOp() {
    GoogleIdentity identity = new GoogleIdentity("same-sub", "existing@x.com", true);
    UUID id = UUID.randomUUID();
    User alreadyLinkedToSameSubject =
        new User(id, "existing@x.com", "{argon2}hash", null, null, true, UserRole.USER, "same-sub");
    when(repository.findByGoogleSubject("same-sub")).thenReturn(Optional.empty());
    when(repository.findByEmail("existing@x.com"))
        .thenReturn(Optional.of(alreadyLinkedToSameSubject));

    User result = service.loginWithGoogle(identity);

    assertThat(result.id()).isEqualTo(id);
    assertThat(result.googleSubject()).isEqualTo("same-sub");
    verify(repository, never()).linkGoogleSubject(any(), anyString());
  }

  @Test
  void loginWithGoogleCreatesANewActiveUserRoleAccountWhenNoneExists() {
    GoogleIdentity identity = new GoogleIdentity("google-sub-4", "new@x.com", true);
    when(repository.findByGoogleSubject("google-sub-4")).thenReturn(Optional.empty());
    when(repository.findByEmail("new@x.com")).thenReturn(Optional.empty());

    User result = service.loginWithGoogle(identity);

    verify(repository, times(1))
        .insertWithGoogleSubject(eq(result.id()), eq("new@x.com"), eq("google-sub-4"));
    assertThat(result.email()).isEqualTo("new@x.com");
    assertThat(result.passwordHash()).isNull();
    assertThat(result.active()).isTrue();
    assertThat(result.role()).isEqualTo(UserRole.USER);
    assertThat(result.googleSubject()).isEqualTo("google-sub-4");
  }

  @Test
  void loginWithGoogleRejectsAnInactiveAccountFoundBySubject() {
    GoogleIdentity identity = new GoogleIdentity("google-sub-5", "inactive@x.com", true);
    User inactive =
        new User(
            UUID.randomUUID(),
            "inactive@x.com",
            null,
            null,
            null,
            false,
            UserRole.USER,
            "google-sub-5");
    when(repository.findByGoogleSubject("google-sub-5")).thenReturn(Optional.of(inactive));

    assertThatThrownBy(() -> service.loginWithGoogle(identity))
        .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void loginWithGoogleRejectsAnInactiveAccountFoundByEmail() {
    GoogleIdentity identity = new GoogleIdentity("google-sub-6", "inactive2@x.com", true);
    User inactive =
        new User(
            UUID.randomUUID(),
            "inactive2@x.com",
            "{argon2}hash",
            null,
            null,
            false,
            UserRole.USER,
            null);
    when(repository.findByGoogleSubject("google-sub-6")).thenReturn(Optional.empty());
    when(repository.findByEmail("inactive2@x.com")).thenReturn(Optional.of(inactive));

    assertThatThrownBy(() -> service.loginWithGoogle(identity))
        .isInstanceOf(UnauthorizedException.class);

    verify(repository, never()).linkGoogleSubject(any(), anyString());
  }

  @Test
  void loginWithGoogleRetriesOnceWhenACreateRaceLosesTheUniqueConstraint() {
    GoogleIdentity identity = new GoogleIdentity("google-sub-7", "race@x.com", true);
    UUID winnerId = UUID.randomUUID();
    User winner =
        new User(winnerId, "race@x.com", null, null, null, true, UserRole.USER, "google-sub-7");
    when(repository.findByGoogleSubject("google-sub-7"))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(winner));
    when(repository.findByEmail("race@x.com")).thenReturn(Optional.empty());
    org.mockito.Mockito.doThrow(new org.springframework.dao.DataIntegrityViolationException("dup"))
        .when(repository)
        .insertWithGoogleSubject(any(), eq("race@x.com"), eq("google-sub-7"));

    User result = service.loginWithGoogle(identity);

    assertThat(result).isEqualTo(winner);
    verify(repository, times(1))
        .insertWithGoogleSubject(any(), eq("race@x.com"), eq("google-sub-7"));
  }
}
