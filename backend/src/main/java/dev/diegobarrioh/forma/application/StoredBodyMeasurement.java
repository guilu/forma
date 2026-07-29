package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.BodyMeasurement;
import java.util.UUID;

/**
 * A stored {@link BodyMeasurement} together with the id of the row holding it (FOR-187).
 *
 * <p>Exists so a caller can name one measurement among several — to delete it — without the domain
 * type growing an identity. {@link BodyMeasurement} is a value object (FOR-15): it is constructed
 * in insights, weekly summaries and the Withings import, none of which have or want a database id.
 * Pairing the two here keeps the id at the edge, where it belongs, and leaves those call sites
 * untouched.
 *
 * <p>Application-layer, not delivery: the repository port speaks it, and the delivery layer maps it
 * into its own read model (ADR-005).
 *
 * @param id the {@code body_measurements} row's primary key
 * @param measurement the measurement itself, unchanged
 */
public record StoredBodyMeasurement(UUID id, BodyMeasurement measurement) {}
