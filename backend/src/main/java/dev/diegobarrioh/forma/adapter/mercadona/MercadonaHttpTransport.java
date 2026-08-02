package dev.diegobarrioh.forma.adapter.mercadona;

/**
 * The one thing {@link MercadonaCatalogAdapter} needs from the network (FOR-194): fetch a URL, get
 * a body back.
 *
 * <p>Same seam as {@code WithingsHttpTransport} (FOR-131), and for the same reason: it keeps the
 * crawling and the parsing — everything worth testing — reachable by a test that never opens a
 * socket.
 */
public interface MercadonaHttpTransport {

  /**
   * @return the response body
   * @throws dev.diegobarrioh.forma.application.StoreCatalogUnavailableException on any transport
   *     failure or non-2xx status
   */
  String get(String url);
}
