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
    passwordEncoder = new SecurityConfig().passwordEncoder();
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
    User stored = new User(id, "a@x.com", "{argon2}hash", null, null, true);
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
}
