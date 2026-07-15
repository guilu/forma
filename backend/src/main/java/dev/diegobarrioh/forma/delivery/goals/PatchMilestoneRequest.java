package dev.diegobarrioh.forma.delivery.goals;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * A milestone completion-state change as supplied on {@code PATCH /api/v1/goals/{id}} (FOR-125
 * api.md). Only {@code completed} can change this way — a milestone is never renamed, retargeted,
 * added or removed via this slice's PATCH (spec FOR-125 Open Questions: milestone completion is
 * always user-set; no other milestone mutation is in scope).
 *
 * @param id the id of an existing milestone on the goal being patched
 * @param completed the new completion state
 */
public record PatchMilestoneRequest(@NotBlank String id, @NotNull Boolean completed) {}
