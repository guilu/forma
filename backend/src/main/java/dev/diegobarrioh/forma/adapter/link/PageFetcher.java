package dev.diegobarrioh.forma.adapter.link;

import java.util.Optional;

/**
 * Fetches a web page as text (FOR-200). The one thing {@link HttpLinkPreview} needs from the
 * network, split out so the parsing and — more importantly — the refusing can be tested without
 * opening a socket.
 *
 * @see JdkHttpPageFetcher for the size cap and the timeouts
 */
public interface PageFetcher {

  /** The page body, or empty when the site answered with something unusable. */
  Optional<String> fetch(String url);
}
