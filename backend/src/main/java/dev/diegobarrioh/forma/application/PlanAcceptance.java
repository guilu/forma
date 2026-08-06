package dev.diegobarrioh.forma.application;

/**
 * What the first screen after login needs to know: whether there is a plan waiting to be started,
 * and what it is called.
 *
 * @param pending whether a plan is written but not yet switched on
 * @param planName the name of that plan, or {@code null} when there is nothing to offer
 */
public record PlanAcceptance(boolean pending, String planName) {

  private static final PlanAcceptance NOTHING = new PlanAcceptance(false, null);

  /** Nothing to offer: no plan, or one the user already accepted. */
  public static PlanAcceptance nothing() {
    return NOTHING;
  }

  /** A plan is waiting for an answer. */
  public static PlanAcceptance of(String planName) {
    return new PlanAcceptance(true, planName);
  }
}
