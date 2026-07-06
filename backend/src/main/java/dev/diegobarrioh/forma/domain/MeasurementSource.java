package dev.diegobarrioh.forma.domain;

/**
 * Classifies where a {@link BodyMeasurement} originated.
 *
 * <p>A measurement is entered manually today ({@link #MANUAL}). This is a classification marker
 * only, never a payload carrier: provider-specific details (tokens, external ids, sync metadata)
 * belong to Integrations adapters, not to the Body domain (docs/architecture-overview.md,
 * docs/domain-model.md).
 *
 * <p>New sources (for example an external Withings import in a later FOR-2 story) can be added here
 * without breaking this contract, because the type is internal to the domain (spec FOR-15 Open
 * Questions).
 */
public enum MeasurementSource {
  MANUAL
}
