package dev.diegobarrioh.forma.domain;

/**
 * Classifies where a {@link BodyMeasurement} originated.
 *
 * <p>A measurement is entered manually ({@link #MANUAL}) or imported from Withings ({@link
 * #WITHINGS}, FOR-132 — real Getmeas sync, slice 3 of FOR-103). This is a classification marker
 * only, never a payload carrier: provider-specific details (tokens, external ids, sync metadata)
 * belong to Integrations adapters, not to the Body domain (docs/architecture-overview.md,
 * docs/domain-model.md) — {@link #WITHINGS} carries no Withings measure-group id or any other
 * provider identifier; that dedup key lives entirely in the integrations-side markers table
 * (ADR-004).
 *
 * <p>New sources can be added here without breaking this contract, because the type is internal to
 * the domain (spec FOR-15 Open Questions).
 */
public enum MeasurementSource {
  MANUAL,
  WITHINGS
}
