package dev.diegobarrioh.forma.adapter.link;

import dev.diegobarrioh.forma.application.LinkPreview;
import dev.diegobarrioh.forma.application.ValidationException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Finds the product photo a page advertises (FOR-200), so an admin adding a product by hand does
 * not have to hunt for an image URL.
 *
 * <p>Best effort, and it says so: most shops publish an Open Graph image and this finds it; some do
 * not, and then the admin pastes a URL into the field that is there anyway. Nothing here is load
 * bearing.
 *
 * <p><strong>The URL comes from a form.</strong> A server that fetches whatever it is handed is a
 * way to read what only the server can reach — a cloud metadata endpoint, an admin page on
 * localhost, a service on the private network. {@link #requirePublicHttpUrl} refuses those before a
 * socket is opened, and the fetcher re-checks after every redirect, because a public host is free
 * to redirect to a private one.
 */
@Component
public class HttpLinkPreview implements LinkPreview {

  /** Both spellings of the tag, either attribute order, either quote. */
  private static final Pattern META_IMAGE =
      Pattern.compile(
          "<meta[^>]+(?:property|name)\\s*=\\s*[\"'](?:og:image|twitter:image)[\"'][^>]*"
              + "content\\s*=\\s*[\"']([^\"']+)[\"']"
              + "|<meta[^>]+content\\s*=\\s*[\"']([^\"']+)[\"'][^>]*"
              + "(?:property|name)\\s*=\\s*[\"'](?:og:image|twitter:image)[\"']",
          Pattern.CASE_INSENSITIVE);

  /**
   * Amazon publishes no Open Graph image — a real product page answers 200 with 2.7 MB of HTML and
   * not one og: tag; the photo is inside an embedded JSON blob. Site-shaped and brittle by nature,
   * which is why it only runs when the general rule found nothing.
   */
  private static final Pattern AMAZON_IMAGE =
      Pattern.compile("\"large\"\\s*:\\s*\"(https://m\\.media-amazon\\.com/images/[^\"]+)\"");

  /**
   * Turns a host into the addresses it answers on. A seam, not an abstraction for its own sake: the
   * refusal below is the part worth testing, and a test that has to resolve a real name to exercise
   * it is a test that fails on a train.
   */
  @FunctionalInterface
  interface HostResolver {
    InetAddress[] resolve(String host) throws UnknownHostException;
  }

  private final PageFetcher fetcher;
  private final HostResolver resolver;

  // Explicit because there are two: with more than one constructor and no
  // annotation, Spring asks for a no-arg one that should not exist.
  @Autowired
  public HttpLinkPreview(PageFetcher fetcher) {
    this(fetcher, InetAddress::getAllByName);
  }

  HttpLinkPreview(PageFetcher fetcher, HostResolver resolver) {
    this.fetcher = fetcher;
    this.resolver = resolver;
  }

  @Override
  public Optional<String> imageFor(String pageUrl) {
    requirePublicHttpUrl(pageUrl, resolver);
    return fetcher
        .fetch(pageUrl)
        .flatMap(HttpLinkPreview::findImage)
        .filter(HttpLinkPreview::isHttp);
  }

  private static Optional<String> findImage(String body) {
    Matcher meta = META_IMAGE.matcher(body);
    if (meta.find()) {
      return Optional.ofNullable(meta.group(1) != null ? meta.group(1) : meta.group(2));
    }
    Matcher amazon = AMAZON_IMAGE.matcher(body);
    return amazon.find() ? Optional.of(amazon.group(1)) : Optional.empty();
  }

  private static boolean isHttp(String url) {
    String lower = url.toLowerCase(Locale.ROOT);
    return lower.startsWith("http://") || lower.startsWith("https://");
  }

  /**
   * Refuses anything that is not a public http(s) address.
   *
   * @throws ValidationException with a message safe to show: it never echoes what the host resolved
   *     to, which would turn this endpoint into a way to map the private network
   */
  static void requirePublicHttpUrl(String url, HostResolver resolver) {
    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException ex) {
      throw new ValidationException("El enlace no es una URL válida");
    }
    if (uri.getScheme() == null || !isHttp(uri.getScheme() + "://")) {
      throw new ValidationException("Solo se pueden leer enlaces http o https");
    }
    String host = uri.getHost();
    if (host == null || host.isBlank()) {
      throw new ValidationException("El enlace no tiene un dominio");
    }
    InetAddress[] addresses;
    try {
      addresses = resolver.resolve(host);
    } catch (UnknownHostException ex) {
      throw new ValidationException("No se pudo resolver el dominio del enlace");
    }
    for (InetAddress address : addresses) {
      // Every address the name resolves to, not just the first: a host that
      // answers with one public and one private address would otherwise pass.
      if (address.isLoopbackAddress()
          || address.isAnyLocalAddress()
          || address.isLinkLocalAddress()
          || address.isSiteLocalAddress()
          || address.isMulticastAddress()) {
        throw new ValidationException("Ese enlace apunta a una dirección interna");
      }
    }
  }
}
