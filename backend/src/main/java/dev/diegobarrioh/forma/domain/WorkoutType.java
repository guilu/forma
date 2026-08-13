package dev.diegobarrioh.forma.domain;

/**
 * Type of a strength workout (FOR-25), per docs/domain-model.md.
 *
 * <p>Closed classification. The Jira "Legs and core" template maps to {@link #LEGS}. {@link
 * #FULL_BODY} is part of the model's vocabulary but is not seeded in this story.
 */
public enum WorkoutType {
  PUSH(BodyView.FRONT),
  PULL(BodyView.BACK),
  LEGS(BodyView.FRONT),
  FULL_BODY(BodyView.FRONT);

  private final BodyView bodyView;

  WorkoutType(BodyView bodyView) {
    this.bodyView = bodyView;
  }

  public BodyView bodyView() {
    return bodyView;
  }
}
