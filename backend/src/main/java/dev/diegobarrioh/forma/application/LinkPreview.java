package dev.diegobarrioh.forma.application;

import java.util.Optional;

/**
 * Port over "what photo does this page advertise" (FOR-200). Owned by the application side;
 * adapters implement it (ADR-001).
 *
 * <p>A convenience for an admin typing a product by hand, never a dependency: a page that publishes
 * nothing, or a site that refuses to answer, leaves the field empty and the admin pastes a URL.
 */
public interface LinkPreview {

  /**
   * The product image the page at {@code pageUrl} advertises, if any.
   *
   * @throws ValidationException when the URL is not a public http(s) address
   */
  Optional<String> imageFor(String pageUrl);
}
