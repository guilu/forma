package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.Goal;
import dev.diegobarrioh.forma.domain.GoalProgress;

/**
 * A goal paired with its freshly-derived progress (FOR-125): the read model {@link GoalService}
 * returns to the delivery layer. Progress is never stored (see {@link GoalProgress}), so this
 * pairing only ever exists transiently, computed at request time.
 *
 * @param id the goal's persisted id
 * @param goal the goal data
 * @param progress the goal's currently derived progress
 */
public record GoalView(String id, Goal goal, GoalProgress progress) {}
