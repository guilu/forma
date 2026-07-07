package dev.diegobarrioh.forma.domain;

/**
 * Equipment required by a strength {@link Exercise} (FOR-24).
 *
 * <p>Restricted to home-friendly equipment (docs/domain-model.md); machine/gym-only equipment is
 * deliberately absent so the catalog matches what is available at home.
 */
public enum Equipment {
  DUMBBELL,
  BENCH,
  BAND,
  PULL_UP_BAR,
  BODYWEIGHT
}
