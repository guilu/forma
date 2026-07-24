package dev.diegobarrioh.forma.delivery.security;

import dev.diegobarrioh.forma.application.UserRepository;
import dev.diegobarrioh.forma.domain.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Loads accounts by email for Spring Security's {@code DaoAuthenticationProvider} (FOR-145,
 * ADR-012). Delegates lookup to {@link UserRepository} (the application-layer port) and adapts the
 * result into a {@link FormaUserPrincipal}; never touches SQL/JDBC directly (ADR-001).
 */
@Component
public class FormaUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  public FormaUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    User user =
        userRepository
            .findByEmail(email == null ? null : email.trim().toLowerCase())
            .orElseThrow(() -> new UsernameNotFoundException("No existe la cuenta"));
    return FormaUserPrincipal.from(user);
  }
}
