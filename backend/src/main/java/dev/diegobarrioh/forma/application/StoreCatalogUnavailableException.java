package dev.diegobarrioh.forma.application;

/**
 * A store's own catalogue could not be read (FOR-194).
 *
 * <p>Separate from {@link NotFoundException} because it says something different: the food and the
 * chain are both fine, the shop is not answering. Distinguishing them is what lets the screen say
 * "vuelve a intentarlo" instead of "no existe".
 *
 * <p>Imports run against an API nobody promised us — undocumented, unversioned, and free to change
 * or refuse at any time. This exception is that fact made explicit, so a failure degrades the
 * import button and never the catalog screens around it.
 */
public class StoreCatalogUnavailableException extends RuntimeException {

  public StoreCatalogUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }

  public StoreCatalogUnavailableException(String message) {
    super(message);
  }
}
