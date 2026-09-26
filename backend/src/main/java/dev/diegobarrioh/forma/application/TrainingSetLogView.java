package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.LoggedSet;
import java.util.List;

/**
 * Read model for {@code GET /training/sessions/{id}/sets} (training-progression-and-logging slice
 * B, design D6): the session's full set grid, template-driven with the log left-joined — every
 * {@code (exerciseId, setNumber)} the current template prescribes, with a stored {@link LoggedSet}
 * where one exists and an empty/undone placeholder otherwise. A set logged against an exerciseId
 * the template no longer has is not included here (but is not deleted either — see {@link
 * TrainingSetLogService#getSets(String)}).
 */
public record TrainingSetLogView(String sessionId, List<LoggedSet> sets) {}
