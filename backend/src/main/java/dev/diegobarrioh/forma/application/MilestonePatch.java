package dev.diegobarrioh.forma.application;

/**
 * A single milestone completion-state change from a {@code PATCH /api/v1/goals/{id}} request
 * (FOR-125). Milestone completion is always user-set (spec FOR-125 Open Questions); this slice's
 * PATCH can only toggle an existing milestone's {@code completed} flag by id, never add, remove or
 * rename one.
 *
 * @param milestoneId the id of the milestone to update
 * @param completed the new completion state
 */
public record MilestonePatch(String milestoneId, boolean completed) {}
