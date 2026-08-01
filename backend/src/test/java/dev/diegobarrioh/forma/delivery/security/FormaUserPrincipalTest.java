package dev.diegobarrioh.forma.delivery.security;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.domain.User;
import dev.diegobarrioh.forma.domain.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

/**
 * Authority mapping (FOR-190). FORMA had no roles at all: every authenticated account carried a
 * hardcoded {@code ROLE_USER}. The admin screens need a second one, and it has to come from the
 * account rather than from anything implicit.
 */
class FormaUserPrincipalTest {

  private static User user(UserRole role) {
    return new User(
        UUID.randomUUID(), "someone@forma.test", "hash", Instant.now(), null, true, role);
  }

  @Test
  void everyAccountCarriesTheUserAuthority() {
    FormaUserPrincipal principal = FormaUserPrincipal.from(user(UserRole.USER));

    assertThat(principal.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("ROLE_USER");
  }

  /**
   * An admin is also a user: the admin screens are additions to the app, not a separate one, so
   * every ordinary endpoint keeps working for them without a second rule.
   */
  @Test
  void anAdminCarriesBothAuthorities() {
    FormaUserPrincipal principal = FormaUserPrincipal.from(user(UserRole.ADMIN));

    assertThat(principal.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
  }
}
