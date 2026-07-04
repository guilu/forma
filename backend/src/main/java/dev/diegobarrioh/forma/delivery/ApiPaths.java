package dev.diegobarrioh.forma.delivery;

/**
 * Central definition of the versioned API base path (FOR-88, ADR-005).
 *
 * <p>All product controllers mount under {@link #V1} so the versioning strategy stays consistent as
 * endpoints are added by later stories.
 */
public final class ApiPaths {

  /** Base path for version 1 of the FORMA REST API. */
  public static final String V1 = "/api/v1";

  private ApiPaths() {}
}
