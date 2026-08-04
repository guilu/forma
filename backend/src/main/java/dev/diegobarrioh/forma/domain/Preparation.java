package dev.diegobarrioh.forma.domain;

/**
 * What state a food's numbers describe (V51).
 *
 * <p>The catalog held macros with no word about whether they were measured before or after cooking,
 * and the two differ enough to make the numbers unusable without it: rice is 360 kcal/100 g dry and
 * about 130 cooked. Nothing said which, so nobody could tell whether to weigh it dry or cooked, and
 * the source document for this redesign got it wrong — its "100 g arroz = 250 g patata" only works
 * if the rice was cooked, while the catalog holds it dry, a difference of nearly twofold.
 *
 * <p>Three states, because a food either goes into the kitchen, comes out of it, or never passes
 * through. That is a small closed idea, so this is an enum with a CHECK rather than a table: unlike
 * the food groups (V43), no amount of curating produces a fourth.
 *
 * <p>Absent is not one of them. {@code null} means nobody has decided, which is a different thing
 * from {@link #TAL_CUAL} — the question not applying to olive oil is an answer, and not having been
 * asked about chicken is not.
 */
public enum Preparation {
  /** Weighed before cooking, and cooking is expected: dry rice, raw chicken. */
  CRUDO,
  /** Weighed after cooking: boiled rice, a roasted breast. */
  COCINADO,
  /** Never cooked, so the question does not apply: oil, milk, a banana. */
  TAL_CUAL;

  /**
   * Whether comparing a portion of this food with one of {@code other} compares like with like.
   *
   * <p>False only when both are known and they disagree about the kitchen. Dry rice against boiled
   * pasta is two different questions wearing the same units, and an equivalence between them is
   * arithmetically fine and nutritionally meaningless.
   *
   * <p>{@link #TAL_CUAL} agrees with everything: oil is oil whether the thing beside it was cooked
   * or not, so a swap involving it is never comparing states.
   *
   * <p>An unknown state agrees with everything too, and deliberately: refusing to compare until
   * somebody has classified both foods would make an unfilled column block work rather than inform
   * it. Silence is not disagreement.
   */
  public static boolean comparable(Preparation one, Preparation other) {
    if (one == null || other == null || one == TAL_CUAL || other == TAL_CUAL) {
      return true;
    }
    return one == other;
  }
}
