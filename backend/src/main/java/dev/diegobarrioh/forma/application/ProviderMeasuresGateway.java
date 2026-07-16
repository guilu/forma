package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.IntegrationProvider;
import java.time.Instant;
import java.util.List;

/**
 * Provider-neutral port for fetching a provider's body-composition measures (FOR-132, ADR-004:
 * "External integrations will be implemented as adapters behind provider-neutral application
 * ports"), mirroring how {@link ProviderOAuthGateway} multiplexes by {@link #provider()} (FOR-131).
 * {@code adapter/withings}'s {@code WithingsMeasuresAdapter} is the only implementation this story
 * registers; a future Google Fit/Apple Health story (explicitly out of scope here) would add
 * further implementations behind this same port, never change it.
 *
 * <p>No provider-specific type (request/response JSON shape, measure-type codes, ...) appears in
 * this interface's signature — those stay inside the adapter (ADR-004 Rules: "Do not store provider
 * payloads as the primary domain model"). {@link ImportedMeasureGroup} is the provider-neutral
 * result shape every implementation must map its provider's response into; mapping and unit
 * conversion happen entirely inside the adapter (spec FOR-132: "Getmeas call +
 * payload→BodyMeasurement mapping live in the Withings adapter").
 */
public interface ProviderMeasuresGateway {

  /** Which {@link IntegrationProvider} this gateway fetches measures for. */
  IntegrationProvider provider();

  /**
   * Fetches measure groups for the authenticated user, mapped and ready to persist.
   *
   * <p>Groups the caller cannot construct a valid {@code BodyMeasurement} from (e.g. no weight
   * measure present — see {@code BodyMeasurement}'s constructor validation) or that carry only
   * measure types this codebase doesn't model (e.g. Withings bone mass, type 88) are silently
   * omitted from the result — never partially fabricated, never a crash (spec FOR-132 Edge Cases).
   *
   * @param accessToken the caller's valid (already-refreshed) provider access token; never logged
   * @param since when non-{@code null}, fetch only groups updated at or after this instant
   *     (incremental sync); when {@code null}, fetch full history (first sync)
   * @return the fetched, mapped measure groups; empty if the provider has nothing new
   * @throws ProviderSyncException if the provider is unreachable, rate-limited, returns a 5xx, or
   *     returns a response this adapter cannot parse
   */
  List<ImportedMeasureGroup> fetchMeasureGroups(String accessToken, Instant since);
}
