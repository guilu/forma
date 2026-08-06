package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.diegobarrioh.forma.domain.PlanStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The first-login gate: an account arrives with a plan already written for it but not yet switched
 * on, and nothing is shown until somebody says yes.
 */
@ExtendWith(MockitoExtension.class)
class PlanActivationServiceTest {

  private static final UUID USER = UUID.randomUUID();

  @Mock private NutritionPlanService plans;
  @Mock private PlanAcceptanceRepository acceptances;
  @Mock private CurrentUserProvider currentUser;

  private PlanActivationService service;

  @BeforeEach
  void setUp() {
    service = new PlanActivationService(plans, acceptances, currentUser);
  }

  private NutritionPlan plan(UUID id, String name, PlanStatus status) {
    return new NutritionPlan(id, USER, name, null, null, status, null, null, null, null, List.of());
  }

  @Test
  void offersTheDraftPlanWhenNothingHasBeenAcceptedYet() {
    when(currentUser.currentUserId()).thenReturn(USER);
    when(acceptances.accepted(USER)).thenReturn(false);
    when(plans.findAll(USER))
        .thenReturn(List.of(plan(UUID.randomUUID(), "Dieta semanal", PlanStatus.DRAFT)));

    PlanAcceptance status = service.pending();

    assertThat(status.pending()).isTrue();
    assertThat(status.planName()).isEqualTo("Dieta semanal");
  }

  /**
   * Nothing to offer is not an error: it is an account whose plan generation never produced one.
   */
  @Test
  void offersNothingWhenTheAccountHasNoPlanAtAll() {
    when(currentUser.currentUserId()).thenReturn(USER);
    when(acceptances.accepted(USER)).thenReturn(false);
    when(plans.findAll(USER)).thenReturn(List.of());

    assertThat(service.pending().pending()).isFalse();
    assertThat(service.pending().planName()).isNull();
  }

  /** A finished plan is history, not an offer. Only a DRAFT is waiting to be started. */
  @Test
  void offersNothingWhenTheOnlyPlanIsAlreadyFinished() {
    when(currentUser.currentUserId()).thenReturn(USER);
    when(acceptances.accepted(USER)).thenReturn(false);
    when(plans.findAll(USER))
        .thenReturn(List.of(plan(UUID.randomUUID(), "Plan base", PlanStatus.COMPLETED)));

    assertThat(service.pending().pending()).isFalse();
  }

  @Test
  void offersNothingOnceTheUserHasAccepted() {
    when(currentUser.currentUserId()).thenReturn(USER);
    when(acceptances.accepted(USER)).thenReturn(true);

    assertThat(service.pending().pending()).isFalse();
  }

  @Test
  void acceptingActivatesTheDraftAndRecordsTheAcceptance() {
    UUID planId = UUID.randomUUID();
    when(currentUser.currentUserId()).thenReturn(USER);
    when(plans.findAll(USER)).thenReturn(List.of(plan(planId, "Dieta semanal", PlanStatus.DRAFT)));

    service.accept();

    verify(plans).activate(USER, planId);
    verify(acceptances).markAccepted(eq(USER), any(Instant.class));
  }

  /**
   * Accepting with nothing to accept is the client asking for something that is not there, not a
   * server fault — and it must not leave the account marked as having accepted a plan it never got.
   */
  @Test
  void acceptingWithNoDraftFailsAndRecordsNothing() {
    when(currentUser.currentUserId()).thenReturn(USER);
    when(plans.findAll(USER)).thenReturn(List.of());

    assertThatThrownBy(() -> service.accept()).isInstanceOf(NotFoundException.class);

    verify(acceptances, never()).markAccepted(any(), any());
  }

  /** What the training gate asks: the plan in code is withheld until the account has said yes. */
  @Test
  void reportsAcceptanceForTheGates() {
    when(currentUser.currentUserId()).thenReturn(USER);
    when(acceptances.accepted(USER)).thenReturn(true);

    assertThat(service.accepted()).isTrue();
  }
}
