package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.RunningPlanGenerator;
import dev.diegobarrioh.forma.domain.RunningPlanSession;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Application use case exposing the initial running plan (FOR-23).
 *
 * <p>Returns the deterministic 16-week progression from {@link RunningPlanGenerator} so later
 * stories (FOR-26 calendar, and a future read endpoint) can retrieve it. The plan is generated on
 * demand — no persisted entity (spec FOR-23). Mirrors the FOR-21 pattern of a thin application
 * service over a pure domain calculation.
 */
@Service
public class RunningPlanService {

  /** The current initial running plan (16 weeks, 3 sessions per week). */
  public List<RunningPlanSession> currentPlan() {
    return RunningPlanGenerator.sixteenWeekPlan();
  }
}
