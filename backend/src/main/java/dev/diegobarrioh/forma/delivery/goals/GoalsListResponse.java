package dev.diegobarrioh.forma.delivery.goals;

import java.util.List;

/** Response body for {@code GET /api/v1/goals} (FOR-125 api.md): {@code {"goals": [...]}}. */
public record GoalsListResponse(List<GoalResponse> goals) {}
