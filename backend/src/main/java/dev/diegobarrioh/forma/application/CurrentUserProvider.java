package dev.diegobarrioh.forma.application;

import java.util.UUID;

/**
 * Port resolving the authenticated caller's stable account id (FOR-145, ADR-012). Replaces the 11
 * duplicated {@code OWNER_ID = "default-user"} constants (e.g. {@link GoalService#OWNER_ID}) with
 * the real per-request principal.
 *
 * <p>Implemented by {@code delivery.security.SecurityContextCurrentUserProvider}, which reads
 * {@code SecurityContextHolder}. Kept as a port here (ADR-001) so application-layer services never
 * depend on Spring Security types directly — only on this narrow contract.
 *
 * <p>Introduced in this slice (145a) but <b>not yet wired</b> into the 11 existing owner-scoped
 * services — that wiring is the next slice (145b), so the diff here stays reviewable and this port
 * can be exercised end to end (e.g. via {@code AuthController#me}) before the broader rollout.
 */
public interface CurrentUserProvider {

  /**
   * @return the authenticated caller's account id
   * @throws UnauthorizedException if no authenticated caller is present
   */
  UUID currentUserId();
}
