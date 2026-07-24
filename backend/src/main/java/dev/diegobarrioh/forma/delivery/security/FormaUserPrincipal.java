package dev.diegobarrioh.forma.delivery.security;

import dev.diegobarrioh.forma.domain.User;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security principal adapting a {@link User} account (FOR-145, ADR-012). Framework boundary:
 * {@link User} stays framework-free (ADR-001); this type only exists in the delivery layer to
 * satisfy {@link UserDetails}, consumed by {@link FormaUserDetailsService} and read back by {@link
 * SecurityContextCurrentUserProvider}.
 *
 * <p>Every authenticated account carries a single fixed authority, {@code ROLE_USER} — FORMA has no
 * role hierarchy yet.
 */
public final class FormaUserPrincipal implements UserDetails {

  private static final long serialVersionUID = 1L;

  private final UUID id;
  private final String email;
  private final String passwordHash;
  private final boolean active;

  public FormaUserPrincipal(UUID id, String email, String passwordHash, boolean active) {
    this.id = id;
    this.email = email;
    this.passwordHash = passwordHash;
    this.active = active;
  }

  public static FormaUserPrincipal from(User user) {
    return new FormaUserPrincipal(user.id(), user.email(), user.passwordHash(), user.active());
  }

  public UUID id() {
    return id;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_USER"));
  }

  @Override
  public String getPassword() {
    return passwordHash;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return active;
  }
}
