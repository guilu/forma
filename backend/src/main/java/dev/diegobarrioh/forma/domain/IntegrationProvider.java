package dev.diegobarrioh.forma.domain;

/**
 * A provider-neutral, externally-integrated data source (FOR-126, first slice of FOR-103).
 *
 * <p>Resolves spec FOR-103/FOR-126's open question ("which providers seed the status list?") by
 * seeding all three: the FOR-57 Integraciones UI already renders exactly these three ({@code
 * frontend/src/api/integrations.ts} mock fixture) — matching that surface keeps {@code GET
 * /api/v1/integrations} a drop-in replacement for the mock with zero frontend changes, which is
 * this slice's whole purpose (unblocks FOR-123).
 */
public enum IntegrationProvider {
  WITHINGS,
  GOOGLE_FIT,
  APPLE_HEALTH
}
