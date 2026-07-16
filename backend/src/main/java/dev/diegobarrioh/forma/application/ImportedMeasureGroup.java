package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.BodyMeasurement;
import java.util.Objects;

/**
 * One provider measure group already mapped to a normalized {@link BodyMeasurement}, paired with
 * the provider's own group identifier so {@link IntegrationService#sync} can dedup against {@link
 * ImportedMeasureMarkerStore} (FOR-132, ADR-004: "Duplicate detection is mandatory for imported
 * records").
 *
 * <p>This type crosses the application/adapter boundary the same way {@link ExchangedTokens} does
 * (FOR-131) — it exists so a {@link ProviderMeasuresGateway} can hand back both the normalized
 * measurement and the provider-specific dedup key in one shape, without leaking that key onto
 * {@link BodyMeasurement} itself (spec FOR-132: "Do NOT add an external id to BodyMeasurement —
 * markers live in the Integrations adapter"). It never reaches a delivery-layer response DTO.
 *
 * @param externalGroupId the provider's own identifier for this measure group (Withings {@code
 *     grpid}); opaque outside the adapter, used only as {@link ImportedMeasureMarkerStore}'s dedup
 *     key
 * @param measurement the already-mapped, provider-clean measurement
 */
public record ImportedMeasureGroup(long externalGroupId, BodyMeasurement measurement) {

  public ImportedMeasureGroup {
    Objects.requireNonNull(measurement, "measurement must not be null");
  }
}
