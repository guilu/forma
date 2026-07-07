package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.domain.RunningPlanGenerator;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link RunningPlanService} (FOR-23). Verifies the service exposes the deterministic
 * generated plan (no Spring context needed — ADR-007).
 */
class RunningPlanServiceTest {

  private final RunningPlanService service = new RunningPlanService();

  @Test
  void exposesTheGeneratedSixteenWeekPlan() {
    assertThat(service.currentPlan()).hasSize(48).isEqualTo(RunningPlanGenerator.sixteenWeekPlan());
  }
}
