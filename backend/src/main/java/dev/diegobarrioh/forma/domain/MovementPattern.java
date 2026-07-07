package dev.diegobarrioh.forma.domain;

/**
 * Movement pattern of a strength {@link Exercise} (FOR-24).
 *
 * <p>Closed classification per docs/domain-model.md. {@code CARRY} from domain-model is
 * intentionally omitted for now — no seeded exercise needs it (spec FOR-24 Open Questions); it can
 * be added later without breaking the contract, as with {@link MeasurementSource}.
 */
public enum MovementPattern {
  PUSH,
  PULL,
  SQUAT,
  HINGE,
  CORE
}
