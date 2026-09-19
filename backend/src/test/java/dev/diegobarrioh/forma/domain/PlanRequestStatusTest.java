package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * The {@code open_marker} rule migration V63 relies on: {@code '1'} while a request is open ({@link
 * PlanRequestStatus#PENDING}/{@link PlanRequestStatus#GENERATING}), null once it is terminal.
 * Mirrors {@link PlanStatus#marker()}'s own {@code active_marker} rule for {@code nutrition_plan}.
 */
class PlanRequestStatusTest {

  @Test
  void pendingAndGeneratingCarryTheOpenMarker() {
    assertThat(PlanRequestStatus.PENDING.openMarker()).isEqualTo("1");
    assertThat(PlanRequestStatus.GENERATING.openMarker()).isEqualTo("1");
  }

  @Test
  void readyAndFailedCarryNoMarker() {
    assertThat(PlanRequestStatus.READY.openMarker()).isNull();
    assertThat(PlanRequestStatus.FAILED.openMarker()).isNull();
  }

  /** DELETED (migration V65) is terminal exactly like READY and FAILED: no open_marker. */
  @Test
  void deletedCarriesNoMarker() {
    assertThat(PlanRequestStatus.DELETED.openMarker()).isNull();
  }
}
